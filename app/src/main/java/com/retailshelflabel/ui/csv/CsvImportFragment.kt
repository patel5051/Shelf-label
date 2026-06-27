package com.retailshelflabel.ui.csv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentCsvImportBinding

/**
 * CSV import screen.
 *
 * The user taps "Choose File", picks a CSV from Files/Downloads, and the
 * ViewModel parses + upserts all rows, then shows a summary dialog.
 */
class CsvImportFragment : Fragment() {

    private var _binding: FragmentCsvImportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CsvImportViewModel by viewModels()

    private val pickFile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.importCsv(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCsvImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CsvImportViewModel.ImportState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnChooseFile.isEnabled = true
                }
                is CsvImportViewModel.ImportState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnChooseFile.isEnabled = false
                }
                is CsvImportViewModel.ImportState.Complete -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnChooseFile.isEnabled = true
                    showSummaryDialog(state)
                    viewModel.reset()
                }
                is CsvImportViewModel.ImportState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnChooseFile.isEnabled = true
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.import_error_title)
                        .setMessage(state.message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    viewModel.reset()
                }
            }
        }

        binding.btnChooseFile.setOnClickListener {
            pickFile.launch("text/*")
        }
    }

    private fun showSummaryDialog(result: CsvImportViewModel.ImportState.Complete) {
        val errorText = if (result.errorMessages.isNotEmpty()) {
            "\n\nErrors:\n" + result.errorMessages.joinToString("\n")
        } else ""

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_complete_title)
            .setMessage(
                getString(
                    R.string.import_summary,
                    result.newItems,
                    result.updatedItems,
                    result.errors
                ) + errorText
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
