package th.co.opendream.vbs_recorder.activities

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.databinding.ActivitySettingBinding
import th.co.opendream.vbs_recorder.fragments.settings.QrScannerFragment


class SettingActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var binding: ActivitySettingBinding

    private val REQUEST_CAMERA_PERMISSION = QrScannerFragment.REQUEST_CODE_PERMISSIONS
    private var permissions: Array<String> = QrScannerFragment.REQUIRED_PERMISSIONS

    var permissionCameraAccepted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        val toolbarTitle: TextView = toolbar.findViewById(R.id.toolbar_title)
        toolbarTitle.text = getText(R.string.title_setting)


        val navController = findNavController(R.id.nav_host_fragment_content_setting)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionCameraAccepted = if (requestCode == REQUEST_CAMERA_PERMISSION) {
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (permissionCameraAccepted) {
            // Trigger the service
            val currentFragment = getCurrentFragment()
            if (currentFragment is QrScannerFragment) {
                currentFragment.startCamera()
            }

        } else {
            Snackbar.make(binding.root, "Permission denied to record audio", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    private fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_setting) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.primaryNavigationFragment
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_setting)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_setting, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_close -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    fun changeToolbarTitle(title: String) {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val toolbarTitle: TextView = toolbar.findViewById(R.id.toolbar_title)
        toolbarTitle.text = title
    }


}