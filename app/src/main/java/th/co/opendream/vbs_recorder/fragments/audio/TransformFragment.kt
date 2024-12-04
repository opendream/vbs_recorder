package th.co.opendream.vbs_recorder.fragments.audio

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import th.co.opendream.vbs_recorder.databinding.FragmentAudioTransformBinding
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.utils.AudioUtil
import th.co.opendream.vbs_recorder.utils.CommonUtil
import java.io.File

class TransformFragment : Fragment() {
    private var _binding: FragmentAudioTransformBinding? = null

    private val binding get() = _binding!!

    var db: VBSDatabase? = null
    var record: th.co.opendream.vbs_recorder.models.Record? = null

    private var demoPath = "audiorecord.mp3"
    private var transformedPath = "audiorecord_high.mp3"
    private var transformedType = "high"

    private fun generateTransformFile() {
        val transformFilePath = requireContext().externalCacheDir.toString() + "/" + transformedPath + AUDIO_EXTENTION
        val demoFilePath = readAudioFileFromMediaStore(requireContext().contentResolver, demoPath)
        if (demoFilePath != null) {

            if (transformedType == "high") {
                AudioUtil().applyAudioFileHighPassFilter(
                    demoFilePath,
                    highFrequencyCutOff,
                    highOrder,
                    transformFilePath
                )
                saveToMediaStore(transformFilePath, record?.filePath + "_high")
            } else {
                AudioUtil().applyAudioFileLowPassFilter(
                    demoFilePath,
                    lowFrequencyCutOff,
                    lowOrder,
                    transformFilePath
                )
                saveToMediaStore(transformFilePath, record?.filePath + "_low")
            }

        }

    }

    private fun readAudioFileFromMediaStore(contentResolver: ContentResolver, fileName: String): String? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
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
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                return path
            }
        }
        return ""
    }

    private fun saveToMediaStore(filePath: String, displayName: String) {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
        }

        val resolver = requireContext().contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                val inputStream = File(filePath).inputStream()
                inputStream.copyTo(outputStream!!)
                inputStream.close()
            }
        }
    }

    private fun updateRecord() {
        if (transformedType == "high") {
            record!!.highPassFilePath = transformedPath
        } else {
            record!!.lowPassFilePath = transformedPath
        }

        CoroutineScope(Dispatchers.IO).launch {
            db!!.recordDao().update(record!!)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioTransformBinding.inflate(inflater, container, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        db = Room.databaseBuilder(
            requireContext(),
            VBSDatabase::class.java,
            "vbs_database"
        ).build()

        val recordId = arguments?.getInt("record_id") ?: -1
        val transformType = arguments?.getString("transform_type")

        if (recordId != -1) {

            db!!.recordDao().getById(recordId).observe(viewLifecycleOwner, { record ->
                this.record = record

                demoPath = "${record.filePath!!}${CommonUtil.AUDIO_EXTENTION}"
                transformedType = transformType ?: "high"

                if (record != null) {
                    if (transformedType == "high") {
                        transformedPath = "${record.filePath}_high"
                        generateTransformFile()
                    } else {
                        transformedPath = "${record.filePath}_low"
                        generateTransformFile()
                    }

                    updateRecord()

                    Snackbar.make(binding.root, "Record already generated", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()

                    requireActivity().onBackPressed()
                } else {
                    Snackbar.make(binding.root, "Record not found", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                    requireActivity().onBackPressed()
                }
            })

        } else {
            Snackbar.make(binding.root, "Record not found", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            requireActivity().onBackPressed()
        }

    }

    companion object {
        val highFrequencyCutOff = 1000.0
        val highOrder = 5

        val lowFrequencyCutOff = 1000.0
        val lowOrder = 5

        val AUDIO_EXTENTION = ".wav"
    }


}