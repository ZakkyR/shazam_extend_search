package com.example.shazamextendsearch

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("pref_notification_channel")?.setOnPreferenceClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_SHORTCUT)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
            }
            startActivity(intent)
            true
        }

        findPreference<Preference>("pref_version")?.summary = BuildConfig.VERSION_NAME

        findPreference<Preference>("pref_check_update")?.setOnPreferenceClickListener {
            UpdateChecker.checkAndShowDialog(requireActivity(), BuildConfig.VERSION_NAME, showUpToDate = true)
            true
        }
    }
}
