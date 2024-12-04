package th.co.opendream.vbs_recorder.fragments.settings

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.activities.SettingActivity
import th.co.opendream.vbs_recorder.databinding.FragmentSettingBinding
import th.co.opendream.vbs_recorder.utils.CommonUtil


class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null

    private val binding get() = _binding!!

    private var commonUtil: CommonUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        commonUtil = CommonUtil(requireContext())


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        binding.settingFilePrefix.text = commonUtil!!.getFilePrefix()
        binding.settingMetadata.text = commonUtil!!.getMetadata()
        binding.settingUploadToS3.isChecked = commonUtil!!.getCanUploadToS3()

        binding.settingS3Bucket.text = commonUtil!!.getS3BucketName()
        binding.settingS3Region.text = commonUtil!!.getS3Region()
        binding.settingS3AccessKey.text = commonUtil!!.getS3AccessKey()
        binding.settingS3SecretKey.text = commonUtil!!.getS3SecretKey()


        binding.buttonQrScanMetadata.setOnClickListener {
            val bundle = Bundle()
            bundle.apply {
                putString("key", "metadata")
            }
            findNavController().navigate(R.id.action_SettingFragment_to_QrScannerFragment, bundle)
        }

        binding.buttonQrScanS3Setting.setOnClickListener {
            val bundle = Bundle()
            bundle.apply {
                putString("key", "s3_setting")
            }
            findNavController().navigate(R.id.action_SettingFragment_to_QrScannerFragment, bundle)
        }

        binding.settingFilePrefix.setOnClickListener {
            showSettingTextDialog(inflater, binding.settingFilePrefix, "File Prefix", commonUtil!!.getFilePrefix())
        }

        binding.settingUploadToS3.setOnCheckedChangeListener { _, isChecked ->
            commonUtil!!.setUploadToS3(isChecked)

        }

        binding.settingMetadata.setOnClickListener {
            showSettingTextDialog(inflater, binding.settingMetadata, "Metadata", commonUtil!!.getMetadata())
        }

        binding.settingS3Bucket.setOnClickListener {
            showSettingTextDialog(inflater, binding.settingS3Bucket, "S3 Bucket", commonUtil!!.getS3BucketName() ?: "")
        }

        binding.settingS3Region.setOnClickListener {
            showSettingTextDialog(inflater, binding.settingS3Region, "S3 Region", commonUtil!!.getS3Region() ?: "")
        }

        binding.settingS3AccessKey.setOnClickListener {
            showSettingTextDialog(inflater, binding.settingS3AccessKey, "S3 Access Key", commonUtil!!.getS3AccessKey() ?: "")
        }

        binding.settingS3SecretKey.setOnClickListener {
            showSettingTextDialog(inflater, binding.settingS3SecretKey, "S3 Secret Key", commonUtil!!.getS3SecretKey() ?: "")
        }


        return binding.root
    }

    inner class onSettingClickListener : View.OnClickListener {
        private var newValue: String = ""

        constructor(value: String) {
            newValue = value
        }

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.settingFilePrefix -> {
                    commonUtil!!.setFilePrefix(newValue)
                    binding.settingFilePrefix.text = newValue
                }
                R.id.settingMetadata -> {
                    commonUtil!!.setMetadata(newValue)
                    binding.settingMetadata.text = newValue
                }
                R.id.settingS3Bucket -> {
                    commonUtil!!.setS3BucketName(newValue)
                    binding.settingS3Bucket.text = newValue
                }
                R.id.settingS3Region -> {
                    commonUtil!!.setS3Region(newValue)
                    binding.settingS3Region.text = newValue
                }
                R.id.settingS3AccessKey -> {
                    commonUtil!!.setS3AccessKey(newValue)
                    binding.settingS3AccessKey.text = newValue
                }
                R.id.settingS3SecretKey -> {
                    commonUtil!!.setS3SecretKey(newValue)
                    binding.settingS3SecretKey.text = newValue
                }
            }
        }
    }

    private fun showSettingTextDialog(inflater: LayoutInflater, v: View, title: String, oldValue: String) {
        val dialogView = inflater.inflate(R.layout.dialog_setting_form, null)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<TextView>(R.id.title).text = title

        val input = dialogView.findViewById<EditText>(R.id.input)
        input.setText(oldValue)

        dialogView.findViewById<Button>(R.id.button_submit).setOnClickListener{
            val newValue = input.text.toString()
            onSettingClickListener(newValue).onClick(v)
            builder.dismiss()

        }
        dialogView.findViewById<Button>(R.id.button_cancel).setOnClickListener{
            builder.cancel()
        }

        builder.setView(dialogView)
        builder.show()
    }

    override fun onResume() {
        super.onResume()

        (activity as SettingActivity).changeToolbarTitle(getString(R.string.title_setting))
    }


}