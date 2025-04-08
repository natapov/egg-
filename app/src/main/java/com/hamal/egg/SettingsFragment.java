package com.hamal.egg;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        MainActivity context = (MainActivity) requireContext();
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference eggIpPref = findPreference("egg_ip");
        if (eggIpPref != null) {
            eggIpPref.setSummary(context.sample_ip());
        }

        Preference recordingFolderPref = findPreference("recording_folder");
        if (recordingFolderPref != null) {
            try {
                recordingFolderPref.setSummary(context.getExternalFilesDir(null).getPath());
            } catch (Exception e) {
                recordingFolderPref.setSummary("N/A");
            }
        }

        Preference shutDownApp = findPreference("shut_down_app");
        if (shutDownApp != null) {
            shutDownApp.setOnPreferenceClickListener(preference -> {
                // Force kill the app process
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
            });
        }

        Preference restartAppPref = findPreference("restart_app");
        if (restartAppPref != null) {
            restartAppPref.setOnPreferenceClickListener(preference -> {
                // Get the package manager to restart the launcher
                Intent intent = requireContext().getPackageManager()
                    .getLaunchIntentForPackage(requireContext().getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                
                // Create a pending intent to restart the launcher
                Intent restartIntent = Intent.makeRestartActivityTask(intent.getComponent());
                
                // Schedule the restart
                requireContext().startActivity(restartIntent);
                
                // Kill the current process
                System.exit(0);
                return true;
            });
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
