package com.hamal.egg.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;


import com.hamal.egg.databinding.FragmentVideoBinding;
import java.io.File;
import java.util.ArrayList;

public class PlayerFragment extends Fragment {
    private FragmentVideoBinding binding;
    Context context;
    private LibVLC libVlc;
    private MediaPlayer mediaPlayer;
    private VLCVideoLayout videoLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoBinding.inflate(inflater, container, false);
        context = getContext();
        View root = binding.getRoot();
        videoLayout = binding.videoLayout;
        File externalFilesDir = context.getExternalFilesDir(null);
        assert externalFilesDir != null;
        File[] files = externalFilesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".avi")){
                    break;
                }
            }
        }
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

        Media media = new Media(libVlc, Uri.parse("your_video_url_here"));
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=600");

        mediaPlayer.setMedia(media);
        media.release();
        mediaPlayer.play();
    }

    @Override
    public void onStop() {
        super.onStop();
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