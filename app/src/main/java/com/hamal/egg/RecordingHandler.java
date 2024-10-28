package com.hamal.egg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordingHandler {
    static final String TAG = "MjpegRecordingHandler";
    final Context context;
    final SharedPreferences sharedPreferences;

    public RecordingHandler(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    MJPEGGenerator m = null;
    public void startRecording(String cam_name, int x_size, int y_size) {
        try {
            File mjpegFilePath = createSavingFile(cam_name, "avi");
            m = new MJPEGGenerator(mjpegFilePath, x_size, y_size, 12.0);
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

    private File createSavingFile(String postfix, String extension) {
        Date date = new Date();

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yy_HH:mm:ss");
        String szFileName = sdf.format(date) + "_" + postfix + "." + extension;
        try {
            File file = new File(context.getExternalFilesDir(null).getPath(), szFileName);
            if (file.exists())
                file.delete();
            file.createNewFile();
            Log.d(TAG, "file path is " + file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }
    public void capture_frame(byte[] jpeg) {
        try {
            m.addImage(jpeg);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}

