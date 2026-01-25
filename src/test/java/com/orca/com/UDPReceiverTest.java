package com.orca.com;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceiverTest {
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(8888)) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // 接收数据包
            socket.receive(packet);
            byte[] data = packet.getData();
            int length = packet.getLength();

            // 解析小端格式的数据（示例：将接收到的4字节数据转换为整数）
            int value = (data[3] & 0xFF) << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
            System.out.println("接收到的数据（小端格式解析后）：" + value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
