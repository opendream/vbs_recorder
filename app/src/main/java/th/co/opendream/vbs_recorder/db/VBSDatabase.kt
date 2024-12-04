package th.co.opendream.vbs_recorder.db

import androidx.room.Database
import androidx.room.RoomDatabase

import th.co.opendream.vbs_recorder.models.Record


@Database(entities = [Record::class], version = 2)
abstract class VBSDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}

