package com.hamal.egg;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.hamal.egg.databinding.ActivityMainBinding;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    public MjpegView camera1;
    public MjpegView camera2;
    public MjpegView camera3;
    private static final String TAG = "Cameras model";

    private static final int UDP_PORT = 8888;
    private Thread thread = null;
    private volatile String latestReceivedString = null;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    private boolean running = true;
    public void initializeCameras() {
        Resources res = getResources();

        if (camera1 == null) {
            camera1 = new MjpegView(this, "cam_1", ":8008",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
        if (camera2 == null) {
            camera2 = new MjpegView(this, "cam_2", ":9800",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
        if (camera3 == null) {
            camera3 = new MjpegView(this, "cam_3", ":9801",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
        if (thread == null){
            thread = new Thread(this::listen_for_ip);
            thread.start();
        }
    }

    public void listen_for_ip() {
        DatagramSocket socket = null;
        while (running) {
            try {
                socket = new DatagramSocket(UDP_PORT);
                byte[] buffer = new byte[1024];
                while (running) {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeCameras();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}
