package th.co.opendream.vbs_recorder.db

import androidx.lifecycle.LiveData

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


import th.co.opendream.vbs_recorder.models.Record


@Dao
interface RecordDao {
    @Insert
    fun insert(record: Record)

    @Insert
    fun insertAll(vararg record: Record)

    @Delete
    fun delete(record: Record)

    @Query("SELECT * FROM records ORDER BY created_at DESC")
    fun getAll(): List<Record>

    @Query("SELECT * FROM records LIMIT 1")
    fun getOne(): List<Record>

    @Query("SELECT * FROM records WHERE id = :id")
    fun getById(id: Int): LiveData<Record>

    @Query("SELECT * FROM records WHERE id = :id")
    fun filterById(id: Int): List<Record>

    @Query("SELECT * FROM records WHERE file_path = :filePath")
    fun filterByFilePath(filePath: String): List<Record>

    @Query("SELECT * FROM records WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    fun filterByCreatedAt(startDate: Long, endDate: Long): List<Record>

    @Query("SELECT * FROM records WHERE created_at >= :startDate ORDER BY created_at DESC")
    fun filterByStartDateCreatedAt(startDate: Long): List<Record>

    @Query("SELECT * FROM records WHERE created_at <= :endDate ORDER BY created_at DESC")
    fun filterByEndDateCreatedAt(endDate: Long): List<Record>

    @Query("SELECT * FROM records WHERE is_synced LIKE :isSynced ORDER BY created_at DESC")
    fun filterBySynced(isSynced: Boolean): List<Record>

    @Update
    fun update(record: Record)

    @Query("DELETE  FROM records WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Int>)

}