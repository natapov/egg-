package com.hamal.egg.ui.dashboard;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.hamal.egg.databinding.CameraViewBinding;

public class DashboardFragment extends Fragment {
    private CameraViewBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = CameraViewBinding.inflate(inflater, container, false);
        binding.recordButton1.setOnClickListener(n -> {
            binding.recordButton1.setSelected(binding.cam1.toggleRecording());
        });
        binding.recordButton2.setOnClickListener(n -> {
            binding.recordButton2.setSelected(binding.cam2.toggleRecording());
        });
        binding.recordButton3.setOnClickListener(n -> {
            binding.recordButton3.setSelected(binding.cam3.toggleRecording());
        });
        return binding.getRoot();
    }
    @Override
    public void onResume() {
        super.onResume();

        binding.cam1.startPlayback("http://192.168.192.220:8008/stream.mjpg");
        binding.cam2.startPlayback("http://192.168.192.220:9800/stream.mjpg");
        binding.cam3.startPlayback("http://192.168.192.220:9801/stream.mjpg");
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