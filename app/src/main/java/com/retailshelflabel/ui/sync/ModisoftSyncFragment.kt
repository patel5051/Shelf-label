package com.retailshelflabel.ui.sync

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentModisoftSyncBinding
import com.retailshelflabel.sync.ModisoftSyncManager
import com.retailshelflabel.util.PreferencesHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modisoft Pricebook Sync screen.
 *
 * Lets the user choose a sync mode (API / CSV / Manual), trigger an
 * immediate sync, and review the history of past sync attempts.
 */
class ModisoftSyncFragment : Fragment() {

    private var _binding: FragmentModisoftSyncBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ModisoftSyncViewModel by viewModels()
    private lateinit var logAdapter: SyncLogAdapter
    private var selectedCsvUri: Uri? = null

    private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

    // CSV file picker launcher
    private val csvPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCsvUri = uri
            binding.tvCsvFilename.text = getFileName(uri) ?: uri.lastPathSegment ?: "selected file"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModisoftSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logAdapter = SyncLogAdapter()
        binding.rvSyncLog.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logAdapter
            isNestedScrollingEnabled = false
        }

        // ── Mode selector ──────────────────────────────────────────────────
        val savedMode = PreferencesHelper.getModisoftSyncMode(requireContext())
        when (savedMode) {
            ModisoftSyncManager.MODE_CSV    -> binding.rbModeCsv.isChecked = true
            ModisoftSyncManager.MODE_MANUAL -> binding.rbModeManual.isChecked = true
            else                            -> binding.rbModeApi.isChecked = true
        }
        updateModeCards(savedMode)

        binding.rgSyncMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_mode_csv    -> ModisoftSyncManager.MODE_CSV
                R.id.rb_mode_manual -> ModisoftSyncManager.MODE_MANUAL
                else                -> ModisoftSyncManager.MODE_API
            }
            PreferencesHelper.setModisoftSyncMode(requireContext(), mode)
            updateModeCards(mode)
        }

        binding.btnOpenApiSettings.setOnClickListener {
            findNavController().navigate(R.id.action_sync_to_settings)
        }

        binding.btnPickCsv.setOnClickListener {
            csvPickerLauncher.launch("text/*")
        }

        // ── Last sync timestamp ────────────────────────────────────────────
        updateLastSyncLabel()

        // ── Sync Now ──────────────────────────────────────────────────────
        binding.btnSyncNow.setOnClickListener {
            val mode = selectedMode()
            if (mode == ModisoftSyncManager.MODE_CSV && selectedCsvUri == null) {
                Snackbar.make(binding.root, R.string.no_file_selected_error, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.syncNow(mode, selectedCsvUri)
        }

        // ── State observer ─────────────────────────────────────────────────
        viewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ModisoftSyncViewModel.SyncState.Running -> {
                    binding.progressSync.visibility = View.VISIBLE
                    binding.btnSyncNow.isEnabled = false
                    binding.btnSyncNow.text = getString(R.string.syncing)
                }
                is ModisoftSyncViewModel.SyncState.Done -> {
                    binding.progressSync.visibility = View.GONE
                    binding.btnSyncNow.isEnabled = true
                    binding.btnSyncNow.text = getString(R.string.sync_now)
                    updateLastSyncLabel()
                    Snackbar.make(
                        binding.root,
                        state.message,
                        if (state.success) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.resetState()
                }
                is ModisoftSyncViewModel.SyncState.Error -> {
                    binding.progressSync.visibility = View.GONE
                    binding.btnSyncNow.isEnabled = true
                    binding.btnSyncNow.text = getString(R.string.sync_now)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.resetState()
                }
                else -> { /* Idle — nothing to do */ }
            }
        }

        // ── Sync log ───────────────────────────────────────────────────────
        viewModel.recentLogs.observe(viewLifecycleOwner) { logs ->
            logAdapter.submitList(logs)
            binding.tvLogEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            binding.rvSyncLog.visibility = if (logs.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun selectedMode(): String = when (binding.rgSyncMode.checkedRadioButtonId) {
        R.id.rb_mode_csv    -> ModisoftSyncManager.MODE_CSV
        R.id.rb_mode_manual -> ModisoftSyncManager.MODE_MANUAL
        else                -> ModisoftSyncManager.MODE_API
    }

    private fun updateModeCards(mode: String) {
        binding.cardApiInfo.visibility    = if (mode == ModisoftSyncManager.MODE_API)    View.VISIBLE else View.GONE
        binding.cardCsvPicker.visibility  = if (mode == ModisoftSyncManager.MODE_CSV)    View.VISIBLE else View.GONE
        binding.cardManualInfo.visibility = if (mode == ModisoftSyncManager.MODE_MANUAL) View.VISIBLE else View.GONE
    }

    private fun updateLastSyncLabel() {
        val ts = PreferencesHelper.getLastSyncTimestamp(requireContext())
        binding.tvLastSync.text = if (ts == 0L)
            getString(R.string.last_sync_never)
        else
            getString(R.string.last_sync_at, dateFormat.format(Date(ts)))
    }

    private fun getFileName(uri: Uri): String? {
        return requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
