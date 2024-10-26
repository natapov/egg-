package com.hamal.egg;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = requireContext();
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference eggIpPref = findPreference("egg_ip");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (eggIpPref != null) {
            MainActivity ip_provider = (MainActivity) context;
            eggIpPref.setSummary(ip_provider.sample_ip());
        }

        Preference recordingFolderPref = findPreference("recording_folder");
        if (recordingFolderPref != null) {
            try {
                recordingFolderPref.setSummary(context.getExternalFilesDir(null).getPath());
            } catch (Exception e) {
                recordingFolderPref.setSummary("N/A");
            }
        }

    }


    public static int getXSize(@NonNull SharedPreferences sharedPreferences) {
        switch (sharedPreferences.getString("quality","Low")) {
            case "Medium":
                return 640;
            case "High":
                return 960;
            default:
                return 320;
        }
    }

    public static int getYSize(@NonNull SharedPreferences sharedPreferences) {
        switch (sharedPreferences.getString("quality","Low")) {
            case "Medium":
                return 360;
            case "High":
                return 540;
            default:
                return 180;
        }
    }
}
