package com.retailshelflabel.ui.search

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentSearchBinding
import com.retailshelflabel.sdk.ScanResultBus
import kotlinx.coroutines.launch

/**
 * Search screen — text search (debounced) and barcode scan lookup.
 *
 * Long-pressing any item enters multi-select mode: checkboxes appear on every
 * row. A "Print X items" FAB counts the selection; tapping it navigates to the
 * bulk-print confirmation screen. A "Cancel" FAB exits selection mode.
 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: ItemListAdapter

    /** 300 ms debounce prevents a Room query on every keystroke */
    private val searchDebounce = Handler(Looper.getMainLooper())
    private val searchRunnable = Runnable {
        viewModel.setQuery(binding.etSearch.text?.toString() ?: "")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ItemListAdapter(
            onClick = { item ->
                if (!adapter.isMultiSelectMode) {
                    findNavController().navigate(
                        R.id.action_search_to_detail,
                        bundleOf("itemId" to item.itemId)
                    )
                }
            },
            onLongClick = { item ->
                adapter.enterMultiSelectMode(item)
                updateMultiSelectUi(adapter.getSelectedIds())
            },
            onSelectionChanged = { selectedIds ->
                updateMultiSelectUi(selectedIds)
            }
        )

        binding.rvItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            this.adapter = this@SearchFragment.adapter
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items) {
                // After DiffUtil commits the list, check whether we are returning from the
                // bulk-print screen with a saved selection to restore.
                viewModel.consumeMultiSelectState()?.let { (ids, scrollPos) ->
                    adapter.restoreMultiSelectMode(ids)
                    updateMultiSelectUi(ids)
                    if (scrollPos > 0) {
                        (binding.rvItems.layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(scrollPos, 0)
                    }
                }
            }
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchDebounce.removeCallbacks(searchRunnable)
                searchDebounce.postDelayed(searchRunnable, 300L)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Collect SUNMI scanner broadcast results while this screen is visible
        viewLifecycleOwner.lifecycleScope.launch {
            ScanResultBus.events.collect { barcode ->
                if (!adapter.isMultiSelectMode) {
                    findNavController().navigate(
                        R.id.action_search_to_detail,
                        bundleOf("scannedBarcode" to barcode)
                    )
                }
            }
        }

        binding.btnAddItem.setOnClickListener {
            findNavController().navigate(R.id.action_search_to_edit)
        }

        binding.btnBulkPrint.setOnClickListener {
            val selectedItems = adapter.getSelectedItems()
            if (selectedItems.isNotEmpty()) {
                val ids = selectedItems.map { it.itemId }.toLongArray()
                // Persist multi-select state so it can be restored when the user
                // navigates back from the bulk-print screen.
                val lm = binding.rvItems.layoutManager as? LinearLayoutManager
                val firstVisible = lm?.findFirstVisibleItemPosition() ?: 0
                viewModel.saveMultiSelectState(adapter.getSelectedIds(), firstVisible)
                findNavController().navigate(
                    R.id.action_search_to_bulk_print,
                    bundleOf("selectedItemIds" to ids)
                )
            }
        }

        binding.btnCancelSelection.setOnClickListener {
            val layoutManager = binding.rvItems.layoutManager as? LinearLayoutManager
            val scrollState = layoutManager?.onSaveInstanceState()
            adapter.exitMultiSelectMode()
            showNormalModeUi()
            binding.rvItems.post {
                scrollState?.let { layoutManager?.onRestoreInstanceState(it) }
            }
        }
    }

    private fun updateMultiSelectUi(selectedIds: Set<Long>) {
        val count = selectedIds.size
        binding.btnAddItem.visibility = View.GONE
        binding.btnCancelSelection.visibility = View.VISIBLE
        if (count > 0) {
            binding.btnBulkPrint.visibility = View.VISIBLE
            binding.btnBulkPrint.text = resources.getQuantityString(
                R.plurals.bulk_print_fab_count, count, count
            )
        } else {
            binding.btnBulkPrint.visibility = View.GONE
        }
    }

    private fun showNormalModeUi() {
        binding.btnAddItem.visibility = View.VISIBLE
        binding.btnBulkPrint.visibility = View.GONE
        binding.btnCancelSelection.visibility = View.GONE
    }

    override fun onDestroyView() {
        searchDebounce.removeCallbacks(searchRunnable)
        super.onDestroyView()
        _binding = null
    }
}
