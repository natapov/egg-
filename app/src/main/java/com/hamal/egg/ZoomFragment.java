package com.hamal.egg;

import android.content.Context;
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
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = ZoomViewBinding.inflate(inflater, container, false);
        context = (MainActivity) getContext();
        View root = binding.getRoot();
        int cameraNum = getArguments().getInt("cameraNum", 0);
        assert cameraNum != 0;
        camera = context.getCamera(cameraNum);
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
        camera.startPlayback(binding.cameraFrame, true);
    }

    @Override
    public void onPause() {
        binding.camHolder.removeView(camera);
        super.onPause();
    }
}