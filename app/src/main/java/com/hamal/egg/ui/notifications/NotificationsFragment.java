package com.hamal.egg.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.hamal.egg.databinding.FragmentVideoBinding;
import java.io.File;

public class NotificationsFragment extends Fragment {
    private static final int READ_REQUEST_CODE = 42;

    private FragmentVideoBinding binding;
    Context context;
    private Intent intent;
    ExoPlayer player;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoBinding.inflate(inflater, container, false);
        context = getContext();
        View root = binding.getRoot();
        PlayerView player_view = binding.playerView;
        player = new ExoPlayer.Builder(context).build();
        player_view.setPlayer(player);


        File externalFilesDir = context.getExternalFilesDir(null);
        assert externalFilesDir != null;
        File[] files = externalFilesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".avi")){
                    MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(file));
                    player.addMediaItem(mediaItem);
                    break;
                }
            }
        }
        player.prepare();
        player.play();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}