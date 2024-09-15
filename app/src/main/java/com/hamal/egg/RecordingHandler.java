package com.hamal.egg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordingHandler {
    private static final String TAG = "MjpegRecordingHandler";
    private final Context context;
    public RecordingHandler(Context context) {
        this.context = context;
    }
    MJPEGGenerator m = null;
    public void startRecording() {
        try {
            File mjpegFilePath = createSavingFile("video", "avi");
            m = new MJPEGGenerator(mjpegFilePath, 320, 180, 12.0, 0);
            Toast.makeText(context, "start recording, file path is:" + mjpegFilePath, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
    public void stopRecording() {
        try {
            if (m != null) {
                m.finishAVI();
                m = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private File createSavingFile(String prefix, String extension) {
        Date date = new Date();

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yy_HH:mm:ss");
        String szFileName = prefix + "-" + sdf.format(date);
        try {
            String path = context.getExternalFilesDir(null).getPath() + "/" + szFileName + "." + extension;
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            Log.d(TAG, "file path is " + file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }
    public void capture_frame(byte[] bitmap) {
        try {
            m.addImage(bitmap);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}

