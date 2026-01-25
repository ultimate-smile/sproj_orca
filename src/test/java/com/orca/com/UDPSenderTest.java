package com.orca.com;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSenderTest {
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            // 准备小端格式的数据（示例：将整数1234转换为小端字节数组）
            int value = 1234;
            byte[] data = new byte[4];
            data[0] = (byte) (value & 0xFF);
            data[1] = (byte) ((value >> 8) & 0xFF);
            data[2] = (byte) ((value >> 16) & 0xFF);
            data[3] = (byte) ((value >> 24) & 0xFF);

            // 创建数据包
            InetAddress address = InetAddress.getByName("127.0.0.1");
            int port = 8888;
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

            // 发送数据包
            socket.send(packet);
            System.out.println("数据发送成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
