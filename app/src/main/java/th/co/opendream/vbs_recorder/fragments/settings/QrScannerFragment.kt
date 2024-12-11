package th.co.opendream.vbs_recorder.fragments.settings

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.json.JSONException
import org.json.JSONObject
import th.co.opendream.vbs_recorder.activities.SettingActivity
import th.co.opendream.vbs_recorder.databinding.FragmentQrScannerBinding
import th.co.opendream.vbs_recorder.utils.SettingsUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class QrScannerFragment : Fragment() {
    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService

    private var isShowDialog = false
    private var isDone = false

    private var settingsUtil: SettingsUtil? = null
    private var settingKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsUtil = SettingsUtil(requireContext())

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        cameraExecutor = Executors.newSingleThreadExecutor()

        settingKey = arguments?.getString("key")

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        return binding.root
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val barcodeScanner = BarcodeScanning.getClient()
            val imageAnalysis = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor, { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {

                                    if (isShowDialog) {
                                        continue
                                    }

                                    val qrCode = barcode.displayValue
                                    Log.d("QrScannerFragment", "QR Code: $qrCode")

                                    showQRTextDialog(qrCode.toString())
                                    stopCamera()
                                    break
                                }
                            }
                            .addOnFailureListener {
                                Log.e("QrScannerFragment", "Barcode scanning failed", it)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }
                })
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("QrScannerFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun showQRTextDialog(qrCode: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder
            .setMessage("Found: ${qrCode}")
            .setTitle("QR Scan")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.cancel()
                isDone = true

                if (settingKey == "metadata") {
                    settingsUtil!!.setMetadata(qrCode)

                    onBackPressed()

                } else if (settingKey == "s3_setting") {

                    val isJson = try {
                        JSONObject(qrCode)
                        true
                    } catch (e: JSONException) {
                        false
                    }

                    if (isJson) {
                        try {
                            val jsonObject = JSONObject(qrCode)
                            settingsUtil!!.setS3BucketName(jsonObject.getString("bucket"))
                            settingsUtil!!.setS3Region(jsonObject.getString("region"))
                            settingsUtil!!.setS3AccessKey(jsonObject.getString("access_key"))
                            settingsUtil!!.setS3SecretKey(jsonObject.getString("secret_key"))

                            onBackPressed()
                            return@setPositiveButton
                        } catch (_: Exception) {}

                    }

                    AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("Invalid QR Code")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.cancel()
                            startCamera()
                            isShowDialog = false
                        }
                        .setOnDismissListener {
                            startCamera()
                            isShowDialog = false
                        }
                        .show()
                }

            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }.setOnDismissListener {
                if (!isDone) {
                    startCamera()
                    isShowDialog = false
                }
            }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()

        isShowDialog = true
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()

        _binding = null
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Snackbar.make(
                    binding.root,
                    "Permissions not granted by the user.",
                    Snackbar.LENGTH_SHORT
                ).show()


            }
        }
    }

    fun onBackPressed() {
        requireActivity().onBackPressed()
    }

    override fun onResume() {
        super.onResume()

        (activity as SettingActivity).changeToolbarTitle("QR Scanner")
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
        val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}