package com.hamal.egg;

import android.content.Context;
import android.content.res.Resources;

import androidx.lifecycle.ViewModel;

public class CamerasModel extends ViewModel {
    public MjpegView camera1;
    public MjpegView camera2;
    public MjpegView camera3;

    // Create cameras if they don't exist
    public void initializeCameras(Context context) {
        Resources res = context.getResources();
        if (camera1 == null) {
            camera1 = new MjpegView(context, "cam_1", (MainActivity) context, ":8008",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
        if (camera2 == null) {
            camera2 = new MjpegView(context, "cam_2", (MainActivity) context, ":9800",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
        if (camera3 == null) {
            camera3 = new MjpegView(context, "cam_3", (MainActivity) context, ":9801",
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_width),
                    res.getDimensionPixelOffset(R.dimen.zoom_cam_height));
        }
    }

    public MjpegView getCamera(int n) {
        switch (n) {
            case 2:
                return camera2;
            case 3:
                return camera3;
            default:
                return camera1;
        }
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        if (camera1 != null) {
            camera1.cleanup();
            camera1 = null;
        }
        if (camera2 != null) {
            camera2.cleanup();
            camera2 = null;
        }
        if (camera3 != null) {
            camera3.cleanup();
            camera3 = null;
        }
    }
}