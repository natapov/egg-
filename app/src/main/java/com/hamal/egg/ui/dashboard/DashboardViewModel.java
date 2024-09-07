package com.hamal.egg.ui.dashboard;

import android.os.Build;

import androidx.lifecycle.ViewModel;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class DashboardViewModel extends ViewModel {
    private static final int UDP_PORT = 8888;
    private Thread thread = null;
    private volatile String latestReceivedString = null;
    private final Object lock = new Object();

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
               // allowCleartextForHost(receivedString);
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
            if (latestReceivedString == null) {
                lock.wait(); // Wait until notified
            }
            assert latestReceivedString != null;
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

    private void allowCleartextForHost(String host) {
            try {
                Class<?> nspClass = Class.forName("android.security.NetworkSecurityPolicy");
                Method getInstanceMethod = nspClass.getMethod("getInstance");
                Object nspInstance = getInstanceMethod.invoke(null);

                Class<?> strictModeVmPolicyClass = Class.forName("android.os.StrictMode$VmPolicy");
                Method allowCleartextMethod = nspClass.getMethod("setCleartextTrafficPermitted", String.class, boolean.class);
                allowCleartextMethod.invoke(nspInstance, host, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

}