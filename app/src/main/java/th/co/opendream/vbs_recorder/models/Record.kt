package th.co.opendream.vbs_recorder.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "records")
data class Record (
    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "file_path") val filePath: String?,

    @ColumnInfo(name = "transfer_high_pass_file_path") var highPassFilePath: String?,
    @ColumnInfo(name = "transfer_low_pass_file_path") var lowPassFilePath: String?,

    @ColumnInfo(name = "duration") var duration: Int?,

    @ColumnInfo(name = "is_deleted") var isDeleted: Boolean?,

    @ColumnInfo(name = "is_synced") var isSynced: Boolean?,
    @ColumnInfo(name = "synced_path") var syncedPath: String?,
    @ColumnInfo(name = "synced_at") var syncedAt: Long?,

    @ColumnInfo(name = "created_at") val createdAt: Long?,
    @ColumnInfo(name = "updated_at") var updatedAt: Long?,
)

