package com.hamal.egg.ui;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hamal.egg.CamerasModel;
import com.hamal.egg.MainActivity;
import com.hamal.egg.MjpegView;
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
        camera = model.getCamera(0);
        assert(camera != null);
        binding.camHolder.addView(camera);
        return root;
    }
    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) context;
        assert activity != null;
        camera.startPlayback(binding.cameraFrame, true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}