package de.McAPI.Pipeline.protocol;

import com.google.gson.JsonObject;
import de.McAPI.Pipeline.Pipeline;
import de.McAPI.Pipeline.Session;
import de.McAPI.Pipeline.exception.PipelineException;
import de.McAPI.Pipeline.protocol.response.PipelineResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;

/**
 * Created by Yonas on 09.04.2016.
 */
public class PipelineResponseHandler extends SimpleChannelInboundHandler<PipelineResponse> {

    public PipelineResponseHandler() {}

    @Override
    protected void channelRead0(ChannelHandlerContext context, PipelineResponse pipelineResponse) throws Exception {

        Pipeline pipeline = context.channel().attr(Pipeline.PLUGIN_ATTRIBUTE_KEY).get();
        Session session = context.channel().attr(Session.SESSION_ATTRIBUTE_KEY).get();

        // The first packet is the length of the next package which contains all information.
        // I do this because the length of the first package is kind of predictable, but the
        // second package length depends on too many factors.

        if(pipeline.isDebug()) {
            pipeline.logger().info(String.format(
                    "[%s] Starting to respond...",
                    session.getDebugKey()
            ));
        }


        JsonObject jsonObject = new JsonObject();
        int responseLength = pipelineResponse.getResponse().getBytes().length;
        jsonObject.addProperty("length", responseLength);

        context.pipeline().writeAndFlush(
                Unpooled.copiedBuffer(
                        jsonObject.toString() + "\0",
                        StandardCharsets.UTF_8
                )
        );
        if(pipeline.isDebug()) {
            pipeline.logger().info(String.format(
                    "[%s] Sent \"length\"-package (%s).",
                    session.getDebugKey(),
                    jsonObject.toString()
            ));
        }

        context.pipeline().writeAndFlush(
                Unpooled.copiedBuffer(
                        pipelineResponse.getResponse() + "\0",
                        StandardCharsets.UTF_8
                )
        ).addListener(ChannelFutureListener.CLOSE);

        if(pipeline.isDebug()) {
            pipeline.logger().info(String.format(
                    "[%s] Sent \"data\"-package (%d bytes).",
                    session.getDebugKey(),
                    responseLength
            ));
        }

        if(pipeline.isDebug()) {
            pipeline.logger().info(String.format(
                    "[%s] Finished request.",
                    session.getDebugKey(),
                    responseLength
            ));
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {

        if(cause instanceof PipelineException) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("status", "Error");
            jsonObject.addProperty("message", cause.getMessage().substring(cause.getMessage().indexOf(": ") + 2));

            context.pipeline().writeAndFlush(
                    Unpooled.copiedBuffer(
                            jsonObject.toString() + "\0",
                            StandardCharsets.UTF_8
                    )
            );
        }

        if(context.channel().attr(Pipeline.PLUGIN_ATTRIBUTE_KEY).get().isDebug()) {
            cause.printStackTrace();
        }
    }

}
