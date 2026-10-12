package com.ymid.wakeonlan.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ymid.wakeonlan.R
import com.ymid.wakeonlan.databinding.FragmentHistoryBinding
import com.ymid.wakeonlan.ui.ConsoleHeaderHost

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private val adapter = HistoryAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecyclerView.adapter = adapter

        viewModel.logs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
            binding.historyEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            updateHeader()
        }
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }

    private fun updateHeader() {
        val host = activity as? ConsoleHeaderHost ?: return
        if (!isResumed) return
        val count = viewModel.logs.value?.size ?: return
        host.setHeaderSubtitle(resources.getQuantityString(R.plurals.history_entry_count, count, count))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
