package com.hamal.egg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.hamal.egg.databinding.ZoomViewBinding;


public class ZoomFragment extends Fragment {
    private ZoomViewBinding binding;
    MainActivity context;
    MjpegView camera;
    MjpegView mini_camera_1;
    MjpegView mini_camera_2;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = ZoomViewBinding.inflate(inflater, container, false);
        context = (MainActivity) getContext();
        View root = binding.getRoot();
        int cameraNum = getArguments().getInt("cameraNum", 0);
        assert cameraNum != 0;

        camera = context.getCamera(cameraNum);
        int mini_1 = (cameraNum) % 3 + 1;
        int mini_2 = (cameraNum + 1) % 3 + 1;
        mini_camera_1 = context.getCamera(mini_1);
        mini_camera_2 = context.getCamera(mini_2);
        assert(camera != null);
        camera.recordingButton = binding.recordButton;

        binding.zoomButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_back_to_dashboard);
        });
        binding.recordButton.setOnClickListener(n -> {
            camera.toggleRecording();
        });

        return root;
    }
    @Override
    public void onResume() {
        super.onResume();
        binding.camHolder.addView(camera);
        binding.miniCamHolder1.addView(mini_camera_1);
        binding.miniCamHolder2.addView(mini_camera_2);
        camera.startPlayback(binding.cameraFrame, true);
        mini_camera_1.startPlayback(binding.miniFrame1, true);
        mini_camera_2.startPlayback(binding.miniFrame2, true);
    }

    @Override
    public void onPause() {
        binding.camHolder.removeView(camera);
        binding.miniCamHolder1.removeView(mini_camera_1);
        binding.miniCamHolder2.removeView(mini_camera_2);
        super.onPause();
    }
}