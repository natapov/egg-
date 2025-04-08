package com.hamal.egg;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.hamal.egg.databinding.CameraViewBinding;

public class DashboardFragment extends Fragment {
    private CameraViewBinding binding;
    SharedPreferences sharedPreferences;
    MainActivity context;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = CameraViewBinding.inflate(inflater, container, false);
        context = (MainActivity) requireContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // link the cameras to their buttons, they are in charge of maintaining correct button state
        context.camera1.recordingButton = binding.recordButton1;
        context.camera2.recordingButton = binding.recordButton2;
        context.camera3.recordingButton = binding.recordButton3;

        binding.recordButton1.setOnClickListener(n -> {
            boolean is_recording = context.camera1.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                context.camera2.setRecording(is_recording);
                context.camera3.setRecording(is_recording);
            }
        });
        binding.recordButton2.setOnClickListener(n -> {
            boolean is_recording = context.camera2.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                context.camera1.setRecording(is_recording);
                context.camera3.setRecording(is_recording);
            }
        });
        binding.recordButton3.setOnClickListener(n -> {
            boolean is_recording = context.camera3.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                context.camera1.setRecording(is_recording);
                context.camera2.setRecording(is_recording);
            }
        });
        binding.zoomButton1.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("cameraNum", 1);
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_dashboard_to_zoom, bundle);
        });
        binding.zoomButton2.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("cameraNum", 2);
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_dashboard_to_zoom, bundle);
        });
        binding.zoomButton3.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("cameraNum", 3);
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_dashboard_to_zoom, bundle);
        });
        return binding.getRoot();
    }
    @Override
    public void onStart() {
        super.onStart();
        binding.camHolder1.addView(context.camera1);
        binding.camHolder2.addView(context.camera2);
        binding.camHolder3.addView(context.camera3);
    }

    @Override
    public void onResume() {
        super.onResume();
        context.camera1.startPlayback(binding.frame1,false);
        context.camera2.startPlayback(binding.frame2,false);
        context.camera3.startPlayback(binding.frame3,false);
    }

    @Override
    public void onPause() {
        super.onPause();
        context.camera1.stopPlayback();
        context.camera2.stopPlayback();
        context.camera3.stopPlayback();
    }
    @Override
    public void onStop() {
        super.onStop();
        binding.camHolder1.removeView(context.camera1);
        binding.camHolder2.removeView(context.camera2);
        binding.camHolder3.removeView(context.camera3);
    }
}