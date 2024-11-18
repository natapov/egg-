package com.hamal.egg;

import android.content.Context;
import android.content.res.Resources;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;
import android.util.Log;
import androidx.lifecycle.ViewModel;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CountDownLatch;

public class CamerasModel extends ViewModel {
    public MjpegView camera1;
    public MjpegView camera2;
    public MjpegView camera3;
    private static final String TAG = "Cameras model";

    private static final int UDP_PORT = 8888;
    private Thread thread = null;
    private volatile String latestReceivedString = null;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    private boolean running = true;
    WifiManager wifi;
    TetheringManager tetheringManager;
    // Create cameras if they don't exist
    public void initializeCameras(Context context) {
        Resources res = context.getResources();
        wifi = context.getSystemService(WifiManager.class);
        tetheringManager = context.getSystemService(TetheringManager.class);

        if (camera1 == null) {
            camera1 = new MjpegView(context, "cam_1", this, ":8008",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
        if (camera2 == null) {
            camera2 = new MjpegView(context, "cam_2", this, ":9800",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
        if (camera3 == null) {
            camera3 = new MjpegView(context, "cam_3", this, ":9801",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
        if (thread == null){
            thread = new Thread(this::listen_for_ip);
            thread.start();
        }
    }


    private void startTether(){
        if (wifi.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED || wifi.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLING) {
            return;
        }
        TetheringManager.StartTetheringCallback callback = new TetheringManager.StartTetheringCallback() {
            @Override
            public void onTetheringStarted() {
                // Tethering started successfully
            }

            @Override
            public void onTetheringFailed(int error) {
                //@todo log and try again
            }
        };
        TetheringManager.TetheringRequest request = new TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI).build();
        tetheringManager.startTethering(request, Runnable::run, callback);
    }
    public void listen_for_ip() {
        DatagramSocket socket = null;
        while (running) {
            startTether();
            try {
                socket = new DatagramSocket(UDP_PORT);
                byte[] buffer = new byte[1024];
                while (running) {
                    startTether();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String receivedString = new String(packet.getData(), 0, packet.getLength());
                    latestReceivedString = receivedString;
                    initLatch.countDown();
                }
            } catch (Exception e) {
                Log.e(TAG, "in listen_for_ip", e);
            } finally {
                if (!running)try {
                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't close socket", e);
                }
            }
        }
    }

    public String get_ip() throws InterruptedException {
        initLatch.await();
        assert latestReceivedString != null;
        return latestReceivedString;
    }
    public String sample_ip() {
        if (latestReceivedString == null) {
            return "N/A";
        }
        return latestReceivedString;
    }

    public MjpegView getCamera(int n) {
        switch (n) {
            case 2:
                return camera2;
            case 3:
                return camera3;
            default:
                return camera1;
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (camera1 != null) {
            camera1.cleanup();
            camera1 = null;
        }
        if (camera2 != null) {
            camera2.cleanup();
            camera2 = null;
        }
        if (camera3 != null) {
            camera3.cleanup();
            camera3 = null;
        }
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }
}