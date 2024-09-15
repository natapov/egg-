package com.hamal.egg.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import com.google.android.exoplayer2.ExoPlayer;
//import com.google.android.exoplayer2.MediaItem;
//import com.google.android.exoplayer2.ui.PlayerView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.hamal.egg.databinding.FragmentVideoBinding;
import java.io.File;

public class NotificationsFragment extends Fragment { private static final int READ_REQUEST_CODE = 42;

    private FragmentVideoBinding binding;
    Uri current_video;
    Context context;
    private Intent intent;
    String url = "https://ia803409.us.archive.org/18/items/mp4_20210502/mp4.ia.mp4";
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoBinding.inflate(inflater, container, false);
        context = getContext();
        View root = binding.getRoot();
//        File f = context.getExternalFilesDir("my_images");
//        assert f != null;
//        Uri contentUri = FileProvider.getUriForFile(context, "com.hamal.egg.provider", f);
//        intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(contentUri, "*/*");
//        startActivityForResult(intent, READ_REQUEST_CODE);
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






//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
//        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            if (resultData != null) {
//                current_video = resultData.getData();
//                // Handle the selected file URI here
//            }
//        }
//    }
//
}
