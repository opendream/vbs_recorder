package th.co.opendream.vbs_recorder.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import th.co.opendream.vbs_recorder.activities.MainActivity
import th.co.opendream.vbs_recorder.databinding.FragmentRecordBinding
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.services.Record2Service
import th.co.opendream.vbs_recorder.utils.CommonUtil
import th.co.opendream.vbs_recorder.utils.DateUtil
import java.util.Date
import java.util.Timer
import java.util.TimerTask


class RecordFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var startTime: Long = 0

    private var recordServiceIntent: Intent? = null

    private var db: VBSDatabase? = null
    private var commonUtil: CommonUtil? = null
    private var timer: Timer? = null

    var isRecording = false

    var newRecord: th.co.opendream.vbs_recorder.models.Record? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        commonUtil = CommonUtil(requireContext())

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRecordBinding.inflate(inflater, container, false)

        val now = DateUtil.formatLocaleDate(Date())
        binding.title.text = now

        binding.timeText.text = "00:00:00"


        binding.buttonRecord.setOnClickListener { view ->

            if (!isRecording) {
                startRecording()

                binding.animationView.playAnimation()

            } else {
                stopRecording()

                binding.animationView.pauseAnimation()
            }

        }

        return binding.root

    }



    private fun startTimer() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    Log.d("RecordService", "Recording time: ${elapsedTime / 1000} seconds")

                    val seconds = (elapsedTime / 1000).toInt()

                    val hours = seconds / 3600
                    val minutes = (seconds / 60) % 60
                    val secs = seconds % 60
                    binding.timeText.text = String.format("%02d:%02d:%02d", hours, minutes, secs)
                }
            }
        }, 0, 1000)

    }

    private fun stopTimer() {
        timer?.cancel()

        startTime = System.currentTimeMillis()
        binding.timeText.text = "00:00:00"
    }


    fun startRecording() {
        val permissionToRecordAccepted = (activity as MainActivity).permissionToRecordAccepted

        if (!permissionToRecordAccepted) {
            (activity as MainActivity).requestPermissions()
        } else {
            Snackbar.make(binding.root, "Start recording", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            val now = Date()
            val title = "${DateUtil.formatLocaleDateTitle(now)} at ${DateUtil.formatLocaleTimeTitle(now)}"

            var prefix = CommonUtil.AUDIO_PREFIX
            if (commonUtil!!.getFilePrefix() != "") {
                prefix = commonUtil!!.getFilePrefix()
            }

            val output = "${prefix}${DateUtil.formatLocaleDateTimeFile(Date())}"

            recordServiceIntent = Intent(requireContext(), Record2Service::class.java)
            recordServiceIntent!!.putExtra("service_id", Record2Service.SERVICE_ID)
            recordServiceIntent!!.putExtra("title", title)
            recordServiceIntent!!.putExtra("filePath", output)
            recordServiceIntent!!.putExtra("segmentCount", 1)

            ContextCompat.startForegroundService(requireContext(), recordServiceIntent!!)

            startTime = System.currentTimeMillis()

            // Save the intent data to SharedPreferences
            val sharedPreferences = requireContext().getSharedPreferences("RecordPrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("title", title)
                putString("filePath", output)
                putLong("startTime", startTime)
                putInt("segmentCount", 1)
                apply()
            }

            startTimer()
            isRecording = true
            binding.buttonRecord.text = "Stop Recording"
        }
    }

    fun stopRecording() {
        if (recordServiceIntent == null) {
            return
        }

        Snackbar.make(binding.root, "Stop recording", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()

        requireActivity().stopService(recordServiceIntent)

        stopTimer()

        isRecording = false
        binding.buttonRecord.text = "Start Recording"

        requireActivity().onBackPressed()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear() // This will hide the menu
        super.onPrepareOptionsMenu(menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        db = Room.databaseBuilder(
            requireContext(),
            VBSDatabase::class.java,
            "vbs_database"
        ).build()

    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).changeToolbarTitle("Record")

        val sharedPreferences = requireContext().getSharedPreferences("RecordPrefs", Context.MODE_PRIVATE)
        val _title = sharedPreferences.getString("title", null)
        val _filePath = sharedPreferences.getString("filePath", null)
        val _startTime = sharedPreferences.getLong("startTime", System.currentTimeMillis())
        val _segmentCount = sharedPreferences.getInt("segmentCount", 1)

        if (_title != null && _filePath != null) {

            recordServiceIntent = Intent(requireContext(), Record2Service::class.java)
            recordServiceIntent!!.putExtra("service_id", Record2Service.SERVICE_ID)
            recordServiceIntent!!.putExtra("title", _title)
            recordServiceIntent!!.putExtra("filePath", _filePath)
            recordServiceIntent!!.putExtra("segmentCount", _segmentCount)

        }

        if (!CommonUtil.isServiceRunning(requireContext(), Record2Service::class.java)) {
            binding.buttonRecord.text = "Start Recording"
            isRecording = false

            binding.animationView.pauseAnimation()

        } else {
            binding.buttonRecord.text = "Stop Recording"
            isRecording = true

            binding.animationView.playAnimation()

            startTime = _startTime
            startTimer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        handler?.removeCallbacks(runnable!!)
    }


}