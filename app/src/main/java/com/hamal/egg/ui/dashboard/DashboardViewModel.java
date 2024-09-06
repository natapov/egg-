package com.hamal.egg.ui.dashboard;

import androidx.lifecycle.ViewModel;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class DashboardViewModel extends ViewModel {
    private static final int UDP_PORT = 8888;
    Thread thread = null;

    public DashboardViewModel() {
    }
    public void Start(){
        thread = new Thread(this::listen_for_ip);
        thread.start();
    }

    public void listen_for_ip() {
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            byte[] buffer = new byte[1024];
            // add lock for first time,
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String k = new String(packet.getData(), 0, packet.getLength());
            }
        } catch (IOException e) {
        }
    }
}