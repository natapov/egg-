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
import android.net.TetheringManager;
import android.net.wifi.WifiManager;
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
import java.util.concurrent.CountDownLatch;
public class MjpegView extends SurfaceView {
    private static final String TAG = "MjpegView";
    private static final int HEADER_MAX_LENGTH = 300;
    private static final byte[] SOI_MARKER = {'\r', '\n', '\r', '\n'};
    private static final long CONNECT_TIMEOUT_MS = 300;
    private static final long READ_TIMEOUT_MS = 300;
    private static final long RETRY_DELAY_MS = 100;

    // Configuration
    private final int maxWidth;
    private final int maxHeight;
    private final String portEndpoint;
    private final String cameraName;
    private final MainActivity context;
    private final SharedPreferences sharedPreferences;
    private volatile boolean isZoom;

    // UI Elements
    private FrameLayout cameraFrame;
    public Button recordingButton;
    private final Paint fpsPaint;

    // State
    private volatile boolean isRunning = false;
    private volatile boolean isRecording = false;
    private volatile String currentIp;
    private URL streamUrl;

    // Threading
    private final Object connectionLock = new Object();
    private final CountDownLatch surfaceReadyLatch = new CountDownLatch(1);
    private volatile Thread workerThread;

    // Camera Connection
    private HttpURLConnection connection;
    private DataInputStream dataInput;

    // Image Processing
    private final BitmapFactory.Options bitmapOptions;
    private Bitmap reusableBitmap;
    private final RecordingHandler recordingHandler;
    private final byte[] headerBuffer = new byte[HEADER_MAX_LENGTH];
    // FPS counting
    private long lastFpsUpdate = 0;
    private int framesSinceUpdate;


    public MjpegView(Context context, String name, String urlEnd, int width, int height) {
        super(context, null);
        this.context = (MainActivity) context;
        this.cameraName = name;
        this.portEndpoint = urlEnd;
        this.maxWidth = width;
        this.maxHeight = height;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Initialize image processing objects
        this.recordingHandler = new RecordingHandler(context);
        this.reusableBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        bitmapOptions.inBitmap = reusableBitmap;

        // Initialize UI elements
        this.fpsPaint = new Paint();
        fpsPaint.setTextAlign(Paint.Align.LEFT);
        fpsPaint.setTextSize(12);

        setupSurfaceCallback();
    }

