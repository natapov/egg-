package com.hamal.egg;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
//import android.net.TetheringManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;


public class MjpegView extends SurfaceView{
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int FRAME_MAX_LENGTH = 150000;
    private static final Object tethering_lock = new Object();


    private final static byte[] SOI_MARKER = {'\r', '\n', '\r', '\n'};
    public byte[] frameBuffer = new byte[FRAME_MAX_LENGTH];
    public byte[] headerBuffer = new byte[HEADER_MAX_LENGTH];
    private final String CONTENT_LENGTH = "Content-Length: ";
    private File last_frame;
    Thread thread = null;
    boolean is_run = false;
    ContentResolver mResolver;
    Uri mUri;
    Rect dest_rect = null;
    Bitmap bm;
    BitmapFactory.Options options = new BitmapFactory.Options();
    SurfaceHolder holder = null;
    Exception last_thread_exception = null;
    private Context mContext;
    private String url = null;

    public MjpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mResolver = mContext.getContentResolver();

//        ContentValues contentValues = new ContentValues();
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "last_frame");
//        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
//        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
//        mUri = mResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//
//        last_frame = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "last_frame.jpg");
        options.inMutable = true;
        holder = this.getHolder();

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // Code to execute when the surface dimensions or format change
            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
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
    public void record_frame(){
        try (OutputStream outputStream = mResolver.openOutputStream(mUri)) {
            outputStream.write(frameBuffer);
        }catch (Exception e){}
    }
    public int read_frame() throws IOException {
        DataInputStream bis = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(this.url).openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(30);
            connection.setReadTimeout(300);
            connection.connect();
            bis = new DataInputStream(connection.getInputStream());
            read_until_sequence(headerBuffer, bis, SOI_MARKER);
            int contentLength = parseContentLength(headerBuffer);

            bis.readFully(frameBuffer,0, contentLength);
            // record_frame();
            return contentLength;
        }
        finally {
            if (bis != null) try {
                bis.close();
            } catch (IOException e) {
            }
            if (connection != null) connection.disconnect();
        }
    }
    private Rect destRect(int bmw, int bmh) {
        int x = (getWidth() / 2) - (bmw / 2);
        int y = (getHeight() / 2) - (bmh / 2);
        return new Rect(x, y, bmw + x, bmh + y);
    }
    public void run_loop() throws IOException {
        boolean first_frame = true;
        Canvas canvas = null;
        int allowed_failed_frames = 100;
        Paint frame_paint = new Paint();
        frame_paint.setStyle(Paint.Style.STROKE);
        final int stroke_width = 4;
        final int frame_offset = stroke_width/2;
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
                Log.e("Draw loop", Arrays.toString(e.getStackTrace()));
                read_success = false;
                frame_paint.setColor(Color.RED);
                if(allowed_failed_frames-- == 0) {
                    throw e;
                }
            }
            try {
                canvas = holder.lockCanvas();
            } catch (Exception ignored){}
            if (canvas == null) {
                Log.w("draw thread", "null canvas, skipping render");
                continue;
            }
            if (read_success) {
                bm = BitmapFactory.decodeByteArray(frameBuffer, 0, bytesRead, options);
                if (first_frame ) {
                    dest_rect = destRect(bm.getWidth(), bm.getHeight());
                    options.inBitmap = bm; //reuse bm after first time
                    first_frame = false;
                }
                frame_paint.setColor(Color.GRAY);
            }
            canvas.drawRect(dest_rect.left - frame_offset,
                    dest_rect.top - frame_offset,
                    dest_rect.right + frame_offset,
                    dest_rect.bottom + frame_offset,
                    frame_paint);
            canvas.drawBitmap(bm, null, dest_rect, null); // redraw the last frame even if fail, otherwise will show on even older frame that's still on the backbuffer
            assert(canvas != null);
            holder.unlockCanvasAndPost(canvas);
        }
    }
    public void connect() {
        for(int i = 0;; i++) {
            startTether(); // check that tethering is on
            try {
                run_loop();
            }
            catch (Exception e){
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
