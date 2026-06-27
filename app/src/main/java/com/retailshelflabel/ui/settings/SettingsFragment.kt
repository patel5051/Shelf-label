package com.retailshelflabel.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.retailshelflabel.R
import com.retailshelflabel.sdk.PrinterManager
import com.retailshelflabel.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import com.retailshelflabel.util.PreferencesHelper

/**
 * Settings screen backed by SharedPreferences (via PreferenceManager defaults).
 *
 * Hosts a [SettingsPreferenceFragment] (PreferenceFragmentCompat) inside a
 * simple container layout so we can keep the Navigation Component toolbar.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsPreferenceFragment())
                .commit()
        }

        binding.btnTestPrint.setOnClickListener {
            val printer = PrinterManager(requireContext())
            printer.initPrinter(
                onReady = {
                    printer.printTestLabel { success, message ->
                        val msg = if (success)
                            getString(R.string.test_print_success)
                        else
                            message ?: getString(R.string.print_error)
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                    }
                },
                onError = { err ->
                    Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
