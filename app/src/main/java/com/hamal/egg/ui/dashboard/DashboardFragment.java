package com.hamal.egg.ui.dashboard;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.hamal.egg.databinding.CameraViewBinding;

public class DashboardFragment extends Fragment {
    private CameraViewBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = CameraViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onResume() {
        super.onResume();
        binding.recordButton1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { binding.cam1.startRecording();} else { binding.cam1.stopRecording(); }
        });
        binding.recordButton2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { binding.cam2.startRecording();} else { binding.cam2.stopRecording(); }
        });
        binding.recordButton3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { binding.cam3.startRecording();} else { binding.cam3.stopRecording(); }
        });
        binding.cam1.startPlayback("http://192.168.192.220:8008/stream.mjpg");
        binding.cam2.startPlayback("http://192.168.192.220:8008/stream.mjpg");
        binding.cam3.startPlayback("http://192.168.192.220:8008/stream.mjpg");
    }
    @Override
    public void onDestroyView() {
        binding.cam1.stopPlayback();
        binding.cam2.stopPlayback();
        binding.cam3.stopPlayback();
        super.onDestroyView();
        binding = null;
    }
}