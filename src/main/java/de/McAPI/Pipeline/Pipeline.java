package de.McAPI.Pipeline;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import de.McAPI.Pipeline.protocol.PipelineHandshakeHandler;
import de.McAPI.Pipeline.protocol.PipelineRequestDecoder;
import de.McAPI.Pipeline.protocol.PipelineResponseHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.AttributeKey;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.Optional;

@Plugin(id = "pipeline", name = "Pipeline", version = "0.3-alpha", url = "http://mcapi.de", authors = "Yonas")
public class Pipeline {

    public final static AttributeKey<Pipeline> PLUGIN_ATTRIBUTE_KEY = AttributeKey.valueOf("key_pipeline_plugin");

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path directory;

    private boolean debug = true;

    private NioEventLoopGroup group = new NioEventLoopGroup(1);
    private io.netty.channel.Channel channel;

    private Key signature;
    private String version;

    public Pipeline() {}

    @Listener
    public void onStart(GameStartedServerEvent event) {

        // get the host
        String host = null;
        Optional<InetSocketAddress> address = Sponge.getServer().getBoundAddress();
        host = (address.isPresent() ? address.get().getAddress().getHostAddress() : "0.0.0.0");

        // parse version
        this.version = Sponge.getGame().getPluginManager().getPlugin("pipeline").get().getVersion().get();

        // Config
        Path config = this.directory.resolve("config.yml");
        try {
            Files.createDirectories(this.directory);
        } catch (IOException e) {
            e.printStackTrace();
        }


        if(!(Files.exists(config))) {

            try {

                String defaultConfig = new String(
                        ByteStreams.toByteArray(Pipeline.class.getClassLoader().getResourceAsStream("config.yml")),
                        StandardCharsets.UTF_8
                );

                defaultConfig = defaultConfig
                        .replace("%default_signature%", Token.generateSignature())
                        .replace("%ip%", host);


                Files.write(config, ImmutableList.<CharSequence>of(defaultConfig), StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        ConfigurationLoader loader = YAMLConfigurationLoader.builder().setPath(config).build();

        ConfigurationNode node;

        try {
            node = loader.load();
        } catch (IOException e) {
            this.logger.info("Failed to load config.");
            return;
        }

        // load all values
        this.signature = new SecretKeySpec(node.getNode("signature").getString().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.debug = node.getNode("debug").getBoolean(true);
        int port  = node.getNode("port").getInt(20000);

        new ServerBootstrap()
            .channel(NioServerSocketChannel.class)
            .group(this.group)
            .childHandler(new ChannelInitializer<NioSocketChannel>() {

                @Override
                protected void initChannel(NioSocketChannel channel) throws Exception {

                    Session session = new Session();

                    if (isDebug()) {
                        logger.info(String.format(
                                "[%s] Creating a new request.",
                                session.getDebugKey()
                        ));
                    }


                    channel.attr(Session.SESSION_ATTRIBUTE_KEY).set(session);
                    channel.attr(Pipeline.PLUGIN_ATTRIBUTE_KEY).set(Pipeline.this);

                    if (isDebug()) {
                        logger.info(String.format(
                                "[%s] Establishing connection with %s.",
                                session.getDebugKey(),
                                channel.remoteAddress().toString()
                        ));
                    }

                    channel.pipeline().addLast("handshakeHandler", new PipelineHandshakeHandler());
                    channel.pipeline().addAfter("handshakeHandler", "stringDecoder", new StringDecoder(StandardCharsets.UTF_8));
                    channel.pipeline().addAfter("stringDecoder", "requestDecoder", new PipelineRequestDecoder());
                    channel.pipeline().addAfter("requestDecoder", "response", new PipelineResponseHandler());
                }

            })
            .bind(host, port)
            .addListener((ChannelFutureListener) channelFuture -> {

                if (channelFuture.isSuccess()) {
                    channel = channelFuture.channel();
                    logger.info("Pipeline is now open.");
                } else {
                    logger.info("Pipeline wasn't able to let the oil threw...");

                    if (isDebug() && !(channelFuture.cause() == null)) {
                        logger.info(channelFuture.cause().getMessage());
                    }
                }

            });

    }

    @Listener
    public void onStop(GameStoppingServerEvent event) {

        if(!(this.channel == null)) {
            this.channel.close();
        }

        this.group.shutdownGracefully();
        this.logger.info("Closed the Pipeline.");

    }

    /**
     * This method returns the related signature.
     * @return
     */
    public Key getSignature() {
        return this.signature;
    }

    /**
     * Returns the logger.
     * @return
     */
    public Logger logger() {
        return this.logger;
    }

    /**
     * Returns the plugin version in a human-readable format.
     * @return
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Returns true if the debug modus is active.
     * @return
     */
    public boolean isDebug() {
        return this.debug;
    }

}
