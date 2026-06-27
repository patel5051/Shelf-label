package com.retailshelflabel.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.retailshelflabel.R

/**
 * Loads app preferences from res/xml/preferences.xml.
 *
 * All keys are defined as constants in [PreferencesHelper] and must match
 * the keys in preferences.xml.
 */
class SettingsPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
