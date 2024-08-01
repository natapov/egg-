package com.hamal.egg.ui.dashboard;
import com.hamal.egg.MjpegView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.hamal.egg.databinding.CameraViewBinding;
import com.hamal.egg.databinding.FragmentDashboardBinding;
import com.hamal.egg.databinding.FragmentHomeBinding;

public class DashboardFragment extends Fragment {

    private CameraViewBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = CameraViewBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}