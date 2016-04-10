package de.McAPI.Pipeline.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.McAPI.Pipeline.Pipeline;
import de.McAPI.Pipeline.Session;
import de.McAPI.Pipeline.exception.PipelineException;
import de.McAPI.Pipeline.protocol.response.PipelineResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Yonas on 09.04.2016.
 */
public class PipelineRequestDecoder extends MessageToMessageDecoder<String> {

    private final static Gson gson = new Gson();
    private static final SecureRandom random = new SecureRandom();

    public PipelineRequestDecoder() {}

    @Override
    protected void decode(ChannelHandlerContext context, String input, List<Object> list) throws Exception {

        Pipeline pipeline = context.channel().attr(Pipeline.PLUGIN_ATTRIBUTE_KEY).get();
        Session session = context.channel().attr(Session.SESSION_ATTRIBUTE_KEY).get();

        if(pipeline.isDebug()) {
            pipeline.logger().info(String.format(
                    "[%s] Starting to decode the response.",
                    session.getDebugKey()
            ));
        }

        JsonObject request = this.gson.fromJson(input, JsonObject.class);
        JsonObject payload = this.gson.fromJson(request.get("payload").getAsString(), JsonObject.class);

        // Validate the provided key
        String providedKey = null;

        if(payload.has("key")) {
            providedKey = payload.get("key").getAsString();
        } else {
            if(pipeline.isDebug()) {
                pipeline.logger().info(String.format(
                        "[%s] The payload has no \"key\" value.",
                        session.getDebugKey()
                ));
            }
        }

        // If the key is null or it doesnt match the session key, then we will drop the connection.
        if(providedKey == null || !(providedKey.toString().equals(session.getKey()))) {
            if(pipeline.isDebug()) {
                pipeline.logger().info(String.format(
                        "[%s] The provided key (%s) is not valid or not equals to the session key.",
                        session.getDebugKey(),
                        providedKey
                ));
            }
            throw new PipelineException("The provided key is not valid.");
        }

        Key signature = pipeline.getSignature();

        byte[] sigBytes = DatatypeConverter.parseBase64Binary(
                request.get("signature").getAsString()
        );

        if(!(hmacEqual(sigBytes, request.get("payload").getAsString().getBytes(StandardCharsets.UTF_8), signature))) {
            if(pipeline.isDebug()) {
                pipeline.logger().info(String.format(
                        "[%s] The provided signature is not valid.",
                        session.getDebugKey()
                ));
            }
            throw new PipelineException("The provided signature is not valid.");
        }

        if(pipeline.isDebug()) {
            pipeline.logger().info(String.format(
                    "[%s] Creating a new PipelineResponse.",
                    session.getDebugKey()
            ));
        }

        PipelineResponse pipelineResponse = new PipelineResponse(pipeline, session);
        pipelineResponse.build();
        list.add(pipelineResponse);
        context.pipeline().remove(this);


    }

    /**
     * @author NuVotifier-Author
     * @param sig
     * @param message
     * @param key
     * @return
     * @throws PipelineException
     */
    private boolean hmacEqual(byte[] sig, byte[] message, Key key) throws PipelineException {
        // See https://www.nccgroup.trust/us/about-us/newsroom-and-events/blog/2011/february/double-hmac-verification/
        // This randomizes the byte order to make timing attacks more difficult.
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
        } catch (NoSuchAlgorithmException e) {
            throw new PipelineException(e.getMessage());
        } catch (InvalidKeyException e) {
            throw new PipelineException(e.getMessage());
        }

        byte[] calculatedSig = mac.doFinal(message);

        // Generate a random key for use in comparison
        byte[] randomKey = new byte[32];
        this.random.nextBytes(randomKey);

        // Then generateToken two HMACs for the different signatures found
        Mac mac2;
        try {
            mac2 = Mac.getInstance("HmacSHA256");
            mac2.init(new SecretKeySpec(randomKey, "HmacSHA256"));
        } catch (NoSuchAlgorithmException e) {
            throw new PipelineException(e.getMessage());
        } catch (InvalidKeyException e) {
            throw new PipelineException(e.getMessage());
        }

        byte[] clientSig = mac2.doFinal(sig);
        mac2.reset();
        byte[] realSig = mac2.doFinal(calculatedSig);

        return Arrays.equals(clientSig, realSig);
    }

}
