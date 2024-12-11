package th.co.opendream.vbs_recorder.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.activities.MainActivity
import th.co.opendream.vbs_recorder.adapters.OnRecordClickListener
import th.co.opendream.vbs_recorder.adapters.RecordAdapter
import th.co.opendream.vbs_recorder.databinding.FragmentHomeBinding
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.models.Record
import th.co.opendream.vbs_recorder.services.Record2Service
import th.co.opendream.vbs_recorder.utils.SettingsUtil
import th.co.opendream.vbs_recorder.utils.DateUtil

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class HomeFragment : Fragment(), OnRecordClickListener {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!
    private var db: VBSDatabase? = null

    private var adapter: RecordAdapter? = null
    private var listener: OnRecordClickListener? = null

    var filterStartDate: String? = null
    var filterEndDate: String? = null

    private var deleteRecordIds = mutableListOf<Int>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val startDate = arguments?.getString("start_date")
        val endDate = arguments?.getString("end_date")

        filterStartDate = startDate
        filterEndDate = endDate

        if (startDate != null && endDate != null && (startDate.isNotEmpty() || endDate.isNotEmpty())) {
            // Use startDate and endDate as needed
            binding.textFilter.text = "From: ${if (startDate.isNotEmpty()) startDate else "N/A"} - To: ${if (endDate.isNotEmpty()) endDate else "N/A"}"
            binding.filterView.visibility = View.VISIBLE
        } else {
            binding.filterView.visibility = View.GONE
        }

        binding.buttonFilterClose.setOnClickListener {
            filterStartDate = null
            filterEndDate = null

            fetchRecords()

            binding.textFilter.text = "Start Date To End Date"
            binding.filterView.visibility = View.GONE
        }

        binding.btnSelectedAll.setOnClickListener {

            if (binding.btnSelectedAll.isChecked) {
                adapter?.selectAllRecords()

                binding.btnDeleteAll.isEnabled = true
            } else {
                adapter?.deselectAllRecords()

                binding.btnDeleteAll.isEnabled = false
            }
        }

        binding.btnDeleteAll.isEnabled = false
        binding.btnDeleteAll.setOnClickListener {
            deleteRecordIds = (adapter?.getDeleteSelectedRecords() ?: mutableListOf()).toMutableList()
            showDeleteDialog(inflater, it)
        }

        binding.btnCancelDelete.setOnClickListener {
            binding.confirmDeleteView.visibility = View.GONE
            binding.btnSelectedAll.isChecked = false

            adapter?.setCheckBoxesVisibility(false)
            adapter?.clearDeleteSelectedRecords()
        }


        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            fetchRecords()
        }, FETCH_DELAYED)

        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabRecord.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_RecordFragment)
        }

        binding.recordView.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_RecordFragment)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(
            requireContext(),
            VBSDatabase::class.java,
            "vbs_database"
        ).build()

        listener = this

//        fetchRecords()

    }

    private fun fetchRecords() {
        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {

                val format = FilterFragment.formatter

                if (filterStartDate != null && filterEndDate != null && filterStartDate!!.isNotEmpty() && filterEndDate!!.isNotEmpty()) {
                    val startTime = DateUtil.convertStringToTime(filterStartDate!!, format)
                    val endTime = DateUtil.convertStringToTime(filterEndDate!!, format, 1)
                    db!!.recordDao().filterByCreatedAt(startTime, endTime)
                } else if (filterStartDate != null && filterStartDate!!.isNotEmpty()) {
                    val startTime = DateUtil.convertStringToTime(filterStartDate!!, format)
                    db!!.recordDao().filterByStartDateCreatedAt(startTime)
                } else if (filterEndDate != null && filterEndDate!!.isNotEmpty()) {
                    val endTime = DateUtil.convertStringToTime(filterEndDate!!, format, 1)
                    db!!.recordDao().filterByEndDateCreatedAt(endTime)
                } else {
                    db!!.recordDao().getAll()
                }
            }
            setupListView(records)
        }
    }

    private fun setupListView(records: List<Record>) {
        adapter = RecordAdapter(requireContext(), records, listener!!)
        binding.recordListView.adapter = adapter

        updateEmptyView(records.isEmpty())

        binding.fabRecord.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_RecordFragment)
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recordListView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recordListView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    fun refreshData() {
        fetchRecords()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        if (!SettingsUtil.isServiceRunning(requireContext(), Record2Service::class.java)) {
            binding.fabRecord.visibility = View.VISIBLE
            binding.recordView.visibility = View.GONE
        } else {
            binding.fabRecord.visibility = View.GONE
            binding.recordView.visibility = View.VISIBLE
        }

        (activity as MainActivity).changeToolbarTitle("MSR")
    }


    override fun onRecordClick(record: Record) {

        val bundle = Bundle()
        bundle.putInt("record_id", record.id)

        findNavController().navigate(R.id.action_HomeFragment_to_PlayBackFragment, bundle)
    }

    override fun onRecordLongClick(record: Record): Boolean {
        binding.confirmDeleteView.visibility = View.VISIBLE
        adapter?.setCheckBoxesVisibility(true)
        return true
    }

    override fun onRecordDeleteClick(isActive: Boolean) {
        if (isActive) {
            binding.btnDeleteAll.isEnabled = true
        } else {
            binding.btnDeleteAll.isEnabled = false
        }
    }

    private fun showDeleteDialog(inflater: LayoutInflater, v: View) {
        if (deleteRecordIds.isEmpty()) {
            Snackbar.make(v, "Please select record to delete", Snackbar.LENGTH_SHORT).show()
            return
        }

        val dialogView = inflater.inflate(R.layout.dialog_default, null)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<TextView>(R.id.title).text = "Delete ${deleteRecordIds.size} records"
        dialogView.findViewById<TextView>(R.id.description).text =
            "Are you sure you want to delete this records. You can not revert this action?"

        dialogView.findViewById<Button>(R.id.button_yes).setOnClickListener {

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    db!!.recordDao().deleteByIds(deleteRecordIds)
                }

                fetchRecords()
            }

            binding.confirmDeleteView.visibility = View.GONE
            binding.btnSelectedAll.isChecked = false
            binding.btnDeleteAll.isEnabled = false

            adapter?.setCheckBoxesVisibility(false)
            adapter?.clearDeleteSelectedRecords()

            builder.cancel()
        }

        dialogView.findViewById<Button>(R.id.button_no).setOnClickListener {
            builder.cancel()
        }

        builder.setView(dialogView)
        builder.show()
    }

    companion object {
        const val TAG = "HomeFragment"
        const val FETCH_DELAYED = 500L
    }

}