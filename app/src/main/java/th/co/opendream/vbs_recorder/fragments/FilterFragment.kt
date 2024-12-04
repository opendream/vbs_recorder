package th.co.opendream.vbs_recorder.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.activities.MainActivity
import th.co.opendream.vbs_recorder.databinding.FragmentFilterBinding
import java.util.*

class FilterFragment : Fragment() {
    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)

        val startDate = arguments?.getString("start_date")
        val endDate = arguments?.getString("end_date")

        if (startDate != null && endDate != null) {
            binding.startDate.setText(startDate)
            binding.endDate.setText(endDate)
        }

        binding.buttonSubmit.setOnClickListener {
            val startDate = binding.startDate.text.toString()
            val endDate = binding.endDate.text.toString()


            val bundle = Bundle().apply {
                putString("start_date", startDate)
                putString("end_date", endDate)
            }


            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.FilterFragment, true)
                .setPopUpTo(R.id.HomeFragment, true)
                .build()

            findNavController().navigate(R.id.action_FilterFragment_to_HomeFragment, bundle, navOptions)

        }

        setupDatePickers()

        return binding.root
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            when (view.tag) {
                "start_date" -> binding.startDate.setText(selectedDate)
                "end_date" -> binding.endDate.setText(selectedDate)
            }
        }

        binding.startDate.setOnClickListener {
            showDatePickerDialog("start_date", calendar, dateSetListener)
        }

        binding.endDate.setOnClickListener {
            showDatePickerDialog("end_date", calendar, dateSetListener)
        }
    }

    private fun showDatePickerDialog(tag: String, calendar: Calendar, dateSetListener: DatePickerDialog.OnDateSetListener) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), dateSetListener, year, month, day)
        datePickerDialog.datePicker.tag = tag
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).changeToolbarTitle("Filter")
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear() // This will hide the menu
        super.onPrepareOptionsMenu(menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    companion object {
        val formatter = "dd/MM/yyyy"
    }
}