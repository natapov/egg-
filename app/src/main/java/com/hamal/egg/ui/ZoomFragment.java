package com.hamal.egg.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.hamal.egg.CamerasModel;
import com.hamal.egg.MjpegView;
import com.hamal.egg.R;
import com.hamal.egg.databinding.ZoomViewBinding;


public class ZoomFragment extends Fragment {
    private ZoomViewBinding binding;
    Context context;
    MjpegView camera;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = ZoomViewBinding.inflate(inflater, container, false);
        context = getContext();
        View root = binding.getRoot();
        CamerasModel model = new ViewModelProvider(requireActivity()).get(CamerasModel.class);
        int cameraNum = getArguments().getInt("cameraNum", 0);
        assert cameraNum != 0;
        camera = model.getCamera(cameraNum);
        assert(camera != null);
        camera.recording_button = binding.recordButton;

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