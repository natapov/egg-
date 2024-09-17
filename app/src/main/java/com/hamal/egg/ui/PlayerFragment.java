package com.hamal.egg.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.hamal.egg.R;
import com.hamal.egg.databinding.FragmentVideoBinding;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerFragment extends Fragment {
    private FragmentVideoBinding binding;
    Context context;
    private LibVLC libVlc;
    private MediaPlayer mediaPlayer;
    private VLCVideoLayout videoLayout;
    File selectedFile = null;
    private Handler handler = new Handler();
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> fileList;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoBinding.inflate(inflater, container, false);
        context = getContext();
        View root = binding.getRoot();
        videoLayout = binding.videoLayout;
        listView = binding.listView;
        File externalFilesDir = context.getExternalFilesDir(null);
        assert externalFilesDir != null;
        File[] files = externalFilesDir.listFiles();
        fileList = new ArrayList<String>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".avi")){
                    fileList.add(file.getName());
                }
            }
        }
        // Create and set the adapter
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_checked, fileList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Set item click listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFile = fileList.get(position);
            boolean isChecked = listView.isItemChecked(position);
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayList<String> options = new ArrayList<>();
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        options.add("--rtsp-tcp");

        libVlc = new LibVLC(requireContext(), options);
        mediaPlayer = new MediaPlayer(libVlc);
        mediaPlayer.attachViews(videoLayout, null, false, false);

        setupControls();

        if (selectedFile != null) {
            Media media = new Media(libVlc, Uri.fromFile(selectedFile));
            media.setHWDecoderEnabled(true, false);

            mediaPlayer.setMedia(media);
            media.release();
            mediaPlayer.play();
            startProgressUpdate();
        }
    }

    private void setupControls() {
        binding.playPauseButton.setOnClickListener(v -> togglePlayPause());

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.setTime(progress);
                    updateCurrentTime();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mediaPlayer.setEventListener(event -> {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                    updatePlayPauseButton();
                    break;
                case MediaPlayer.Event.EndReached:
                    mediaPlayer.setTime(0);
                    mediaPlayer.stop();
                    updatePlayPauseButton();
                    break;
            }
        });
    }

    private void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.play();
        }
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {
        binding.playPauseButton.setImageResource(mediaPlayer.isPlaying() ?
                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void startProgressUpdate() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    long currentTime = mediaPlayer.getTime();
                    long totalTime = mediaPlayer.getLength();
                    updateTimeTexts(currentTime, totalTime);
                    binding.seekBar.setMax((int) totalTime);
                    binding.seekBar.setProgress((int) currentTime);
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void updateTimeTexts(long currentTime, long totalTime) {
        binding.currentTimeText.setText(formatTime(currentTime));
        binding.totalTimeText.setText(formatTime(totalTime));
    }

    private void updateCurrentTime() {
        long currentTime = mediaPlayer.getTime();
        binding.currentTimeText.setText(formatTime(currentTime));
    }

    private String formatTime(long timeMs) {
        int totalSeconds = (int) (timeMs / 1000);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
        mediaPlayer.stop();
        mediaPlayer.detachViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        libVlc.release();
    }
}