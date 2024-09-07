package com.hamal.egg.ui.dashboard;

import androidx.lifecycle.ViewModel;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class DashboardViewModel extends ViewModel {
    private static final int UDP_PORT = 8888;
    private Thread thread = null;
    private volatile String latestReceivedString = null;
    private final Object lock = new Object();
    private boolean firstUpdateReceived = false;

    public DashboardViewModel() {
    }

    public void Start() {
        thread = new Thread(this::listen_for_ip);
        thread.start();
    }

    public void listen_for_ip() {
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedString = new String(packet.getData(), 0, packet.getLength());
                synchronized (lock) {
                    latestReceivedString = receivedString;
                    lock.notifyAll(); // Notify all waiting threads
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String get_ip() throws InterruptedException {
        synchronized (lock) {
            while (latestReceivedString == null) {
                lock.wait(); // Wait until notified
            }
            return latestReceivedString;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (thread != null) {
            thread.interrupt();
        }
    }
}