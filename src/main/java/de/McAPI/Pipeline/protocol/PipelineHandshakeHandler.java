package de.McAPI.Pipeline.protocol;

import de.McAPI.Pipeline.Pipeline;
import de.McAPI.Pipeline.Session;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

/**
 * Created by Yonas on 09.04.2016.
 */
public class PipelineHandshakeHandler extends ChannelInboundHandlerAdapter {

    public PipelineHandshakeHandler() {}

    @Override
    public void channelActive(ChannelHandlerContext context) {

        Pipeline pipeline = context.channel().attr(Pipeline.PLUGIN_ATTRIBUTE_KEY).get();
        Session session = context.channel().attr(Session.SESSION_ATTRIBUTE_KEY).get();

        if(pipeline.isDebug()) {
            pipeline.logger().info(
                    String.format("[%s] Shaking hands.", session.getDebugKey())
            );
        }

        // If the plugin receives a request, then we will say 'hello' and respond with a header containing some
        // data to go on.
        context.pipeline().writeAndFlush(
                Unpooled.copiedBuffer(
                        String.format(
                                "PIPELINE %s %s\0",
                                pipeline.getVersion(),
                                session.getKey()
                        ),
                        StandardCharsets.UTF_8
                )
        );

    }

}
