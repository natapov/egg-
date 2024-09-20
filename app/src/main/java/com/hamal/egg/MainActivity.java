package com.hamal.egg;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.hamal.egg.databinding.ActivityMainBinding;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static final String TAG = "MainActivity";
    private static final int UDP_PORT = 8888;
    private Thread thread = null;
    private volatile String latestReceivedString = null;
    private final Object lock = new Object();
    private boolean running = true;
    public void Start() {
        thread = new Thread(this::listen_for_ip);
        thread.start();
    }

    public void listen_for_ip() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(UDP_PORT);
            byte[] buffer = new byte[1024];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedString = new String(packet.getData(), 0, packet.getLength());
                synchronized (lock) {
                    latestReceivedString = receivedString;
                    lock.notifyAll(); // Notify all waiting threads
                }
            }
        } catch (Exception e) {
            Log.e("kuku", "error", e);
        } finally {
            try {
                socket.close();
            }catch (Exception e) {
                Log.e("kuku", "error", e);
            }
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
        Start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (thread == null){
            Start();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        super.onDestroy();
    }
}
