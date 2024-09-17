package com.hamal.egg;

import android.content.Context;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = requireContext();
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference eggIpPref = findPreference("egg_ip");
        if (eggIpPref != null) {
            try {
                MainActivity ip_provider = (MainActivity) context;
                eggIpPref.setSummary(ip_provider.get_ip());
            } catch (InterruptedException e) {
                eggIpPref.setSummary("N/A");
            }
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
}
