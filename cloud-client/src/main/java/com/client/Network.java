package com.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class Network {
    private String HOST;
    private int PORT;
    private static Network instance;
    private Handler handler = new Handler();
    private SocketChannel curChannel;

    public Network(String host, int port) {
        instance = this;
        this.HOST = host;
        this.PORT = port;
        Thread t = new Thread(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap clientBootstrap = new Bootstrap();
                clientBootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .remoteAddress(new InetSocketAddress(HOST, PORT))
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel socketChannel) {
                                curChannel = socketChannel;
                                socketChannel.pipeline().addLast(handler);
                            }
                        });
                ChannelFuture channelFuture = clientBootstrap.connect().sync();
                channelFuture.channel().closeFuture().sync();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    group.shutdownGracefully().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public static Network getInstance() {
        return instance;
    }

    public Handler getHandler() {
        return handler;
    }

    public void close() {
        curChannel.close();
    }

    public Channel getCurChannel() {
        return curChannel;
    }
}
