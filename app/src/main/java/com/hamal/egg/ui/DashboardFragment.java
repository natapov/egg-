package com.hamal.egg.ui;
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
        binding = CameraViewBinding.inflate(inflater, container, false);
        context = requireContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // link the cameras to their buttons, they are in charge of maintaining correct button state
        binding.cam1.recording_button = binding.recordButton1;
        binding.cam2.recording_button = binding.recordButton2;
        binding.cam3.recording_button = binding.recordButton3;

        binding.recordButton1.setOnClickListener(n -> {
            boolean is_recording = binding.cam1.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                binding.cam2.setRecording(is_recording);
                binding.cam3.setRecording(is_recording);
            }
        });
        binding.recordButton2.setOnClickListener(n -> {
            boolean is_recording = binding.cam2.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                binding.cam1.setRecording(is_recording);
                binding.cam3.setRecording(is_recording);
            }
        });
        binding.recordButton3.setOnClickListener(n -> {
            boolean is_recording = binding.cam3.toggleRecording();
            if (sharedPreferences.getBoolean("record_all", true)) {
                binding.cam1.setRecording(is_recording);
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
        binding.cam1.startPlayback(activity, ":8008");
        binding.cam2.startPlayback(activity, ":9800");
        binding.cam3.startPlayback(activity, ":9801");
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.cam1.stopPlayback();
        binding.cam2.stopPlayback();
        binding.cam3.stopPlayback();
    }

}