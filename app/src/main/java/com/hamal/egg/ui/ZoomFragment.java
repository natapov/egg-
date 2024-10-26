package com.hamal.egg.ui;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.hamal.egg.MainActivity;
import com.hamal.egg.R;
import com.hamal.egg.databinding.ZoomViewBinding;


public class ZoomFragment extends Fragment {
    private ZoomViewBinding binding;
    Context context;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = ZoomViewBinding.inflate(inflater, container, false);
        context = getContext();
        View root = binding.getRoot();

        return root;
    }
    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) context;
        assert activity != null;
        Resources res = getResources();
        String parameter = getArguments().getString("parameterName");

        binding.cam1.startPlayback(activity, ":8008", binding.cameraFrame,res.getDimensionPixelOffset(R.dimen.zoom_cam_width), res.getDimensionPixelOffset(R.dimen.zoom_cam_height), true);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.cam1.stopPlayback();
    }

}