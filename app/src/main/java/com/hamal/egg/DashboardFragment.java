package com.hamal.egg;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.hamal.egg.CamerasModel;
import com.hamal.egg.MainActivity;
import com.hamal.egg.R;
import com.hamal.egg.databinding.CameraViewBinding;

public class DashboardFragment extends Fragment {
    private CameraViewBinding binding;
    SharedPreferences sharedPreferences;
    Context context;
    CamerasModel model;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = CameraViewBinding.inflate(inflater, container, false);
        context =  requireContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        model = new ViewModelProvider(requireActivity()).get(CamerasModel.class);
        model.initializeCameras(context);

        // link the cameras to their buttons, they are in charge of maintaining correct button state
        model.camera1.recording_button = binding.recordButton1;
        model.camera2.recording_button = binding.recordButton2;
        model.camera3.recording_button = binding.recordButton3;

        binding.recordButton1.setOnClickListener(n -> {
            boolean is_recording = model.camera1.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                model.camera2.setRecording(is_recording);
                model.camera3.setRecording(is_recording);
            }
        });
        binding.recordButton2.setOnClickListener(n -> {
            boolean is_recording = model.camera2.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                model.camera1.setRecording(is_recording);
                model.camera3.setRecording(is_recording);
            }
        });
        binding.recordButton3.setOnClickListener(n -> {
            boolean is_recording = model.camera3.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                model.camera1.setRecording(is_recording);
                model.camera2.setRecording(is_recording);
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
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) context;
        assert activity != null;
        Resources res = getResources();
        binding.camHolder1.addView(model.camera1);
        binding.camHolder2.addView(model.camera2);
        binding.camHolder3.addView(model.camera3);
        model.camera1.startPlayback(binding.frame1,false);
        model.camera2.startPlayback(binding.frame2,false);
        model.camera3.startPlayback(binding.frame3,false);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.camHolder1.removeView(model.camera1);
        binding.camHolder2.removeView(model.camera2);
        binding.camHolder3.removeView(model.camera3);
    }
}