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
import com.hamal.egg.databinding.ZoomViewBinding;


public class ZoomFragment extends Fragment {
    private ZoomViewBinding binding;
    Context context;
    CamerasModel model;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = ZoomViewBinding.inflate(inflater, container, false);
        context = getContext();
        View root = binding.getRoot();
        model = new ViewModelProvider(requireActivity()).get(CamerasModel.class);
        assert(model.camera1 != null);
        binding.camHolder.addView(model.camera1);
        return root;
    }
    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) context;
        assert activity != null;
        Resources res = getResources();
        assert getArguments() != null;
        int cameraNum = getArguments().getInt("cameraNum");

        model.camera1.startPlayback(binding.cameraFrame, true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}