package com.retailshelflabel.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentItemDetailBinding
import com.retailshelflabel.ui.queue.PrintQueueManager
import com.retailshelflabel.util.PreferencesHelper

/**
 * Shows full details for a single item.
 *
 * Accepts arguments:
 *  "itemId"        Long   — load item by database ID
 *  "scannedBarcode" String — look up item by barcode (from scanner broadcast)
 */
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ItemDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemId = arguments?.getLong("itemId", -1L) ?: -1L
        val scannedBarcode = arguments?.getString("scannedBarcode")

        when {
            itemId != -1L -> viewModel.loadItem(itemId)
            scannedBarcode != null -> viewModel.lookupBarcode(scannedBarcode)
        }

        viewModel.item.observe(viewLifecycleOwner) { item ->
            if (item == null) return@observe
            val currency = PreferencesHelper.getCurrencySymbol(requireContext())
            binding.tvDescription.text = item.description
            binding.tvPrice.text = "$currency${String.format("%.2f", item.price)}"
            binding.tvBarcode.text = item.barcode
            binding.tvDepartment.text = item.department.ifBlank { "—" }
            binding.tvSize.text = item.size.ifBlank { "—" }
            binding.detailContent.visibility = View.VISIBLE
            binding.tvNotFound.visibility = View.GONE

            // Update the "Add to Queue" button label based on current queue state
            val inQueue = PrintQueueManager.contains(item.itemId)
            binding.btnAddToQueue.text = getString(
                if (inQueue) R.string.in_queue else R.string.add_to_queue
            )
        }

        viewModel.notFound.observe(viewLifecycleOwner) { notFound ->
            if (notFound) {
                binding.detailContent.visibility = View.GONE
                binding.tvNotFound.visibility = View.VISIBLE
                binding.tvNotFoundBarcode.text =
                    getString(R.string.not_found_barcode, scannedBarcode)
            }
        }

        binding.btnPrintLabel.setOnClickListener {
            val item = viewModel.item.value ?: return@setOnClickListener
            findNavController().navigate(
                R.id.action_detail_to_label,
                bundleOf("itemId" to item.itemId)
            )
        }

        binding.btnAddToQueue.setOnClickListener {
            val item = viewModel.item.value ?: return@setOnClickListener
            val inQueue = PrintQueueManager.contains(item.itemId)
            if (inQueue) {
                // Navigate to queue so the user can see it
                findNavController().navigate(R.id.action_detail_to_queue)
            } else {
                PrintQueueManager.add(item, PreferencesHelper.getDefaultCopies(requireContext()))
                binding.btnAddToQueue.text = getString(R.string.in_queue)
                Snackbar.make(binding.root, R.string.added_to_queue, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.view_queue) {
                        findNavController().navigate(R.id.action_detail_to_queue)
                    }
                    .show()
            }
        }

        binding.btnEditItem.setOnClickListener {
            val item = viewModel.item.value ?: return@setOnClickListener
            findNavController().navigate(
                R.id.action_detail_to_edit,
                bundleOf("itemId" to item.itemId)
            )
        }

        binding.btnCreateNew.setOnClickListener {
            findNavController().navigate(
                R.id.action_detail_to_edit,
                bundleOf("prefillBarcode" to scannedBarcode)
            )
        }

        binding.btnDeleteItem.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteItem {
                        Snackbar.make(binding.root, R.string.item_deleted, Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
