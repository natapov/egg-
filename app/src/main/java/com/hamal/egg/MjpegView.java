package com.hamal.egg;
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
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class MjpegView extends SurfaceView{
    private final static int HEADER_MAX_LENGTH = 100;
    final static int stroke_width = 4;
    final static int frame_offset = stroke_width/2;
    static final int default_width = 640;
    static final int default_height = 360;
    private static final Object tethering_lock = new Object();
    private final static byte[] SOI_MARKER = {'\r', '\n', '\r', '\n'};
    public byte[] headerBuffer = new byte[HEADER_MAX_LENGTH];
    Thread thread = null;
    HttpURLConnection connection = null;
    boolean is_run = false;
    boolean is_recording;
    Rect dest_rect = null;
    Bitmap bm;
    URL url;
    BitmapFactory.Options options = new BitmapFactory.Options();
    SurfaceHolder holder = null;
    Exception last_thread_exception = null;
    RecordingHandler  recording_handler;
    DataInputStream data_input = null;
    String m_url_end = null;
    MainActivity ip_provider = null;
    Paint fpsPaint = null;
    SharedPreferences sharedPreferences = null;
    String cam_name;
    public MjpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        recording_handler = new RecordingHandler(context);
        options.inMutable = true;
        holder = this.getHolder();
        fpsPaint = new Paint();
        fpsPaint.setTextAlign(Paint.Align.LEFT);
        fpsPaint.setTextSize(12);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        cam_name = getResources().getResourceEntryName(getId());

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                dest_rect = destRect(default_width, default_height);
            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                // Code to execute when the surface dimensions or format change
            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                setRecording(false);
                stopPlayback();
            }
        });
    }
    public void prepare_connection() throws IOException {
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
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
        read_until_sequence(headerBuffer, data_input, SOI_MARKER);
        int contentLength = parseContentLength(headerBuffer);
        byte[] frameBuffer = new byte[contentLength];
        data_input.readFully(frameBuffer,0, contentLength);
        return frameBuffer;
    }
    private Rect destRect(int bmw, int bmh) {
        final int x = (getWidth() / 2) - (bmw / 2);
        final int y = (getHeight() / 2) - (bmh / 2);
        return new Rect(x, y, bmw + x, bmh + y);
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
        boolean first_frame = true;
        Canvas canvas = null;
        Paint frame_paint = new Paint();
        frame_paint.setStyle(Paint.Style.STROKE);
        frame_paint.setStrokeWidth(stroke_width);
        Exception read_exception = null;
        Bitmap ovl = null;
        long last_print_time = 0;
        int frame_count = 0;
        byte[] frame_buffer = null;
        while(is_run){
            try {
                if (sharedPreferences.getBoolean("reconnect_mode", true)){
                    prepare_connection();
                }
                frame_buffer = read_frame();
                assert frame_buffer != null;
                assert (frame_buffer.length > 0);
            }
            catch (IOException e){
                read_exception = e;
                frame_paint.setColor(Color.RED);
            }
            finally {
                if (sharedPreferences.getBoolean("reconnect_mode", true)) {
                    if (data_input != null) try {
                        data_input.close();
                    } catch (IOException e) {
                    }
                }
            }
            if (read_exception == null) {
                assert frame_buffer != null;
                bm = BitmapFactory.decodeByteArray(frame_buffer, 0, frame_buffer.length, options);
                if (first_frame) {
                    options.inBitmap = bm; //reuse bm after first time
                }
                frame_paint.setColor(Color.GRAY);
            }
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) {
                    Log.w("draw thread", "null canvas, skipping render");
                    throw new Exception("canvas problem");
                }
                canvas.drawRect(dest_rect.left - frame_offset,
                        dest_rect.top - frame_offset,
                        dest_rect.right + frame_offset,
                        dest_rect.bottom + frame_offset,
                        frame_paint);
                canvas.drawBitmap(bm, null, dest_rect, null); // redraw the last frame even if fail, otherwise will show on even older frame that's still on the backbuffer
                if (sharedPreferences.getBoolean("show_fps", true)) {
                    if (ovl != null) {
                        int height = dest_rect.bottom - ovl.getHeight();
                        int width = dest_rect.right - ovl.getWidth();
                        canvas.drawBitmap(ovl, width, height, null);
                    }
                    frame_count++;

                    long current_time = System.currentTimeMillis();
                    long delta = current_time - last_print_time;
                    if (delta >= 1000) {
                        float actual_fps = frame_count/(delta/1000f);
                        String fps_text = String.format("%.2f", actual_fps) + "fps";
                        ovl = makeFpsOverlay(fpsPaint, fps_text);
                        last_print_time = current_time;
                        frame_count = 0;
                    }
                }
            }
            finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                    canvas = null;
                }
                if(read_exception != null){
                    throw read_exception;
                }
            }

            if (is_recording) {
                try {
                    assert frame_buffer != null;
                    recording_handler.capture_frame(frame_buffer);
                }
                catch (Exception recording_e){
                    Log.e("recording", "exception occurred", recording_e);
                }
            }
            frame_buffer = null;
        }
    }
    public boolean toggleRecording(){
        return setRecording(!is_recording);
    }
    public boolean setRecording(boolean on) {
        if (is_recording != on) {
            is_recording = on;
            if (is_recording) {
                recording_handler.startRecording(cam_name);
            } else {
                recording_handler.stopRecording();
            }
        }
        return is_recording;
    }
    public void connect() {
        for(int i = 0;; i++) {
            startTether(); // check that tethering is on
            try {
                String url_string = "http://" + ip_provider.get_ip() + m_url_end;
                try {
                    url = new URL(url_string);
                } catch (MalformedURLException e) {
                    Log.e("startPlayback", "Bad url given:" + url_string, e);
                    return;
                }
                if (!sharedPreferences.getBoolean("reconnect_mode", true)) {
                    prepare_connection();
                }
                run_loop();
            }
            catch (Exception e){
                last_thread_exception = e;
                Log.e("Restarting draw loop", "got exception: ", e);
                continue; // try again
            }
            last_thread_exception = null;
            break;
        }
    }

    public void startPlayback(MainActivity model, String url_end) {
        m_url_end = url_end;
        ip_provider = model;
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
