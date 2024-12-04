package th.co.opendream.vbs_recorder.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.MaterialToolbar

import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.databinding.ActivityMainBinding
import th.co.opendream.vbs_recorder.fragments.HomeFragment
import th.co.opendream.vbs_recorder.fragments.RecordFragment
import th.co.opendream.vbs_recorder.services.S3UploaderBulkSyncService
import th.co.opendream.vbs_recorder.utils.NetworkUtil


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    var permissionToRecordAccepted = false

    private val syncCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == S3UploaderBulkSyncService.ACTION_SYNC_COMPLETED) {

                val currentFragment = getCurrentFragment()
                if (currentFragment is HomeFragment) {
                    currentFragment.refreshData()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        val toolbarTitle: TextView = toolbar.findViewById(R.id.toolbar_title)
        toolbarTitle.text = "MSR"


        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)


        // Register the receiver
        val filter = IntentFilter(S3UploaderBulkSyncService.ACTION_SYNC_COMPLETED)
        registerReceiver(syncCompletedReceiver, filter)

        syncData()

    }

    private fun syncData() {
        if (NetworkUtil.isInternetConnected(this)) {
            val syncIntent = Intent(this, S3UploaderBulkSyncService::class.java)
            startService(syncIntent)
        } else {
            Snackbar.make(binding.root, "No internet connection", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (permissionToRecordAccepted) {
            // Trigger the service
            val currentFragment = getCurrentFragment()
            if (currentFragment is RecordFragment) {
                if (!currentFragment.isRecording) { currentFragment.startRecording() }
            }

        } else {
            Snackbar.make(binding.root, "Permission denied to record audio", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_filter -> {
                val bundle = Bundle()
                val currentFragment = getCurrentFragment()
                if (currentFragment is HomeFragment) {
                    bundle.putString("start_date", currentFragment.filterStartDate)
                    bundle.putString("end_date", currentFragment.filterEndDate)
                }

                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_HomeFragment_to_FilterFragment, bundle)
                true
            }
            R.id.action_refresh -> {
                val currentFragment = getCurrentFragment()
                if (currentFragment is HomeFragment) {
                    syncData()
                    currentFragment.refreshData()
                }
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_up, R.anim.no_animation)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (getCurrentFragment() is HomeFragment) {
            return
        }
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.primaryNavigationFragment
    }

    fun changeToolbarTitle(title: String) {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val toolbarTitle: TextView = toolbar.findViewById(R.id.toolbar_title)
        toolbarTitle.text = title
    }
}