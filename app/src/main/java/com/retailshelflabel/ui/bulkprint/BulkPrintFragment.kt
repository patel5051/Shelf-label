package com.retailshelflabel.ui.bulkprint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.retailshelflabel.R
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.BulkJobItem
import com.retailshelflabel.data.repository.PrintJobRepository
import com.retailshelflabel.databinding.FragmentBulkPrintBinding
import com.retailshelflabel.sdk.PrinterManager
import com.retailshelflabel.util.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bulk-print confirmation screen.
 *
 * Shows all items the user selected on the search screen. Staff can:
 *  - Set a global copy count that applies to all items at once (+/- buttons).
 *  - Override the copy count per-item using row-level +/- buttons.
 *  - Tap "Print All" to send every label to the printer sequentially.
 *
 * This screen also handles the "Reprint this job" entry point from the Print
 * History screen. When launched with a [bulkJobId] argument the ViewModel
 * loads the saved item snapshots instead of querying the live catalogue, so
 * the screen is pre-populated identically to the original job.
 *
 * On successful completion the entire job (all items and per-item copy counts)
 * is saved to the local database so staff can replay it from history later.
 *
 * A printer-status warning banner is shown automatically on entry whenever the
 * printer is not in its normal ready state (out of paper, cover open, etc.).
 * The "Print All" button is disabled until the banner is dismissed and the
 * status re-check confirms the printer is ready.
 */
class BulkPrintFragment : Fragment() {

    private var _binding: FragmentBulkPrintBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BulkPrintViewModel by viewModels()
    private lateinit var bulkAdapter: BulkPrintAdapter
    private lateinit var printerManager: PrinterManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBulkPrintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val printJobRepository = PrintJobRepository(
            (requireActivity().application as ShelfLabelApplication).database.printJobDao()
        )
        printerManager = PrinterManager(requireContext(), printJobRepository)
        printerManager.initPrinter()

        val bulkJobId = arguments?.getLong("bulkJobId", -1L) ?: -1L
        if (bulkJobId != -1L) {
            viewModel.loadFromBulkJob(bulkJobId)
        } else {
            val ids = arguments?.getLongArray("selectedItemIds") ?: LongArray(0)
            viewModel.loadItems(ids)
        }

        bulkAdapter = BulkPrintAdapter(
            onIncrease = { itemId -> viewModel.incrementCopiesForItem(itemId) },
            onDecrease = { itemId -> viewModel.decrementCopiesForItem(itemId) },
            onRemove = { itemId ->
                val snapshot = viewModel.removeItem(itemId)
                if (snapshot != null) showUndoSnackbar(snapshot)
            }
        )

