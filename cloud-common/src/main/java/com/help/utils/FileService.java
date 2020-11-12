package com.help.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.util.concurrent.FutureListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private FileRegion fileRegion;
    private ByteBuf buffer;
    private byte[] filenameBytes;

    public void uploadFile(Channel channel, Path path, FutureListener listener) throws IOException {
        fileRegion = new DefaultFileRegion(new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path.normalize()));
        filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length + 8);
        buffer.writeByte(SignalByte.FILE_SIGNAL_BYTE);
        buffer.writeInt(path.getFileName().toString().length());
        buffer.writeBytes(filenameBytes);
        buffer.writeLong(Files.size(path));
        channel.writeAndFlush(buffer);
        ChannelFuture future = channel.writeAndFlush(fileRegion);
        if (listener != null) {
            future.addListener(listener);
        }
    }

    public void sendCommand(Channel channel, String command) {
        buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + command.length());
        buffer.writeByte(SignalByte.CMD_SIGNAL_BYTE);
        buffer.writeInt(command.length());
        buffer.writeBytes(command.getBytes());
        channel.writeAndFlush(buffer);
    }

    public void delete(Path path) throws IOException {
        Files.delete(path);
    }

    public void createDirectory(Path path, String directory) throws Exception {
        File dir = new File(path + File.separator + directory);
        if (dir.exists()) {
            throw new Exception("The directory is already exists");
        } else {
            dir.mkdir();
        }
    }

    public List<FileInfo> createFileList(String s) {
        List<FileInfo> temp = new ArrayList<>();
        String[] files = s.split("\n");
        for (String file : files) {
            String[] tmp = file.split(",");
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(tmp[0]);
            fileInfo.setSize(Long.parseLong(tmp[1]));
            if (tmp[2].equals("FILE")) {
                fileInfo.setType(FileType.FILE);
            } else {
                fileInfo.setType(FileType.DIRECTORY);
            }
            fileInfo.setLastModified(LocalDateTime.parse(tmp[3]));
            temp.add(fileInfo);
        }
        return temp;
    }
}
