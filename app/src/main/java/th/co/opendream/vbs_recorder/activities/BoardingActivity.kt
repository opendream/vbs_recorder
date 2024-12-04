package th.co.opendream.vbs_recorder.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.db.VBSMigration

class BoardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding)

        // Need to initialize the database before starting the main activity
        val db = Room.databaseBuilder(
            this,
            VBSDatabase::class.java,
            "vbs_database"
        ).addMigrations(VBSMigration.MIGRATION_1_2).build()


        // Access the database to trigger the migration
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.recordDao().getOne()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }, SPLASH_TIME_OUT)

    }

    override fun onResume() {
        super.onResume()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }, SPLASH_TIME_OUT)
    }

    companion object {
        private const val TAG = "BoardingActivity"
        private const val SPLASH_TIME_OUT: Long = 2000
    }
}