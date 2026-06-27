package com.retailshelflabel.ui.history

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentPrintHistoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Print History screen.
 *
 * Displays the last 100 completed print jobs newest-first, mixing single-item
 * prints and bulk print jobs in one chronological list.
 *
 * A search bar at the top filters rows by description or barcode in real time.
 * A date-filter chip opens a date-range picker so staff can narrow results to
 * a specific day or week; an active filter shows the selected range and a
 * clear button.
 *
 * Single-item rows have a Reprint button that re-sends the label immediately.
 * Bulk job rows have a "Reprint this job" button that navigates to the
 * bulk-print confirmation screen pre-populated with the saved item snapshots,
 * so staff can replay an entire job with one tap.
 */
class PrintHistoryFragment : Fragment() {

    private var _binding: FragmentPrintHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PrintHistoryViewModel by viewModels()
    private lateinit var adapter: PrintHistoryAdapter

    private val chipDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrintHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PrintHistoryAdapter(
            onReprint = { job -> viewModel.reprint(job) },
            onReprintBulkJob = { jobWithItems ->
                findNavController().navigate(
                    R.id.action_history_to_bulk_print,
                    bundleOf(
                        "bulkJobId" to jobWithItems.bulkJob.bulkJobId,
                        "selectedItemIds" to LongArray(0)
                    )
                )
            }
        )

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
            this.adapter = this@PrintHistoryFragment.adapter
        }

        setupSearch()
        setupDateFilter()
        setupClearHistory()
        setupExportCsv()

        viewModel.historyEntries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            binding.tvEmptyHistory.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            binding.rvHistory.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.reprintResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is PrintHistoryViewModel.ReprintResult.Success -> {
                    viewModel.clearResult()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.print_success, result.copies),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                is PrintHistoryViewModel.ReprintResult.Error -> {
                    viewModel.clearResult()
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
                null -> {}
            }
        }
    }

    private fun setupExportCsv() {
        binding.btnExportCsv.setOnClickListener {
            viewModel.exportCsv()
        }

        viewModel.csvExportState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PrintHistoryViewModel.CsvExportState.Idle -> {
                    /* resting state — nothing to do */
                }
                is PrintHistoryViewModel.CsvExportState.NoData -> {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.history_export_no_data),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    viewModel.clearCsvExportState()
                }
                is PrintHistoryViewModel.CsvExportState.Ready -> {
                    val fileName = "print_history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
                    val exportsDir = File(requireContext().cacheDir, "exports").also { it.mkdirs() }
                    val file = File(exportsDir, fileName)
                    file.writeText(state.csv, Charsets.UTF_8)

                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.history_export_share_title)))
                    viewModel.clearCsvExportState()
                }
            }
        }
    }

    private fun setupClearHistory() {
        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.history_clear_confirm_title))
                .setMessage(getString(R.string.history_clear_confirm_message))
                .setPositiveButton(getString(R.string.history_clear_confirm_button)) { _, _ ->
                    viewModel.clearHistory()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.history_cleared),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun setupDateFilter() {
        binding.chipDateFilter.setOnClickListener {
            val picker = MaterialDatePicker.Builder
                .dateRangePicker()
                .setTitleText(getString(R.string.history_date_picker_title))
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val startUtc = selection.first ?: return@addOnPositiveButtonClickListener
                val endUtc = selection.second ?: return@addOnPositiveButtonClickListener

                val startMs = startOfDayLocal(startUtc)
                val endMs = endOfDayLocal(endUtc)

                viewModel.setDateRange(startMs, endMs)
                updateDateFilterChip(startMs, endMs)
            }

            picker.show(parentFragmentManager, "date_range_picker")
        }

        binding.chipDateClear.setOnClickListener {
            viewModel.clearDateRange()
            resetDateFilterChip()
        }

        viewModel.dateRangeStart.observe(viewLifecycleOwner) { start ->
            val end = viewModel.dateRangeEnd.value
            if (start != null && end != null) {
                updateDateFilterChip(start, end)
            } else {
                resetDateFilterChip()
            }
        }
    }

    private fun updateDateFilterChip(startMs: Long, endMs: Long) {
        val startLabel = chipDateFormat.format(Date(startMs))
        val endLabel = chipDateFormat.format(Date(endMs))
        val label = if (startLabel == endLabel) startLabel else "$startLabel – $endLabel"
        binding.chipDateFilter.text = label
        binding.chipDateFilter.isChecked = true
        binding.chipDateClear.visibility = View.VISIBLE
    }

    private fun resetDateFilterChip() {
        binding.chipDateFilter.text = getString(R.string.history_filter_by_date)
        binding.chipDateFilter.isChecked = false
        binding.chipDateClear.visibility = View.GONE
    }

    /**
     * Converts a UTC midnight timestamp (as returned by MaterialDatePicker)
     * to the start of that calendar day in the device's local time zone.
     *
     * MaterialDatePicker always returns UTC midnight for the selected date.
     * We first extract the date components (year/month/day) in UTC, then
     * construct a local-timezone Calendar from those same components so we
     * never accidentally shift to a different day in negative-offset zones.
     */
    private fun startOfDayLocal(utcMs: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMs
        }
        return Calendar.getInstance(TimeZone.getDefault()).apply {
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Converts a UTC midnight timestamp to the last millisecond of that
     * calendar day in the device's local time zone, so the filter is
     * inclusive of all records printed on the end day.
     *
     * Uses the same UTC-date-extraction approach as [startOfDayLocal] to
     * avoid off-by-one day errors in negative UTC-offset time zones.
     */
    private fun endOfDayLocal(utcMs: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMs
        }
        return Calendar.getInstance(TimeZone.getDefault()).apply {
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                23, 59, 59
            )
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