        binding.rvBulkItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
            adapter = bulkAdapter
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            if (items.isEmpty() && viewModel.hasLoadedOnce &&
                viewModel.hasPendingRemoval.value != true
            ) {
                findNavController().navigateUp()
                return@observe
            }
            bulkAdapter.submitList(items)
            val count = items.size
            binding.tvSelectedCount.text = resources.getQuantityString(
                R.plurals.bulk_print_item_count, count, count
            )
            updatePrintButton()
        }

        viewModel.copiesMap.observe(viewLifecycleOwner) { copiesMap ->
            bulkAdapter.updateCopies(copiesMap)
            updatePrintButton()
        }

        viewModel.globalCopies.observe(viewLifecycleOwner) { copies ->
            binding.tvGlobalCopies.text = copies.toString()
        }

        viewModel.printerStatus.observe(viewLifecycleOwner) { status ->
            applyPrinterStatus(status)
        }

        binding.btnCopiesIncrease.setOnClickListener { viewModel.incrementGlobalCopies() }
        binding.btnCopiesDecrease.setOnClickListener { viewModel.decrementGlobalCopies() }

        binding.btnPrintAll.setOnClickListener { startBulkPrint() }

        binding.btnClearAll.setOnClickListener { confirmClearAll() }

        binding.btnDismissWarning.setOnClickListener {
            binding.printerWarningBanner.visibility = View.GONE
            checkPrinterStatus()
        }

        checkPrinterStatus()
    }

    /**
     * Query the current printer status and push the result into the ViewModel.
     * The observer on [BulkPrintViewModel.printerStatus] then updates the UI.
     */
    private fun checkPrinterStatus() {
        val status = printerManager.getPrinterStatus()
        viewModel.updatePrinterStatus(status)
    }

    /**
     * Show or hide the warning banner and enable/disable the print button based
     * on [statusCode]. Called every time the LiveData emits a new value.
     */
    private fun applyPrinterStatus(statusCode: Int) {
        val isReady = statusCode == PrinterManager.STATUS_NORMAL
        if (isReady) {
            binding.printerWarningBanner.visibility = View.GONE
        } else {
            binding.tvPrinterWarning.text = PrinterManager.statusLabel(statusCode)
            binding.printerWarningBanner.visibility = View.VISIBLE
        }
        updatePrintButton()
    }

    private fun updatePrintButton() {
        val printerReady = viewModel.isPrinterReady()
        if (!printerReady) {
            binding.btnPrintAll.isEnabled = false
            binding.btnPrintAll.text = getString(R.string.printer_not_ready)
            return
        }
        binding.btnPrintAll.isEnabled = true
        val total = viewModel.totalLabelCount()
        binding.btnPrintAll.text = if (total > 0) {
            getString(R.string.print_all_count, total)
        } else {
            getString(R.string.print_all)
        }
    }

    /**
     * Asks the user to confirm before removing every item from the bulk print.
     * Clearing the whole list cannot be undone (unlike per-item removal), so a
     * confirmation dialog guards against an accidental tap. On confirmation the
     * ViewModel empties the list, which triggers the normal back-navigation.
     */
    private fun confirmClearAll() {
        val count = viewModel.items.value?.size ?: 0
        if (count == 0) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_all_title)
            .setMessage(getString(R.string.clear_all_message, count))
            .setPositiveButton(R.string.clear) { _, _ -> viewModel.clearAll() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startBulkPrint() {
        val items = viewModel.items.value ?: return
        if (items.isEmpty()) return

        binding.btnPrintAll.isEnabled = false
        binding.progressPrint.visibility = View.VISIBLE
        binding.progressPrint.max = items.size
        binding.progressPrint.progress = 0

        var successCount = 0
        val failedDescriptions = mutableListOf<String>()

        fun printNext(index: Int) {
            if (index >= items.size) {
                binding.progressPrint.visibility = View.GONE
                updatePrintButton()

                val msg = if (failedDescriptions.isEmpty()) {
                    getString(R.string.print_success, successCount)
                } else {
                    getString(
                        R.string.print_partial,
                        successCount,
                        failedDescriptions.size,
                        failedDescriptions.joinToString(", ")
                    )
                }
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()

                saveBulkJob(items.map { item ->
                    BulkJobItem(
                        bulkJobId = 0,
                        barcode = item.barcode,
                        description = item.description,
                        price = item.price,
                        copies = viewModel.getCopiesForItem(item.itemId)
                    )
                })
                return
            }

            val item = items[index]
            val copies = viewModel.getCopiesForItem(item.itemId)
            printerManager.printShelfLabel(item, copies) { success, message ->
                binding.progressPrint.progress = index + 1
                if (success) successCount++ else failedDescriptions.add(item.description)
                printNext(index + 1)
            }
        }

        printNext(0)
    }

    /**
     * Persist the completed bulk job to the database on a background thread, then
     * prune any jobs that fall outside the configured retention window.
     *
     * Only called after the print sequence finishes so history always reflects
     * what was actually sent to the printer.
     */
    private fun saveBulkJob(jobItems: List<BulkJobItem>) {
        val retentionDays = PreferencesHelper.getBulkHistoryRetentionDays(requireContext())
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.bulkJobRepository.saveJob(jobItems)
            viewModel.bulkJobRepository.pruneByAge(retentionDays)
        }
    }

    /**
     * Removes the item immediately, then shows a Snackbar offering a timed Undo.
     *
     * - Tapping "Undo" calls [BulkPrintViewModel.undoRemove], which re-inserts the
     *   item at its original position and restores its copy count.
     * - If the Snackbar times out or is swiped away without tapping Undo,
     *   [BulkPrintViewModel.confirmRemoval] is called, which clears the pending-removal
     *   flag and triggers back-navigation if the list is now empty.
     */
    private fun showUndoSnackbar(snapshot: RemovedItemSnapshot) {
        Snackbar.make(
            binding.root,
            getString(R.string.bulk_item_removed, snapshot.item.description),
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.undo) { viewModel.undoRemove(snapshot) }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        viewModel.confirmRemoval()
                        if (viewModel.items.value.isNullOrEmpty()) {
                            findNavController().navigateUp()
                        }
                    }
                }
            })
            .show()
    }

    override fun onDestroyView() {
        printerManager.releasePrinter()
        super.onDestroyView()
        _binding = null
    }
}
