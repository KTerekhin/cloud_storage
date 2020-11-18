package com.handler;

import com.database.service.DataBaseAuthService;
import com.help.utils.FileInfo;
import com.help.utils.FileService;
import com.help.utils.SignalByte;
import com.help.utils.State;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
//import org.apache.logging.log4j.core.Logger;

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
    private final String ROOT_PATH = "testServer";
    private Path userPath;
    FileService fileService = new FileService();
//    Logger log = null;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client accepted");
//        log.info(String.format("[ip: %s]: Channel is connected", ctx.channel().remoteAddress()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client de-accepted");
//        log.info(String.format("[ip: %s]: Channel is disconnected", ctx.channel().remoteAddress()));
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
//                    log.error(String.format("[ip: %s]: Unknown byte command arrived.", ctx.channel().remoteAddress()));
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
//                        log.info(String.format("[ip: %s]: Command from client: Authorization", ctx.channel().remoteAddress()));
                        String path = DataBaseAuthService.getAuthByLoginAndPass(cmd[1], cmd[2]);
                        System.out.println(path);
                        if (path != null) {
                            userPath = Paths.get(ROOT_PATH, path);
                            fileService.sendCommand(ctx.channel(), "auth_OK\nOK");
                        } else {
                            fileService.sendCommand(ctx.channel(), "auth_OK\nNOT");
                        }
                        curState = State.IDLE;
                        break;
                    case "/registration":
//                        log.info(String.format("[ip: %s]: Command from client: Registration", ctx.channel().remoteAddress()));
                        String currentPath = DataBaseAuthService.tryRegister(cmd[1], cmd[2]);
                        System.out.println(currentPath);
                        Path newPath = Paths.get(ROOT_PATH, currentPath);
                        if (!Files.exists(newPath)) {
                            try {
                                Files.createDirectory(newPath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        fileService.sendCommand(ctx.channel(), "Reg_OK\nOK");
                        curState = State.IDLE;
                        break;
                    case "/enterToDirectory":
//                        log.info(String.format("[ip: %s]: Command from client: Enter to directory %s", ctx.channel().remoteAddress(), cmd[1]));
                        userPath = userPath.resolve(cmd[1]);
                        curState = State.FILE_LIST;
                        break;
                    case "/updateFileList":
//                        log.info(String.format("[ip: %s]: Command from client: Update file list", ctx.channel().remoteAddress()));
                        curState = State.FILE_LIST;
                        break;
                    case "/upDirectory":
//                        log.info(String.format("[ip: %s]: Command from client: Up directory.", ctx.channel().remoteAddress()));
                        if (userPath.getParent().toString().equals("testServer")) {
                            curState = State.IDLE;
                        } else {
                            userPath = userPath.getParent();
                            curState = State.FILE_LIST;
                        }
                        break;
                    case "/download":
//                        log.info(String.format("[ip: %s]: Command from client: Download file", ctx.channel().remoteAddress()));
                        try {
                            fileService.uploadFile(ctx.channel(), userPath.resolve(cmd[1]), null);
                            curState = State.IDLE;
                            break;
                        } catch (IOException e) {
                            fileService.sendCommand(ctx.channel(), String.format("/Error\n%s\n%s", e.getClass().getSimpleName(), e.getCause().getMessage()));
                        }
                    case "/mkdir":
//                        log.info(String.format("[ip: %s]: Command from client: Create Directory. Directory name: (%s).", ctx.channel().remoteAddress(), cmd[1]));
                        try {
                            fileService.createDirectory(userPath, cmd[1]);
                            curState = State.FILE_LIST;
                            break;
                        } catch (Exception e) {
                            fileService.sendCommand(ctx.channel(), String.format("/Error\n%s\n%s", e.getClass().getSimpleName(), e.getCause().getMessage()));
                            curState = State.IDLE;
                            break;
                        }
                    case "/delete":
//                        log.info(String.format("[ip: %s]: Command from client: Delete file %s", ctx.channel().remoteAddress(), cmd[1]));
                        try {
                            fileService.delete(userPath.resolve(cmd[1]));
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
//                    log.info(String.format("[ip: %s]: File transaction: Get file name length (%s).", ctx.channel().remoteAddress(), filenameLength));
                }
            }

            if (curState == State.NAME) {
                if (buf.readableBytes() >= filenameLength) {
                    byte[] filenameBytes = new byte[filenameLength];
                    buf.readBytes(filenameBytes);
                    String filename = new String(filenameBytes, StandardCharsets.UTF_8);
                    File file = new File(userPath.toString() + File.separator + filename);
                    System.out.println("Filename received: " + filename);
                    try {
                        out = new BufferedOutputStream(new FileOutputStream((file)));
                    } catch (FileNotFoundException e) {
                        fileService.sendCommand(ctx.channel(), String.format("/Error\n%s\n%s", e.getClass().getSimpleName(), e.getCause().getMessage()));
                        curState = State.IDLE;
                        break;
                    }
                    curState = State.FILE_LENGTH;
//                    log.info(String.format("[ip: %s]: File transaction: Get file name (%s)", ctx.channel().remoteAddress(), filename));
                }
            }

            if (curState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileSize = buf.readLong();
                    System.out.println("File size received");
                    curState = State.FILE;
//                    log.info(String.format("[ip: %s]: File transaction: Get file size (%s).", ctx.channel().remoteAddress(), fileSize));
                }
            }

            if (curState == State.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileSize++;
                    if (fileSize == receivedFileSize) {
//                        log.info(String.format("[ip: %s]: File transaction end.", ctx.channel().remoteAddress()));
                        curState = State.FILE_LIST;
                        System.out.println("File received");
                        out.close();
                        break;
                    }
                }
            }

            if (curState == State.FILE_LIST) {
//                log.info(String.format("[ip: %s]: Build and send file list to client", ctx.channel().remoteAddress()));
                try {
                    List<FileInfo> serverList = Files.list(userPath)
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
//        log.error(String.format("[ip: %s]: Channel disconnected with error: [%s]: %s", ctx.channel().remoteAddress(), cause.getClass().getSimpleName(), cause.getMessage()));
        cause.printStackTrace();
//        ctx.close();
    }
}
