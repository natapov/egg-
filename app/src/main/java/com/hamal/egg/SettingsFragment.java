package com.hamal.egg;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference dynamicValuePref = findPreference("egg_ip");
        MainActivity ip_provider = (MainActivity) getContext();
        if (dynamicValuePref != null) {
            try {
                dynamicValuePref.setSummary(ip_provider.get_ip());
            } catch (InterruptedException e) {
                dynamicValuePref.setSummary("N/A");
            }
        }
    }
}
