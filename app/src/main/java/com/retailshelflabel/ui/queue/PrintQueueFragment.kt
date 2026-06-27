package com.retailshelflabel.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentPrintQueueBinding

/**
 * Print Queue screen.
 *
 * Shows all items queued for printing, lets the user adjust copy counts per item,
 * remove individual items, and print the entire batch at once.
 *
 * Items are added here from [ItemDetailFragment] via [PrintQueueManager].
 */
class PrintQueueFragment : Fragment() {

    private var _binding: FragmentPrintQueueBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PrintQueueViewModel by viewModels()
    private lateinit var adapter: PrintQueueAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrintQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PrintQueueAdapter(
            onCopiesChanged = { itemId, copies -> viewModel.setCopies(itemId, copies) },
            onRemove = { itemId ->
                val snapshot = viewModel.remove(itemId)
                if (snapshot != null) showUndoSnackbar(snapshot)
            }
        )

        binding.rvQueue.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@PrintQueueFragment.adapter
        }

        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            val empty = entries.isEmpty()
            binding.tvEmptyQueue.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvQueue.visibility = if (empty) View.GONE else View.VISIBLE
            binding.btnPrintAll.isEnabled = !empty
            val totalLabels = entries.sumOf { it.copies }
            binding.btnPrintAll.text =
                if (empty) getString(R.string.print_all)
                else getString(R.string.print_all_count, totalLabels)
        }

        viewModel.printResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is PrintQueueViewModel.PrintResult.Success -> {
                    viewModel.clearResult()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.print_success, result.totalLabels),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                is PrintQueueViewModel.PrintResult.Partial -> {
                    viewModel.clearResult()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.print_partial, result.printed, result.failed, result.lastError),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is PrintQueueViewModel.PrintResult.Error -> {
                    viewModel.clearResult()
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
                null -> {}
            }
        }

        binding.btnPrintAll.setOnClickListener { viewModel.printAll() }

        binding.btnClearQueue.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_queue_title)
                .setMessage(R.string.clear_queue_message)
                .setPositiveButton(R.string.clear) { _, _ -> viewModel.clearQueue() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    /**
     * Shows a Snackbar offering a timed Undo after a queue row is removed.
     *
     * - Tapping "Undo" re-inserts the entry at its original position with its copy count.
     * - If the Snackbar times out or is dismissed without tapping Undo, the removal stands.
     */
    private fun showUndoSnackbar(snapshot: PrintQueueManager.RemovedEntrySnapshot) {
        Snackbar.make(
            binding.root,
            getString(R.string.bulk_item_removed, snapshot.entry.item.description),
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.undo) { viewModel.undoRemove(snapshot) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
