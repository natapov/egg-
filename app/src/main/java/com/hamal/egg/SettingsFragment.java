package com.hamal.egg;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = requireContext();
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference eggIpPref = findPreference("egg_ip");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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
            case "Low":
                return 320;
            case "Medium":
                return 640;
            default:
                return 320;
        }
    }

    public static int getYSize(@NonNull SharedPreferences sharedPreferences) {
        switch (sharedPreferences.getString("quality","Low")) {
            case "Low":
                return 180;
            case "Medium":
                return 360;
            default:
                return 180;
        }
    }
}
