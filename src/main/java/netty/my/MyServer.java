package netty.my;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

public class MyServer {
    private final int port;

    public MyServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {

        int port = 9999;
        new MyServer(port).start();
    }

    public void start() throws Exception {
        final MyServerHandler serverHandler = new MyServerHandler();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            System.out.println("channelInitializer!");
                            ch.pipeline().addLast(serverHandler);
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                                private ChannelHandlerContext innerCtx;
                                ChannelFuture connectFuture;
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    final String encodedMsg = "xxx";
                                    Bootstrap bootstrap = new Bootstrap();
                                    bootstrap.channel(NioSocketChannel.class).handler(
                                            new SimpleChannelInboundHandler<ByteBuf>() {
                                                //内层建立的连接，从这里接收内层的应答，在这里是服务端的应答
                                                @Override
                                                protected void channelRead0(
                                                        ChannelHandlerContext ctx, ByteBuf in)
                                                        throws Exception {
                                                    innerCtx = ctx;

                                                    byte[] dst = new byte[in.readableBytes()];
                                                    in.readBytes(dst);

                                                    System.out.println("client 23456 : Received data" + new String(dst));
//                                                    ctx.writeAndFlush(Unpooled.copiedBuffer(new String(dst), CharsetUtil.UTF_8));
                                                }
                                                @Override
                                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                                    System.out.println("链接服务" + ctx.channel().toString());
                                                    ctx.writeAndFlush(Unpooled.copiedBuffer(encodedMsg, CharsetUtil.UTF_8));
                                                }
                                            });
                                    bootstrap.group(ctx.channel().eventLoop());//关键在这里。把外层channel的eventLoop挂接在内层上
                                    connectFuture = bootstrap.connect(
                                            new InetSocketAddress("localhost", 23456));
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                    if (connectFuture.isDone()) {

                                    }
                                    // do something with the data
                                    //channel并不共享，共享的是线程EventLoop，所以如果想向内层转发的话
                                    //需要持有内层的channel
                                    if (innerCtx != null && innerCtx.channel().isActive()) {
                                        innerCtx.writeAndFlush(msg);
                                    }

                                }
                            });
                        }
                    });
            ChannelFuture f = serverBootstrap.bind().sync();
            f.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        System.out.println("Server bound " + port);
                    } else {
                        System.out.println("bind attempt failed");
                    }
                }
            });
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
