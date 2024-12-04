package th.co.opendream.vbs_recorder.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class VBSMigration {
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE records ADD COLUMN transfer_high_pass_file_path TEXT")
                db.execSQL("ALTER TABLE records ADD COLUMN transfer_low_pass_file_path TEXT")
            }
        }
    }

}