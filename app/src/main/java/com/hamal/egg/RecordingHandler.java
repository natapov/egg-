package com.hamal.egg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordingHandler {
    private static final String TAG = "MjpegRecordingHandler";

    private final Context context;
    private DataOutputStream dos;
    private Bitmap lastBitmap;

    public RecordingHandler(Context context) {
        this.context = context;
    }

    /**
     * Start recording the live image.
     */
    public void startRecording() {
        try {
            String mjpegFilePath = createMjpegFile().getAbsolutePath();
            FileOutputStream fos = new FileOutputStream(mjpegFilePath);
            dos = new DataOutputStream(fos);
            Toast.makeText(context, "start recording, file path is:" + mjpegFilePath, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Stop recording the live image.
     */
    public void stopRecording() throws IOException {
        dos.flush();
        dos.close();
    }

    /**
     * Save the last acquired bitmap into a JPEG file.
     */
//    public void saveBitmapToFile() {
//        FileOutputStream fos;
//        BufferedOutputStream bos;
//        String imagePath = createJpgFile().getAbsolutePath();
//        try {
//            fos = new FileOutputStream(imagePath);
//            bos = new BufferedOutputStream(fos);
//            ByteArrayOutputStream jpegByteArrayOutputStream = new ByteArrayOutputStream();
//            lastBitmap.compress(Bitmap.CompressFormat.JPEG, 75, jpegByteArrayOutputStream);
//            byte[] jpegByteArray = jpegByteArrayOutputStream.toByteArray();
//            bos.write(jpegByteArray);
//            bos.flush();
//            Toast.makeText(context, "saved image:" + imagePath, Toast.LENGTH_LONG).show();
//        } catch (IOException e) {
//            Log.e(TAG, e.getMessage());
//        }
//    }

    /**
     * Create a JPEG file in the app's external cache directory.
     *
     * @return File
     */
    private File createJpgFile() {
        return createSavingFile("photo", "jpg");
    }

    private File createSavingFile(String prefix, String extension) {
        Date date = new Date();

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
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

    /**
     * Create an MJPEG file in the app's external cache directory.
     *
     * @return File
     */
    private File createMjpegFile() {
        return createSavingFile("video", "mjpeg");
    }

    public void capture_frame(byte[] bitmap, int bitmap_size, byte[] header, int header_size) {
        try {
            dos.write(header,0, header_size);
            dos.write(bitmap,0, bitmap_size);
            dos.flush();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}

