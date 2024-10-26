package com.hamal.egg;
import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
//import android.net.TetheringManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MjpegView extends SurfaceView{
    private final static int HEADER_MAX_LENGTH = 300; // timestamp limited to 17 chars
    private static final Object tethering_lock = new Object();
    private final static byte[] SOI_MARKER = {'\r', '\n', '\r', '\n'};
    public byte[] headerBuffer = new byte[HEADER_MAX_LENGTH];
    Thread thread = null;
    HttpURLConnection connection = null;
    boolean is_run = false;
    boolean is_recording;
    Bitmap bm;
    URL stream_url;
    BitmapFactory.Options options = new BitmapFactory.Options();
    RecordingHandler  recording_handler;
    DataInputStream data_input = null;
    String port = null;
    public Button recording_button = null;
    MainActivity ip_provider = null;
    Paint fpsPaint = null;
    final SharedPreferences sharedPreferences;
    String cam_name;
    private static final String TAG = "MjpegView";
    private String ip;
    private FrameLayout camera_frame;
    private boolean rotate;


    public MjpegView(Context context, String name, MainActivity model, String url_end, int width, int height) {
        super(context, null);
        recording_handler = new RecordingHandler(context);
        fpsPaint = new Paint();
        fpsPaint.setTextAlign(Paint.Align.LEFT);
        fpsPaint.setTextSize(12);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        cam_name = name;
        port = url_end;
        ip_provider = model;
        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // max size bm for reuse
        options.inMutable = true;
        options.inBitmap = bm;

        this.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) { }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                try {
                    holder.unlockCanvasAndPost(null);
                }
                catch (Exception ignored){}
                //stopPlayback();
                setRecording(false);
            }
        });


    }

    public void change_quality_if_needed() throws IOException{
        int x_size = SettingsFragment.getXSize(sharedPreferences);
        assert bm != null;
        if (x_size != bm.getWidth()){
            String config_string = "http://" + ip + port + "/config?" + "fps=" + "12" + "&resx=" + x_size + "&resy=" + SettingsFragment.getYSize(sharedPreferences);
            URL config_url = new URL(config_string); // todo exception
            HttpURLConnection configConnection = (HttpURLConnection) config_url.openConnection();
            configConnection.setConnectTimeout(30000);
            configConnection.setRequestMethod("GET");
            configConnection.setReadTimeout(30000);
            int responseCode = configConnection.getResponseCode();
            configConnection.disconnect();
        }
    }

    public void actually_connect_to_egg() throws IOException {
        connection = (HttpURLConnection) stream_url.openConnection();
        connection.setDoInput(true);
        connection.connect();
        data_input = new DataInputStream(connection.getInputStream());

    }
    private void read_until_sequence(byte [] buffer, InputStream in, byte[] sequence) throws IOException {
        int seqIndex = 0;
        byte c;
        for (int i = 0; i < HEADER_MAX_LENGTH; i++) {
            c = (byte) in.read();
            buffer[i] = c;
            if (c == sequence[seqIndex]) {
                seqIndex++;
                if (seqIndex == sequence.length) {
                    return;
                }
            } else {
                seqIndex = 0;
            }
        }
        throw new IOException("Bad packet format");
    }
    public byte[] read_frame() throws IOException {
        read_until_sequence(headerBuffer, data_input, SOI_MARKER);
        int contentLength = parseContentLength(headerBuffer);
        byte[] frameBuffer = new byte[contentLength];
        data_input.readFully(frameBuffer,0, contentLength);
        return frameBuffer;
    }
    private Bitmap makeFpsOverlay(Paint p, String text) {
        Rect b = new Rect();
        p.getTextBounds(text, 0, text.length(), b);
        final int bwidth = b.width() + 2;
        final int bheight = b.height() + 2;
        Bitmap bm = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        int overlayBackgroundColor = Color.DKGRAY;
        p.setColor(overlayBackgroundColor);
        c.drawRect(0, 0, bwidth, bheight, p);
        int overlayTextColor = Color.WHITE;
        p.setColor(overlayTextColor);
        c.drawText(text, -b.left + 1,
                ((float) bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
        return bm;
    }
    public void run_loop() throws Exception {
        Canvas canvas = null;
        Bitmap ovl = null;
        long last_print_time = 0;
        int frame_count = 0;
        byte[] frame_buffer = null;
        while(is_run){
            try {
                if (sharedPreferences.getBoolean("reconnect_mode", true)){
                    actually_connect_to_egg();
                }
                frame_buffer = read_frame();
                assert frame_buffer != null;
                assert (frame_buffer.length > 0);
            }
            catch (IOException e){
                if (!is_run){
                    return;
                }
                throw e;
            }
            finally {
                if (sharedPreferences.getBoolean("reconnect_mode", true)) {
                    if (data_input != null) try {
                        data_input.close();
                    } catch (IOException e) {
                    }
                }
            }
            assert frame_buffer != null;
            assert bm != null;
            bm = BitmapFactory.decodeByteArray(frame_buffer, 0, frame_buffer.length, options);
            change_quality_if_needed();
            try {
                canvas = this.getHolder().lockCanvas();
                if (canvas == null) {
                    Log.w(TAG, "null canvas, skipping render");
                    continue;
                }
                if (bm != null) {
                    camera_frame.setBackgroundColor(Color.GRAY);
                    canvas.save();
                    if (rotate) {
                        canvas.rotate(90, bm.getWidth() / 2f, bm.getHeight() / 2f);
                    }
                    canvas.drawBitmap(bm, null, canvas.getClipBounds(), null);
                    canvas.restore();

                    if (sharedPreferences.getBoolean("show_fps", true)) {
                        if (ovl != null) {
                            canvas.drawBitmap(ovl, canvas.getWidth() - ovl.getWidth(), canvas.getHeight() - ovl.getHeight(),  null);
                        }
                        frame_count++;

                        long current_time = System.currentTimeMillis();
                        long delta = current_time - last_print_time;
                        if (delta >= 1000) {
                            float actual_fps = frame_count / (delta / 1000f);
                            String fps_text = String.format("%.2f", actual_fps) + "fps " + bm.getWidth() + "x" + bm.getHeight();
                            ovl = makeFpsOverlay(fpsPaint, fps_text);
                            last_print_time = current_time;
                            frame_count = 0;
                        }
                    }
                }
            }
            finally {
                if (canvas != null) {
                    try {
                        this.getHolder().unlockCanvasAndPost(canvas);
                    }
                    catch (IllegalStateException e){
                        Log.w(TAG, "canvas issue", e);
                    }
                }
                canvas = null;
            }
            if (is_recording) {
                try {
                    assert frame_buffer != null;
                    recording_handler.capture_frame(frame_buffer);
                }
                catch (Exception recording_e){
                    Log.e(TAG, "exception occurred while recording frame", recording_e);
                }
            }
        }
    }
    public boolean toggleRecording(){
        return setRecording(!is_recording);
    }
    public boolean setRecording(boolean new_state) {
        if (is_recording != new_state) {
            is_recording = new_state;
            if (new_state)
                recording_handler.startRecording(cam_name);
            else
                recording_handler.stopRecording();

        }
        if (recording_button != null)
            recording_button.setSelected(is_recording);
        return is_recording;
    }
    public void connect() {
        for(int i = 0;; i++) {
            startTether(); // check that tethering is on
            try {
                ip = ip_provider.get_ip();
                String url_string = "http://" + ip + port + "/stream.mjpg";
                try {
                    stream_url = new URL(url_string);
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Bad url given:" + url_string, e);
                    return;
                }
                if (!sharedPreferences.getBoolean("reconnect_mode", true)) {
                    actually_connect_to_egg();
                }
                run_loop();
            }
            catch (InterruptedException e) {
                Log.e(TAG, "thread interrupted, halting", e);
                camera_frame.setBackgroundColor(Color.RED);
            }
            catch (Exception e){
                Log.e(TAG, "Restarting draw loop: ", e);
                camera_frame.setBackgroundColor(Color.RED);
                try {
                    sleep(1);
                } catch (InterruptedException ex) {
                    assert(!is_run);
                    break;
                }
                continue; // try again
            }
//            catch (Exception e) {
//                Log.e("kuku", "can't handle, pls restart me", e);
//            }
            assert(!is_run);
            Log.i(TAG, "thread terminating: " + Thread.currentThread().getName());
            break;
        }
    }

    public void startPlayback(FrameLayout frame, boolean rotate_cam) {
        camera_frame = frame;
        rotate = rotate_cam;
        is_run = true;
        thread = new Thread(this::connect);
        thread.start();
    }

    public void stopPlayback()  {
        is_run = false;
        try {
            thread.join(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thread.interrupt();
        thread = null;
    }

    private int parseContentLength(byte[] headerBytes) throws IllegalArgumentException {
        String s = new String(headerBytes);
        String CONTENT_LENGTH = "Content-Length: ";
        int start = s.indexOf(CONTENT_LENGTH) + CONTENT_LENGTH.length();
        int end = s.indexOf('\r', start);
        String substring = s.substring(start, end);
        return Integer.parseInt(substring);
    }
    private void startTether(){
//        synchronized(tethering_lock) {
//            WifiManager wifi = mContext.getSystemService(WifiManager.class);
//            while(wifi.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLING) {}
//            if (wifi.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
//                return;
//            }
//            TetheringManager.StartTetheringCallback callback = new TetheringManager.StartTetheringCallback() {
//                @Override
//                public void onTetheringStarted() {
//                    // Tethering started successfully
//                }
//
//                @Override
//                public void onTetheringFailed(int error) {
//                    //@todo log and try again
//                }
//            };
//            TetheringManager tetheringManager = mContext.getSystemService(TetheringManager.class);
//            TetheringManager.TetheringRequest request = new TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI).build();
//            tetheringManager.startTethering(request, Runnable::run, callback);
//        }
    }
}
