package com.retailshelflabel.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentItemEditBinding

/**
 * Add or edit an item.
 *
 * Arguments:
 *  "itemId"        Long    — edit existing item
 *  "prefillBarcode" String — pre-fill barcode for newly scanned items
 */
class ItemEditFragment : Fragment() {

    private var _binding: FragmentItemEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ItemEditViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemId = arguments?.getLong("itemId", -1L) ?: -1L
        val prefillBarcode = arguments?.getString("prefillBarcode")

        if (itemId != -1L) {
            viewModel.loadItem(itemId)
        } else if (!prefillBarcode.isNullOrBlank()) {
            binding.etBarcode.setText(prefillBarcode)
        }

        viewModel.item.observe(viewLifecycleOwner) { item ->
            if (item == null) return@observe
            binding.etBarcode.setText(item.barcode)
            binding.etDescription.setText(item.description)
            binding.etPrice.setText(item.price.toString())
            binding.etDepartment.setText(item.department)
            binding.etSize.setText(item.size)
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ItemEditViewModel.SaveResult.Success -> {
                    viewModel.clearResult()
                    findNavController().navigate(
                        R.id.action_edit_to_detail,
                        bundleOf("itemId" to result.itemId)
                    )
                }
                is ItemEditViewModel.SaveResult.Error -> {
                    viewModel.clearResult()
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
                null -> {}
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.save(
                barcode = binding.etBarcode.text.toString(),
                description = binding.etDescription.text.toString(),
                priceStr = binding.etPrice.text.toString(),
                department = binding.etDepartment.text.toString(),
                size = binding.etSize.text.toString()
            )
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
