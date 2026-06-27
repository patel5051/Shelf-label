package com.retailshelflabel.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.retailshelflabel.R
import com.retailshelflabel.data.db.BulkJobWithItems
import com.retailshelflabel.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen — action buttons that navigate to the main app sections.
 * Modisoft Sync is at the top as the primary data-entry point.
 *
 * A compact "Recent bulk jobs" card is shown above the action buttons whenever
 * at least one bulk print job exists. Each row shows the job timestamp, item
 * count, and a Reprint button that navigates to BulkPrintFragment pre-populated
 * with the saved item snapshots. The card is hidden when no jobs exist.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private val timestampFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnModisoftSync.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_sync)
        }
        binding.btnScanItem.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
        }
        binding.btnSearchItems.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_search)
        }
        binding.btnImportCsv.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_csv)
        }
        binding.btnPrintQueue.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_queue)
        }
        binding.btnPrintHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
        binding.tvSeeAllBulkJobs.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }

        viewModel.recentBulkJobs.observe(viewLifecycleOwner) { jobs ->
            bindRecentBulkJobsCard(jobs)
        }
    }

    /**
     * Show or hide the recent-bulk-jobs card and populate up to 2 job rows.
     * The card is completely hidden (GONE) when [jobs] is empty so it takes
     * no space in the layout and staff on a fresh install see only the action
     * buttons.
     */
    private fun bindRecentBulkJobsCard(jobs: List<BulkJobWithItems>) {
        if (jobs.isEmpty()) {
            binding.cardRecentBulkJobs.visibility = View.GONE
            return
        }
        binding.cardRecentBulkJobs.visibility = View.VISIBLE

        val job0 = jobs.getOrNull(0)
        val job1 = jobs.getOrNull(1)

        if (job0 != null) {
            binding.rowRecentJob0.visibility = View.VISIBLE
            binding.tvRecentJob0Timestamp.text =
                timestampFormat.format(Date(job0.bulkJob.printedAt))
            binding.tvRecentJob0Stats.text = resources.getString(
                R.string.home_recent_bulk_stats,
                job0.bulkJob.totalItems,
                job0.bulkJob.totalCopies
            )
            binding.btnRecentReprint0.setOnClickListener {
                navigateToReprint(job0)
            }
        } else {
            binding.rowRecentJob0.visibility = View.GONE
        }

        if (job1 != null) {
            binding.dividerRecentJobs.visibility = View.VISIBLE
            binding.rowRecentJob1.visibility = View.VISIBLE
            binding.tvRecentJob1Timestamp.text =
                timestampFormat.format(Date(job1.bulkJob.printedAt))
            binding.tvRecentJob1Stats.text = resources.getString(
                R.string.home_recent_bulk_stats,
                job1.bulkJob.totalItems,
                job1.bulkJob.totalCopies
            )
            binding.btnRecentReprint1.setOnClickListener {
                navigateToReprint(job1)
            }
        } else {
            binding.dividerRecentJobs.visibility = View.GONE
            binding.rowRecentJob1.visibility = View.GONE
        }
    }

    /** Navigate to BulkPrintFragment pre-populated with the saved job's item snapshots. */
    private fun navigateToReprint(job: BulkJobWithItems) {
        findNavController().navigate(
            R.id.action_home_to_bulk_print,
            bundleOf(
                "bulkJobId" to job.bulkJob.bulkJobId,
                "selectedItemIds" to LongArray(0)
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