    private void setupSurfaceCallback() {
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) { }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                stopPlayback();
                setRecording(false);
            }
        });
    }

    public void startPlayback(FrameLayout frame, boolean rotateCam) {
        stopPlayback(); // Ensure clean state

        cameraFrame = frame;
        isZoom = rotateCam;
        isRunning = true;

        workerThread = new Thread(this::streamingLoop, "MjpegView-" + cameraName);
        workerThread.start();
    }

    public void stopPlayback() {
        isRunning = false;
        setRecording(false);

        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            workerThread = null;
        }

        closeConnection();
    }

    private void streamingLoop() {
        Log.i(cameraName, "Starting streaming loop");
        while (isRunning) {
            startTether(); // always make sure tethering is turned on
            try {
                updateStreamUrl();
                processFrames();
            } catch (InterruptedException e) {
                Log.i(cameraName, "Stream interrupted", e);
                break;
            } catch (Exception e) {
                Log.e(cameraName, "Error in streaming loop", e);
                setBackgroundColorOnUiThread(Color.RED);

                if (!isRunning) break;

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } finally {
                closeConnection();
            }
        }

        Log.i(cameraName, "Streaming loop terminated");
    }

    private Bitmap createFpsOverlay(Bitmap oldOverlay, int width, int height ) {
        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastFpsUpdate;
        if (timeDelta >= 1000) {
            float fps = framesSinceUpdate / (timeDelta / 1000f);
            String fpsText = String.format("%.2f fps %dx%d", fps, width, height);
            // Create new overlay bitmap without holding canvas lock
            Bitmap newOverlay = drawFpsOverlay(fpsText);

            // Safely swap overlay bitmaps
            if (oldOverlay != null) {
                oldOverlay.recycle();
            }
            lastFpsUpdate = currentTime;
            framesSinceUpdate = 0;
            return newOverlay;
        }
        else {
            framesSinceUpdate++;
            return oldOverlay;
        }
    }
    private void processFrames() throws Exception {
        Bitmap overlayBitmap = null;
        while (isRunning) {
            FrameData frameData = captureFrame();
            if (frameData == null) continue;
            Bitmap bm = frameData.bitmap;

            try {
                if (sharedPreferences.getBoolean("show_fps", true)) {
                    overlayBitmap = createFpsOverlay(overlayBitmap, bm.getWidth(), bm.getHeight());
                }
                drawFrame(bm, overlayBitmap);
                setBackgroundColorOnUiThread(Color.GRAY);

                // Handle recording if needed
                if (isRecording && bm.getWidth() == getXSize()) {
                    recordingHandler.capture_frame(frameData.data);
                }

                // Check if quality adjustment is needed
                if (bm.getWidth() != getXSize()) {
                    adjustQuality();
                }

            } finally {
                if (bm != reusableBitmap) {
                    bm.recycle();
                }
            }
        }
    }

    private Bitmap drawFpsOverlay(String text) {
        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setTextAlign(Paint.Align.LEFT);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        int width = bounds.width() + 2;
        int height = bounds.height() + 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        paint.setColor(Color.DKGRAY);
        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText(text, -bounds.left + 1,
                (height / 2f) - ((paint.ascent() + paint.descent()) / 2) + 1, paint);

        return bitmap;
    }
    private FrameData captureFrame() throws IOException {
        synchronized (connectionLock) {
            if (dataInput == null) {
                connectToStream();
            }

            byte[] frameData = readFrameData();
            if (frameData == null) return null;

            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length, bitmapOptions);
                if (bitmap == null) {
                    throw new IOException("Failed to decode frame");
                }
                return new FrameData(frameData, bitmap);
            } catch (Exception e) {
                Log.e(cameraName, "Error decoding frame", e);
                return null;
            } finally {
                if (sharedPreferences.getBoolean("reconnect_mode", false)) {
                    closeConnection();
                }
            }
        }
    }

    private static class FrameData {
        final byte[] data;
        final Bitmap bitmap;

        FrameData(byte[] data, Bitmap bitmap) {
            this.data = data;
            this.bitmap = bitmap;
        }
    }
    private void drawFrame(Bitmap bm, Bitmap overlay) {
        Canvas canvas = null;
        try {
            SurfaceHolder holder = getHolder();
            if (holder == null) return;

            canvas = holder.lockCanvas();
            if (canvas != null) {
                canvas.save();
                if (isZoom) {
                    canvas.rotate(90, bm.getWidth() / 2f, bm.getWidth()  / 2f);
                }
                canvas.drawBitmap(bm, null, canvas.getClipBounds(), null);
                canvas.restore();

                if (overlay != null) {
                    canvas.drawBitmap(overlay,
                            canvas.getWidth() - overlay.getWidth(),
                            canvas.getHeight() - overlay.getHeight(),
                            null);
                }
            }
        } finally {
            if (canvas != null) {
                try {
                    SurfaceHolder holder = getHolder();
                    if (holder != null) {
                        holder.unlockCanvasAndPost(canvas);
                    }
                } catch (IllegalStateException e) {
                    Log.w(cameraName, "Failed to unlock canvas", e);
                }
            }
        }
    }


    private void connectToStream() throws IOException {
        Log.i(cameraName, "Opening connection");
        connection = (HttpURLConnection) streamUrl.openConnection();
        connection.setConnectTimeout((int) CONNECT_TIMEOUT_MS);
        connection.setReadTimeout((int) READ_TIMEOUT_MS);
        connection.connect();
        dataInput = new DataInputStream(connection.getInputStream());
    }

    private byte[] readFrameData() throws IOException {
        readUntilSequence(headerBuffer, dataInput, SOI_MARKER);
        int contentLength = parseContentLength(headerBuffer);
        byte[] frameBuffer = new byte[contentLength];
        dataInput.readFully(frameBuffer, 0, contentLength);
        return frameBuffer;
    }

    private void readUntilSequence(byte[] buffer, InputStream in, byte[] sequence) throws IOException {
        int seqIndex = 0;
        for (int i = 0; i < HEADER_MAX_LENGTH; i++) {
            int value = in.read();
            if (value == -1) throw new IOException("End of stream");

            buffer[i] = (byte) value;
            if (value == sequence[seqIndex]) {
                seqIndex++;
                if (seqIndex == sequence.length) return;
            } else {
                seqIndex = 0;
            }
        }
        throw new IOException("Header too long");
    }

    private void adjustQuality() {
        HttpURLConnection configConnection = null;
        try {
            String configUrl = String.format("http://%s%s/config?fps=12&resx=%d&resy=%d",
                    currentIp, portEndpoint, getXSize(), getYSize());

            configConnection = (HttpURLConnection) new URL(configUrl).openConnection();
            configConnection.setConnectTimeout(100);
            configConnection.setRequestMethod("GET");
            configConnection.setReadTimeout(100);
            configConnection.getResponseCode();
        } catch (IOException e) {
            Log.w(cameraName, "Failed to adjust quality", e);
        } finally {
            if (configConnection != null) {
                configConnection.disconnect();
            }
        }
    }

    private void updateStreamUrl() throws MalformedURLException, InterruptedException {
        currentIp = context.get_ip();
        String urlString = "http://" + currentIp + portEndpoint + "/stream.mjpg";
        streamUrl = new URL(urlString);
    }

    private void closeConnection() {
        synchronized (connectionLock) {
            if (dataInput != null) {
                try {
                    dataInput.close();
                } catch (IOException e) {
                    Log.e(cameraName, "Error closing data input", e);
                }
                dataInput = null;
            }

            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }
    }

    private void setBackgroundColorOnUiThread(final int color) {
        if (cameraFrame != null) {
            post(() -> cameraFrame.setBackgroundColor(color));
        }
    }

    public boolean toggleRecording() {
        return setRecording(!isRecording);
    }

    public boolean setRecording(boolean newState) {
        if (isRecording != newState) {
            isRecording = newState;
            if (newState) {
                recordingHandler.startRecording(cameraName, getXSize(), getYSize());
            } else {
                recordingHandler.stopRecording();
            }
        }

        if (recordingButton != null) {
            post(() -> recordingButton.setSelected(isRecording));
        }
        return isRecording;
    }

    private int getXSize() {
        return isZoom ? maxHeight : SettingsFragment.getXSize(sharedPreferences);
    }

    private int getYSize() {
        return isZoom ? maxWidth : SettingsFragment.getYSize(sharedPreferences);
    }

    private int parseContentLength(byte[] headerBytes) {
        String header = new String(headerBytes);
        String CONTENT_LENGTH = "Content-Length: ";
        int start = header.indexOf(CONTENT_LENGTH) + CONTENT_LENGTH.length();
        int end = header.indexOf('\r', start);
        return Integer.parseInt(header.substring(start, end));
    }

    private synchronized void startTether(){
        int wifi_state = context.getSystemService(WifiManager.class).getWifiApState();
        if (wifi_state != WifiManager.WIFI_AP_STATE_DISABLED) {
            return;
        }
        TetheringManager.StartTetheringCallback callback = new TetheringManager.StartTetheringCallback() {};
        TetheringManager.TetheringRequest request = new TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI).build();
        context.getSystemService(TetheringManager.class).startTethering(request, Runnable::run, callback);
    }
}