package com.hamal.egg.ui.notifications;

import static android.app.Activity.RESULT_OK;

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
    Uri current_video;
    Context context;
    private Intent intent;
    ExoPlayer player;
    String url = "https://ia803409.us.archive.org/18/items/mp4_20210502/mp4.ia.mp4";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoBinding.inflate(inflater, container, false);
        context = getContext();
        View root = binding.getRoot();
        PlayerView player_view = binding.playerView;
        player = new ExoPlayer.Builder(context).build();
        player_view.setPlayer(player);


        // create intent
        // Create the intent
        // Get the path to the app's external files directory
        File externalFilesDir = context.getExternalFilesDir(null);
        Uri initialUri = Uri.parse(externalFilesDir.getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");  // Set MIME type for AVI files
        // Set the initial directory
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        // Start the activity for result
        startActivityForResult(intent, READ_REQUEST_CODE);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedFileUri = data.getData();
                MediaItem firstItem = MediaItem.fromUri(selectedFileUri);
                player.addMediaItem(firstItem);
            }
        }
    }
}