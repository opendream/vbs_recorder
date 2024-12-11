package th.co.opendream.vbs_recorder.fragments

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.activities.MainActivity
import th.co.opendream.vbs_recorder.databinding.FragmentPlayBackBinding
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.services.S3UploaderService
import th.co.opendream.vbs_recorder.utils.SettingsUtil
import th.co.opendream.vbs_recorder.utils.DateUtil
import java.util.Date
import java.util.Timer
import java.util.TimerTask


class PlayBackFragment : Fragment() {
    private var _binding: FragmentPlayBackBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var recordPath = "audiorecord.wav"
    private var highPassRecordPath: String? = null
    private var lowPassRecordPath: String? = null

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var timer: Timer? = null

    private var durationText = "00:00:00"

    var db: VBSDatabase? = null
    var record: th.co.opendream.vbs_recorder.models.Record? = null

    private var currentRecordType = "original"

    private var uploadServiceIntent: Intent? = null
    private val uploadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "UPLOAD_COMPLETED") {
                Snackbar.make(binding.root, "Upload completed", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

                binding.buttonSyncRecord.isEnabled = false
                binding.buttonSyncRecord.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.mipmap.tabler_icon_cloud_upload
                    )
                )
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPlayBackBinding.inflate(inflater, container, false)

        recordPath = requireContext().externalCacheDir.toString() + "/" + recordPath

        binding.buttonPlayPause.setOnClickListener {
            var uri = readAudioFileFromMediaStore(requireContext().contentResolver, recordPath)

            if (currentRecordType == "high") {
                uri = readAudioFileFromMediaStore(
                    requireContext().contentResolver,
                    highPassRecordPath!!
                )
            } else if (currentRecordType == "low") {
                uri = readAudioFileFromMediaStore(
                    requireContext().contentResolver,
                    lowPassRecordPath!!
                )
            }


            // Play or pause the recording
            if (uri != null) {
                if (isPlaying) {
                    pauseRecording()
                } else {
                    playRecording()
                }
            } else {
                Snackbar.make(
                    binding.root,
                    "File does not exist in MediaStore: $recordPath",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Action", null).show()
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        binding.buttonDeleteRecord.setOnClickListener {
            showDeleteDialog(inflater, binding.root)
        }


        binding.buttonSyncRecord.setOnClickListener {
            showSyncedDialog(inflater, binding.root)
        }

        binding.buttonNext.setOnClickListener {
            if (mediaPlayer != null) {
                mediaPlayer?.seekTo(mediaPlayer!!.currentPosition + SKIP_MEDIA_PLAYER_MS)

                refreshMediaPlayerControl(true)
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress * 1000) // Convert seconds to milliseconds
                    refreshMediaPlayerControl(true)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optional: Add any action you want to perform when the user starts touching the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optional: Add any action you want to perform when the user stops touching the SeekBar
            }
        })

        binding.buttonPrevious.setOnClickListener {
            if (mediaPlayer != null) {
                mediaPlayer?.seekTo(mediaPlayer!!.currentPosition - SKIP_MEDIA_PLAYER_MS)

                refreshMediaPlayerControl(true)

//                Snackbar.make(binding.root, "<< ${SKIP_MEDIA_PLAYER_MS/1000}s", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
            }

        }

        binding.badgeRecordOriginal.setOnClickListener {
            if (currentRecordType == "original") {
                return@setOnClickListener
            }

            currentRecordType = "original"
            refreshCurrentRecordType()
        }

        binding.badgeRecordHighPass.setOnClickListener {
            if (currentRecordType == "high") {
                return@setOnClickListener
            }

            currentRecordType = "high"
            refreshCurrentRecordType()

        }

        binding.badgeRecordLowPass.setOnClickListener {
            if (currentRecordType == "low") {
                return@setOnClickListener
            }

            currentRecordType = "low"
            refreshCurrentRecordType()

        }

        return binding.root

    }

    private fun playRecording() {
        mediaPlayer?.start()

        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    refreshMediaPlayerControl()
                }
            }
        }, 0, 1000)

        isPlaying = true

        binding.buttonPlayPause.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.baseline_pause_24
            )
        )
        binding.animationView.playAnimation()

    }

    private fun pauseRecording() {
        mediaPlayer?.pause()

        binding.animationView.pauseAnimation()

        isPlaying = false
        binding.buttonPlayPause.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.baseline_play_arrow_24
            )
        )

    }

    private fun stopRecording() {
        mediaPlayer?.release()
        binding.animationView.pauseAnimation()

        isPlaying = false
        binding.buttonPlayPause.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.baseline_play_arrow_24
            )
        )

        binding.seekBar.progress = 0
        binding.timeText.text = "00:00:00 / ${durationText}"

        timer?.cancel()

        mediaPlayer = null
    }

    private fun showDeleteDialog(inflater: LayoutInflater, v: View) {
        val dialogView = inflater.inflate(R.layout.dialog_default, null)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<TextView>(R.id.title).text = "Delete Record"
        dialogView.findViewById<TextView>(R.id.description).text =
            "Are you sure you want to delete this record?"

        dialogView.findViewById<Button>(R.id.button_yes).setOnClickListener {
            deleteFilePath(recordPath)

            if (highPassRecordPath != null) {
                deleteFilePath(highPassRecordPath!!)
            }

            if (lowPassRecordPath != null) {
                deleteFilePath(lowPassRecordPath!!)
            }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    db!!.recordDao().delete(record!!)
                }
            }

            requireActivity().onBackPressed()
            builder.dismiss()
        }

        dialogView.findViewById<Button>(R.id.button_no).setOnClickListener {
            builder.cancel()
        }

        builder.setView(dialogView)
        builder.show()
    }

    private fun showSyncedDialog(inflater: LayoutInflater, v: View) {
        val dialogView = inflater.inflate(R.layout.dialog_default, null)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<TextView>(R.id.title).text = "Sync Record"
        dialogView.findViewById<TextView>(R.id.description).text =
            "Are you sure you want to sync this record to S3?"

        dialogView.findViewById<Button>(R.id.button_yes).setOnClickListener {
            uploadServiceIntent = Intent(requireContext(), S3UploaderService::class.java)
            uploadServiceIntent!!.putExtra("recordId", record!!.id)

            ContextCompat.startForegroundService(requireContext(), uploadServiceIntent!!)

            Snackbar.make(binding.root, "Start syncing", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            builder.dismiss()
        }

        dialogView.findViewById<Button>(R.id.button_no).setOnClickListener {
            builder.cancel()
        }

        builder.setView(dialogView)
        builder.show()
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

        val recordId = arguments?.getInt("record_id") ?: -1

        if (recordId != -1) {

            db!!.recordDao().getById(recordId).observe(viewLifecycleOwner, { record ->
                this.record = record

                recordPath = "${record.filePath!!}${SettingsUtil.AUDIO_EXTENTION}"
                if (record.highPassFilePath != null) {
                    highPassRecordPath =
                        "${record.highPassFilePath}${SettingsUtil.AUDIO_EXTENTION}"

                    binding.badgeRecordHighPass.visibility = View.VISIBLE

                    currentRecordType = "high"
                    refreshCurrentRecordType()
                }

                if (record.lowPassFilePath != null) {
                    lowPassRecordPath =
                        "${record.lowPassFilePath}${SettingsUtil.AUDIO_EXTENTION}"
                    binding.badgeRecordLowPass.visibility = View.VISIBLE
                }

                binding.buttonPlayPause.isEnabled = true

                binding.title.text = record.title
                if (record.createdAt != null) {
                    binding.title.text =
                        "${DateUtil.formatLocaleDateTitle(Date(record.createdAt))} at ${
                            DateUtil.formatLocaleTimeTitle(
                                Date(record.createdAt)
                            )
                        }"
                }

                val seconds = record.duration?.toLong() ?: 0
                val hours = seconds / 3600
                val minutes = (seconds / 60) % 60
                val secs = seconds % 60

                durationText = String.format("%02d:%02d:%02d", hours, minutes, secs)
                binding.timeText.text = "00:00:00 / ${durationText}"

                val isSynced = record.isSynced
                if (isSynced == null || !isSynced) {
                    binding.buttonSyncRecord.isEnabled = true
                    binding.buttonSyncRecord.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.baseline_cloud_off_24
                        )
                    )
                } else {
                    binding.buttonSyncRecord.isEnabled = false
                    binding.buttonSyncRecord.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.mipmap.tabler_icon_cloud_upload
                        )
                    )
                }

                refreshMediaPlayer()

            })

        } else {
            Snackbar.make(binding.root, "Record not found", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            requireActivity().onBackPressed()
        }


    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).changeToolbarTitle("Playback")
        requireContext().registerReceiver(uploadCompleteReceiver, IntentFilter("UPLOAD_COMPLETED"))
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(uploadCompleteReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        _binding = null
    }

    private fun readAudioFileFromMediaStore(
        contentResolver: ContentResolver,
        fileName: String
    ): Uri? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                return Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
            }
        }
        return null
    }

    private fun deleteFilePath(filePath: String) {
        val uri = readAudioFileFromMediaStore(requireContext().contentResolver, filePath)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (uri != null) {
                    requireContext().contentResolver.delete(uri, null, null)
                }
            }
        }
    }

    private fun refreshCurrentRecordType() {
        when (currentRecordType) {
            "original" -> {
                binding.badgeRecordOriginal.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.badgeActiveBackground)
                binding.badgeRecordHighPass.backgroundTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.badgeInactiveBackground
                    )
                binding.badgeRecordLowPass.backgroundTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.badgeInactiveBackground
                    )

            }

            "high" -> {
                binding.badgeRecordOriginal.backgroundTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.badgeInactiveBackground
                    )
                binding.badgeRecordHighPass.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.badgeActiveBackground)
                binding.badgeRecordLowPass.backgroundTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.badgeInactiveBackground
                    )
            }

            else -> {
                binding.badgeRecordOriginal.backgroundTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.badgeInactiveBackground
                    )
                binding.badgeRecordHighPass.backgroundTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.badgeInactiveBackground
                    )
                binding.badgeRecordLowPass.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.badgeActiveBackground)
            }
        }

        refreshMediaPlayer()
    }

    private fun refreshMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer?.release()
        }

        var uri = readAudioFileFromMediaStore(requireContext().contentResolver, recordPath)
        if (currentRecordType == "high") {
            uri =
                readAudioFileFromMediaStore(requireContext().contentResolver, highPassRecordPath!!)
        } else if (currentRecordType == "low") {
            uri = readAudioFileFromMediaStore(requireContext().contentResolver, lowPassRecordPath!!)
        }

        if (uri != null) {
            val pfd = requireContext().contentResolver.openFileDescriptor(uri, "r")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(pfd?.fileDescriptor)
                prepare()
            }

            val maxDuration = mediaPlayer?.duration ?: 0

            Log.i("PlayBackFragment", "Max Duration: $maxDuration")
            val maxSeconds = maxDuration / 1000
            binding.seekBar.max = maxSeconds

            mediaPlayer?.setOnCompletionListener {
                isPlaying = false

                binding.animationView.pauseAnimation()
                binding.buttonPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.baseline_play_arrow_24
                    )
                )

                binding.seekBar.progress = 0
                binding.timeText.text = "00:00:00 / ${durationText}"

                mediaPlayer?.seekTo(0)
            }

            binding.seekBar.progress = 0
            binding.timeText.text = "00:00:00 / ${durationText}"
        }
    }

    private fun refreshMediaPlayerControl(force: Boolean = false) {
        val maxDuration = mediaPlayer?.duration ?: 0
        val maxSeconds = maxDuration / 1000

        if (isPlaying || force) {
            val duration = mediaPlayer?.currentPosition ?: 0
            var seconds = (duration / 1000)
            if (!force) {
                seconds += 1
            }

            Log.e(
                "PlayBackFragment",
                "Max Duration: $maxDuration, Duration: $duration, Seconds: $seconds"
            )

            if (seconds >= maxSeconds) {
                seconds = maxSeconds
            }

            binding.seekBar.progress = seconds

            val hours = seconds / 3600
            val minutes = (seconds / 60) % 60

            binding.timeText.text = String.format(
                "%02d:%02d:%02d / %s",
                hours,
                minutes,
                seconds,
                durationText
            )
        }
    }


    companion object {
        private const val TAG = "PlayBackFragment"
        private const val SKIP_MEDIA_PLAYER_MS = 1000
    }

}