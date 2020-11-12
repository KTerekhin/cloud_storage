package com.server;

import com.help.utils.FileInfo;
import com.help.utils.FileService;
import com.help.utils.SignalByte;
import com.help.utils.State;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class CloudServerHandler extends ChannelInboundHandlerAdapter {
    private State curState = State.IDLE;
    private int commandLength = 0;
    private int filenameLength = 0;
    private long fileSize = 0L;
    private long receivedFileSize = 0L;
    private BufferedOutputStream out;
    private StringBuilder sb;
    private Path currentPath = Paths.get("testServer");
    FileService fileService = new FileService();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client accepted");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client de-accepted");
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
                    System.out.println("Start downloading...");
                } else {
                    curState = State.IDLE;
                    throw new RuntimeException("Unknown byte command: " + read);
                }
            }

            if (curState == State.COMMAND) {
                if (buf.readableBytes() >= 4) {
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
                    case "/authorization":
                        if (cmd[1].equals("t") && cmd[2].equals("t")) {
                            fileService.sendCommand(ctx.channel(), "auth_OK\nOK");
                        } else {
                            fileService.sendCommand(ctx.channel(), "auth_OK\nNOT");
                        }
                        curState = State.IDLE;
                        break;
                    case "/enterToDirectory":
                        currentPath = currentPath.resolve(cmd[1]);
                        curState = State.FILE_LIST;
                        break;
                    case "/updateFileList":
                        curState = State.FILE_LIST;
                        break;
                    case "/upDirectory":
                        if (currentPath.getParent().toString().equals("testClient")) {
                            curState = State.IDLE;
                        } else {
                            currentPath = currentPath.getParent();
                            curState = State.FILE_LIST;
                        }
                        break;
                    case "/download":
                        try {
                            fileService.uploadFile(ctx.channel(), currentPath.resolve(cmd[1]), null);
                            curState = State.IDLE;
                            break;
                        } catch (IOException e) {
                            fileService.sendCommand(ctx.channel(), String.format("/Error\n%s\n%s", e.getClass().getSimpleName(), e.getCause().getMessage()));
                        }
                    case "/mkdir":
                        try {
                            fileService.createDirectory(currentPath, cmd[1]);
                            curState = State.FILE_LIST;
                            break;
                        } catch (Exception e) {
                            fileService.sendCommand(ctx.channel(), String.format("/Error\n%s\n%s", e.getClass().getSimpleName(), e.getCause().getMessage()));
                            curState = State.IDLE;
                            break;
                        }
                    case "/delete":
                        try {
                            fileService.delete(currentPath.resolve(cmd[1]));
                            curState = State.FILE_LIST;
                            break;
                        } catch (IOException e) {
                            fileService.sendCommand(ctx.channel(), String.format("/Error\n%s\n%s", e.getClass().getSimpleName(), e.getCause().getMessage()));
                            curState = State.IDLE;
                            break;
                        }
                    default:
                        curState = State.IDLE;
                        throw new IllegalArgumentException("Unknown command: " + sb.toString());
                }
            }

            if (curState == State.NAME_LENGTH) {
                receivedFileSize = 0L;
                fileSize = 0L;
                if (buf.readableBytes() >= 4) {
                    System.out.println("Get filename length.");
                    filenameLength = buf.readInt();
                    curState = State.NAME;
                }
            }

            if (curState == State.NAME) {
                if (buf.readableBytes() >= filenameLength) {
                    byte[] filenameBytes = new byte[filenameLength];
                    buf.readBytes(filenameBytes);
                    String filename = new String(filenameBytes, StandardCharsets.UTF_8);
                    File file = new File(currentPath.toString() + File.separator + filename);
                    System.out.println("Filename received: " + filename);
                    try {
                        out = new BufferedOutputStream(new FileOutputStream((file)));
                    } catch (FileNotFoundException e) {
                        fileService.sendCommand(ctx.channel(), String.format("/Error\n%s\n%s", e.getClass().getSimpleName(), e.getCause().getMessage()));
                        curState = State.IDLE;
                        break;
                    }
                    curState = State.FILE_LENGTH;
                }
            }

            if (curState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileSize = buf.readLong();
                    System.out.println("File size received");
                    curState = State.FILE;
                }
            }

            if (curState == State.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileSize++;
                    if (fileSize == receivedFileSize) {
                        curState = State.FILE_LIST;
                        System.out.println("File received");
                        out.close();
                        break;
                    }
                }
            }

            if (curState == State.FILE_LIST) {
                try {
                    List<FileInfo> serverList = Files.list(currentPath)
                            .map(FileInfo::new)
                            .collect(Collectors.toList());
                    sb = new StringBuilder();
                    for (FileInfo fileInfo : serverList) {
                        sb.append(String.format("%s,%d,%s,%s\n", fileInfo.getFileName(),
                                fileInfo.getSize(),
                                fileInfo.getType(),
                                fileInfo.getLastModified()));
                    }
                    fileService.sendCommand(ctx.channel(), "/FileList\n" + sb.toString());
                    curState = State.IDLE;
                } catch (IOException e) {
                    fileService.sendCommand(ctx.channel(), String.format("/Error\n%s\n%s", e.getClass().getSimpleName(), e.getCause().getMessage()));
                    curState = State.IDLE;
                    break;
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
