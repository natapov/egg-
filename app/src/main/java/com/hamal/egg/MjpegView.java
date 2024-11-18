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
    private final int max_width;
    private final int max_height;
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
    private String ip;
    private FrameLayout camera_frame;
    private boolean is_zoom;
    private SurfaceHolder last_used_holder = null;


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
        max_width = width;
        max_height = height;
        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // max size bm for reuse
        options.inMutable = true;
        options.inBitmap = bm;

        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) { }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                last_used_holder = null;
                stopPlayback();
                setRecording(false);
            }
        });
    }

    public void cleanup(){
        last_used_holder = null;
        stopPlayback();
        setRecording(false);
        bm.recycle();
        bm = null;
    }

    public int getXSize(){
        if (is_zoom){
            return max_height; // height because the frame is rotated
        }
        else {
            return SettingsFragment.getXSize(sharedPreferences);
        }
    }
    public int getYSize(){
        if (is_zoom){
            return max_width; // width because the frame is rotated
        }
        else {
            return SettingsFragment.getYSize(sharedPreferences);
        }
    }

    public void change_quality_if_needed() {
        int x_size = getXSize();
        assert bm != null;
        HttpURLConnection configConnection = null;

        try {
            if (x_size != bm.getWidth()) {
                closeConnectionAndDataInput();
                Log.i(cam_name, "Connecting to change quality");
                String config_string = "http://" + ip + port + "/config?" + "fps=" + "12" + "&resx=" + x_size + "&resy=" + getYSize();
                URL config_url = new URL(config_string); // todo exception
                configConnection = (HttpURLConnection) config_url.openConnection();
                configConnection.setConnectTimeout(100);
                configConnection.setRequestMethod("GET");
                configConnection.setReadTimeout(100);
                int responseCode = configConnection.getResponseCode();
            }
        } catch (IOException e) {
            Log.w(cam_name, "Failed when changing quality, continuing.. ",  e);
        } finally {
            if (configConnection != null){
                configConnection.disconnect();
            }
        }
    }

    public void actually_connect_to_egg() throws IOException {
        Log.i(cam_name, "Opening connection");
        assert data_input == null;
        assert connection == null;
        connection = (HttpURLConnection) stream_url.openConnection();
        connection.setConnectTimeout(300);
        connection.setReadTimeout(300);
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
        assert data_input != null;
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
        byte[] frame_buffer;
        while(is_run){
            Log.i(cam_name, "Start frame loop");
            try {
                if (data_input == null) { // reconnect every frame mode, we check data_input directly for robustness
                    actually_connect_to_egg();
                    assert data_input != null;
                }
                frame_buffer = read_frame();
                assert frame_buffer != null;
                assert (frame_buffer.length > 0);
            }
            catch (IOException e){
                camera_frame.setBackgroundColor(Color.RED);
                if (!is_run){
                    return;
                }
                throw e;
            }
            finally {
                if (sharedPreferences.getBoolean("reconnect_mode", false)) {
                    closeConnectionAndDataInput();
                }
            }
            assert frame_buffer != null;
            assert bm != null;
            bm = BitmapFactory.decodeByteArray(frame_buffer, 0, frame_buffer.length, options);
            try {
                last_used_holder = getHolder();
                canvas = last_used_holder.lockCanvas();
                if (canvas == null) {
                    camera_frame.setBackgroundColor(Color.RED);
                    Log.w(cam_name, "null canvas, skipping render");
                    continue;
                }
                canvas.save();
                if (is_zoom) {
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
                camera_frame.setBackgroundColor(Color.GRAY);
            }
            finally {
                if (canvas != null) {
                    try {
                        last_used_holder.unlockCanvasAndPost(canvas);
                    }
                    catch (IllegalStateException e){
                        Log.w(cam_name, "canvas issue", e);
                    }
                }
                canvas = null;
            }
            if (is_recording && bm.getWidth() == getXSize()) { // make sure quality has updated so that we don't switch frame size mid recording
                try {
                    assert frame_buffer != null;
                    assert bm.getHeight() == getYSize();
                    recording_handler.capture_frame(frame_buffer);
                }
                catch (Exception recording_e){
                    Log.e(cam_name, "exception occurred while recording frame", recording_e);
                }
            }
            change_quality_if_needed();
        }
    }
    public boolean toggleRecording(){
        return setRecording(!is_recording);
    }
    public boolean setRecording(boolean new_state) {
        if (is_recording != new_state) {
            is_recording = new_state;
            if (new_state) {
                recording_handler.startRecording(cam_name, getXSize(), getYSize());
            } else {
                recording_handler.stopRecording();
            }

        }
        if (recording_button != null)
            recording_button.setSelected(is_recording);
        return is_recording;
    }
    public synchronized void connect() {
        Log.i(cam_name, "Starting thread: " + Thread.currentThread().getName());
        is_run = true;
        while(is_run) {
            camera_frame.setBackgroundColor(Color.RED);
            try {
                ip = ip_provider.get_ip();
                String url_string = "http://" + ip + port + "/stream.mjpg";
                try {
                    stream_url = new URL(url_string);
                } catch (MalformedURLException e) {
                    Log.e(cam_name, "Bad url given:" + url_string, e);
                    return;
                }
                run_loop();
            } catch (InterruptedException e) {
                Log.e(cam_name, "thread interrupted, halting", e);
                camera_frame.setBackgroundColor(Color.RED);
            } catch (Exception e) {
                Log.e(cam_name, "Restarting draw loop: ", e);
                camera_frame.setBackgroundColor(Color.RED);
                try {
                    sleep(100);
                } catch (InterruptedException ex) {
                    Log.e(cam_name, "Interrupted, exiting: ", e);
                }
            } finally {
                resetState();
            }
        }
        Log.i(cam_name, "Terminating thread: " + Thread.currentThread().getName());
        thread = null; // only the thread itself should clear this value
    }

    public void startPlayback(FrameLayout frame, boolean rotate_cam) {
        camera_frame = frame;
        is_zoom = rotate_cam;
        assert !is_run;
        thread = new Thread(this::connect);
        thread.start();
    }

    public void resetState() {
        if (last_used_holder != null) try {
            last_used_holder.unlockCanvasAndPost(null);
        }
        catch (Exception err){
            Log.w(cam_name, "Error releasing canvas", err);
        }
        closeConnectionAndDataInput();
        assert connection == null;
        assert data_input == null;
    }

    public void closeConnectionAndDataInput() {
        if (data_input != null) try {
            Log.d(cam_name, "Closing connection");
            data_input.close();
        } catch (IOException err) {
            Log.e(cam_name, "Failed to close data_input", err);
        }
        finally {
            data_input = null;
        }
        if (connection != null) {
            connection.disconnect();
        }
        connection = null;
    }
    public void stopPlayback() {
        is_run = false;
        try {
            thread.join(10);
        } catch (InterruptedException ignored) {}
        thread.interrupt();
    }

    private int parseContentLength(byte[] headerBytes) throws IllegalArgumentException {
        String s = new String(headerBytes);
        String CONTENT_LENGTH = "Content-Length: ";
        int start = s.indexOf(CONTENT_LENGTH) + CONTENT_LENGTH.length();
        int end = s.indexOf('\r', start);
        String substring = s.substring(start, end);
        return Integer.parseInt(substring);
    }
}
//192.168.31.220:8008