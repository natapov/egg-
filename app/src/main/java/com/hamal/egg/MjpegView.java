package com.hamal.egg;
import android.content.Context;
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

import androidx.annotation.NonNull;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class MjpegView extends SurfaceView{
    private final static int HEADER_MAX_LENGTH = 100;
    final static int stroke_width = 4;
    final static int frame_offset = stroke_width/2;
    private final static int FRAME_MAX_LENGTH = 150000;
    private static final Object tethering_lock = new Object();
    private final static byte[] SOI_MARKER = {'\r', '\n', '\r', '\n'};
    public byte[] frameBuffer = new byte[FRAME_MAX_LENGTH];
    public byte[] headerBuffer = new byte[HEADER_MAX_LENGTH];
    private final String CONTENT_LENGTH = "Content-Length: ";
    Thread thread = null;
    boolean is_run = false;
    boolean is_recording;
    Rect dest_rect = null;
    static final int default_width = 640;
    static final int default_height= 360;
    Bitmap bm;
    BitmapFactory.Options options = new BitmapFactory.Options();
    SurfaceHolder holder = null;
    Exception last_thread_exception = null;
    RecordingHandler  recording_handler;
    private String url = null;
    public MjpegView(Context context, AttributeSet attrs) throws Exception{ //todo handle exceptions
        super(context, attrs);
        recording_handler = new RecordingHandler(context);
        options.inMutable = true;
        holder = this.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                dest_rect = destRect(default_width, default_height); //todo verify how constant this really is
            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                // Code to execute when the surface dimensions or format change
            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                stopRecording();
                stopPlayback();
            }
        });

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
    public int read_frame() throws IOException {
        DataInputStream data_input = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(this.url).openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(300);
            connection.setReadTimeout(300);
            connection.connect();
            data_input = new DataInputStream(connection.getInputStream());
            read_until_sequence(headerBuffer, data_input, SOI_MARKER);
            int contentLength = parseContentLength(headerBuffer);

            data_input.readFully(frameBuffer,0, contentLength);
            return contentLength;
        }
        finally {
            if (data_input != null) try {
                data_input.close();
            } catch (IOException e) {
            }
            if (connection != null) connection.disconnect();
        }
    }
    private Rect destRect(int bmw, int bmh) {
        final int x = (getWidth() / 2) - (bmw / 2);
        final int y = (getHeight() / 2) - (bmh / 2);
        return new Rect(x, y, bmw + x, bmh + y);
    }
    public void run_loop() throws IOException {
        boolean first_frame = true;
        Canvas canvas = null;
        int dropped_frames = 0;
        int recorded_frames = 0;
        final int max_allowed_dropped_frames = 10; // in a row
        Paint frame_paint = new Paint();
        frame_paint.setStyle(Paint.Style.STROKE);
        frame_paint.setStrokeWidth(stroke_width);
        boolean read_success = false;
        while(is_run){
            int bytesRead = 0;
            read_success = true;
            try {
                bytesRead = read_frame();
                assert (bytesRead > 0);
            }
            catch (Exception e){
                Log.e("read_frame", Arrays.toString(e.getStackTrace()));
                read_success = false;
                frame_paint.setColor(Color.RED);
                if(dropped_frames++ == max_allowed_dropped_frames) {
                    throw e;
                }
            }
            if (read_success) {
                dropped_frames = 0;
                bm = BitmapFactory.decodeByteArray(frameBuffer, 0, bytesRead, options);
                if (first_frame) {
                    options.inBitmap = bm; //reuse bm after first time
                    first_frame = false;
                }
                frame_paint.setColor(Color.GRAY);
            }
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) {
                    Log.w("draw thread", "null canvas, skipping render");
                    continue;
                }
                canvas.drawRect(dest_rect.left - frame_offset,
                        dest_rect.top - frame_offset,
                        dest_rect.right + frame_offset,
                        dest_rect.bottom + frame_offset,
                        frame_paint);
                if(!first_frame) {
                    canvas.drawBitmap(bm, null, dest_rect, null); // redraw the last frame even if fail, otherwise will show on even older frame that's still on the backbuffer
                }
            }
            catch (Exception canvas_e){
                Log.e("draw_to_canvas", Arrays.toString(canvas_e.getStackTrace()));
                if(dropped_frames++ == max_allowed_dropped_frames) {
                    throw canvas_e;
                }
            }
            finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                    canvas = null;
                }
            }

            if (is_recording && read_success) {
                try {
                    String header = String.format("Frame: %d \r\n", recorded_frames + dropped_frames);
                    recording_handler.capture_frame(frameBuffer, bytesRead, header.getBytes(), header.length());
                    recorded_frames += 1;
                }
                catch (Exception recording_e){
                    Log.e("recording", recording_e.toString());
                }
            }
        }
    }
    public boolean toggleRecording(){
        if (is_recording) {
            stopRecording();
        }
        else {
            startRecording();
        }
        return is_recording;
    }

    public void startRecording() {
        assert(!is_recording);
        recording_handler.startRecording();
        is_recording = true;
    }
    public void stopRecording() {
        if (is_recording) {
            try {
                recording_handler.stopRecording();
            }
            catch (IOException e){
                Log.e("add a tag", e.toString());

            }
        }
        is_recording = false;
    }
    public void connect() {
        for(int i = 0;; i++) {
            startTether(); // check that tethering is on
            try {
                run_loop();
            }
            catch (IOException e){
                last_thread_exception = e;
                Log.e("Restarting draw loop", "got exception: " + Arrays.toString(e.getStackTrace()));
                continue; // try again
            }
            last_thread_exception = null;
            break;
        }
    }

    public void startPlayback(String url) {
        this.url = url;
        thread = new Thread(this::connect);
        is_run = true;
        thread.start();
        if (last_thread_exception != null) {
            Log.e("Too many restarts", "Got exception: " + Arrays.toString(last_thread_exception.getStackTrace()));
        }
    }

    public void stopPlayback()  {
        is_run = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    private int parseContentLength(byte[] headerBytes) throws IllegalArgumentException {
        String s = new String(headerBytes);
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
