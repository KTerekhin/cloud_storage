package com.client;

import com.help.utils.ServiceMessage;
import com.help.utils.SignalByte;
import com.help.utils.State;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Handler extends ChannelInboundHandlerAdapter {
    private State curState = State.IDLE;
    private int nextLength;
    private int commandLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private StringBuilder sb;
    private ServiceMessage callback;
    private Path currentPath = Paths.get("testClient");

    public void setCallback(ServiceMessage callback) {
        this.callback = callback;
    }

    public void setCurrentPath(Path currentPath) {
        this.currentPath = currentPath;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (curState == State.IDLE) {
                byte read = buf.readByte();
                if (read == SignalByte.CMD_SIGNAL_BYTE) {
                    curState = State.COMMAND;
                } else if (read == SignalByte.FILE_SIGNAL_BYTE) {
                    curState = State.NAME_LENGTH;
                } else {
                    curState = State.IDLE;
                    throw new IllegalArgumentException("Unknown byte command: " + read);
                }
            }

            if (curState == State.COMMAND) {
                if (buf.readableBytes() >=4) {
                    commandLength = buf.readInt();
                    curState = State.COMMAND_READ;
                }
            }

            if (curState == State.COMMAND_READ) {
                sb = new StringBuilder();
                while (buf.readableBytes() > 0 && commandLength != 0) {
                    commandLength--;
                    sb.append((char) buf.readByte());
                }
                curState = State.COMMAND_DO;
            }

            if (curState == State.COMMAND_DO) {
                String[] cmd = sb.toString().split("\n");
                switch (cmd[0]) {
                    case "auth_OK":
                        callback.callback(cmd[1]);
                        curState = State.IDLE;
                        break;
                    case "/FileList":
                        callback.callback(sb.toString());
                        curState = State.IDLE;
                        break;
                    default:
                        curState = State.IDLE;
                        throw new IllegalArgumentException("Unknown command: " + sb.toString());
                }
            }

            if (curState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("Get filename length.");
                    nextLength = buf.readInt();
                    curState = State.NAME;
                }
            }

            if (curState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] filename = new byte[nextLength];
                    buf.readBytes(filename);
                    System.out.println("Filename received" + new String(filename, StandardCharsets.UTF_8));
                    out = new BufferedOutputStream(new FileOutputStream(new String(filename)));
                    curState = State.FILE_LENGTH;
                }
            }

            if (curState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("File length received");
                    curState = State.FILE;
                }
            }

            if (curState == State.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        curState = State.IDLE;
                        System.out.println("File received");
                        out.close();
                        break;
                    }
                }
            }
        }

        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
