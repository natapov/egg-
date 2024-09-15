package com.hamal.egg.ui.dashboard;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.hamal.egg.MainActivity;
import com.hamal.egg.databinding.CameraViewBinding;

public class DashboardFragment extends Fragment {
    private CameraViewBinding binding;
    SharedPreferences sharedPreferences;
    Context context;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        context = getContext();
        assert context != null;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        binding = CameraViewBinding.inflate(inflater, container, false);
        binding.recordButton1.setOnClickListener(n -> {
            boolean is_recording = binding.cam1.toggleRecording();
            binding.recordButton1.setSelected(is_recording);
            if (sharedPreferences.getBoolean("record_all", true)) {
                binding.recordButton2.setSelected(is_recording);
                binding.cam2.setRecording(is_recording);
                binding.recordButton3.setSelected(is_recording);
                binding.cam3.setRecording(is_recording);
            }
        });
        binding.recordButton2.setOnClickListener(n -> {
            boolean is_recording = binding.cam2.toggleRecording();
            binding.recordButton2.setSelected(is_recording);
            if (sharedPreferences.getBoolean("record_all", true)) {
                binding.recordButton1.setSelected(is_recording);
                binding.cam1.setRecording(is_recording);
                binding.recordButton3.setSelected(is_recording);
                binding.cam3.setRecording(is_recording);

            }
        });
        binding.recordButton3.setOnClickListener(n -> {
            boolean is_recording = binding.cam3.toggleRecording();
            binding.recordButton3.setSelected(is_recording);
            if (sharedPreferences.getBoolean("record_all", true)) {
                binding.recordButton1.setSelected(is_recording);
                binding.cam1.setRecording(is_recording);
                binding.recordButton2.setSelected(is_recording);
                binding.cam2.setRecording(is_recording);
            }
        });
        return binding.getRoot();
    }
    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) context;
        assert activity != null;
        binding.cam1.startPlayback(activity, ":8008/stream.mjpg");
        binding.cam2.startPlayback(activity, ":9800/stream.mjpg");
        binding.cam3.startPlayback(activity, ":9801/stream.mjpg");
    }
    @Override
    public void onDestroyView() {
        binding.cam1.stopPlayback();
        binding.cam2.stopPlayback();
        binding.cam3.stopPlayback();
        binding = null;
        super.onDestroyView();
    }
}