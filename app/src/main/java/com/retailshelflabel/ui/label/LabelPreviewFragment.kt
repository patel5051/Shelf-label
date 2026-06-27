package com.retailshelflabel.ui.label

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentLabelPreviewBinding
import com.retailshelflabel.util.PreferencesHelper

/**
 * Label preview and print screen.
 *
 * Shows a live rendered preview of the shelf label using [LabelView],
 * a copy count picker, and Print / Test Print buttons.
 *
 * Arguments:
 *  "itemId" Long — item to preview
 */
class LabelPreviewFragment : Fragment() {

    private var _binding: FragmentLabelPreviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LabelPreviewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabelPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemId = arguments?.getLong("itemId", -1L) ?: -1L
        if (itemId != -1L) viewModel.loadItem(itemId)

        val defaultCopies = PreferencesHelper.getDefaultCopies(requireContext())
        viewModel.setCopies(defaultCopies)
        binding.numberPickerCopies.minValue = 1
        binding.numberPickerCopies.maxValue = 50
        binding.numberPickerCopies.value = defaultCopies
        binding.numberPickerCopies.setOnValueChangedListener { _, _, newVal ->
            viewModel.setCopies(newVal)
        }

        viewModel.item.observe(viewLifecycleOwner) { item ->
            if (item == null) return@observe
            binding.labelView.setItem(
                item = item,
                storeName = PreferencesHelper.getStoreName(requireContext()),
                currency = PreferencesHelper.getCurrencySymbol(requireContext()),
                barcodeType = PreferencesHelper.getBarcodeType(requireContext())
            )
        }

        viewModel.printResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LabelPreviewViewModel.PrintResult.Success -> {
                    viewModel.clearResult()
                    val msg = if (result.copies == 0)
                        getString(R.string.test_print_success)
                    else
                        getString(R.string.print_success, result.copies)
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                }
                is LabelPreviewViewModel.PrintResult.Error -> {
                    viewModel.clearResult()
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
                null -> {}
            }
        }

        binding.btnPrint.setOnClickListener { viewModel.print() }
        binding.btnTestPrint.setOnClickListener { viewModel.printTest() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
