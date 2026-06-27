package com.retailshelflabel.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.retailshelflabel.R
import com.retailshelflabel.databinding.FragmentScanBinding
import com.retailshelflabel.sdk.ScanResultBus
import com.retailshelflabel.sdk.ScannerManager
import kotlinx.coroutines.launch

/**
 * Scan mode screen.
 *
 * Listens for SUNMI scanner broadcast results via [ScanResultBus].
 * When a barcode arrives it navigates automatically to ItemDetailFragment
 * which handles the "not found → create" flow.
 *
 * The user can also tap "Trigger Scan" which sends a software scan command
 * (requires SUNMI Scanner SDK .aar — see ScannerManager).
 */
class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    private lateinit var scannerManager: ScannerManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scannerManager = ScannerManager(requireContext())

        // Collect scan results — navigates to detail automatically
        viewLifecycleOwner.lifecycleScope.launch {
            ScanResultBus.events.collect { barcode ->
                findNavController().navigate(
                    R.id.action_scan_to_detail,
                    bundleOf("scannedBarcode" to barcode)
                )
            }
        }

        binding.btnTriggerScan.setOnClickListener {
            scannerManager.startScan()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
