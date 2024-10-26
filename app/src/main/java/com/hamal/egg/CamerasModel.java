package com.hamal.egg;

import android.content.Context;

import androidx.lifecycle.ViewModel;

public class CamerasModel extends ViewModel {
    public MjpegView camera1;
    public MjpegView camera2;
    public MjpegView camera3;

    // Create cameras if they don't exist
    public void initializeCameras(Context context) {
        if (camera1 == null) {
            camera1 = new MjpegView(context, null);
        }
        if (camera2 == null) {
            camera2 = new MjpegView(context, null);
        }
        if (camera3 == null) {
            camera3 = new MjpegView(context, null);
        }
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        // Cleanup if needed
        if (camera1 != null) {
            camera1.stopPlayback();
        }
        if (camera2 != null) {
            camera2.stopPlayback();
        }
        if (camera3 != null) {
            camera3.stopPlayback();
        }
    }
}