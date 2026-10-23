package com.remote.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDeviceDao {
    @Query("SELECT * FROM target_devices ORDER BY id DESC")
    fun getAllDevices(): Flow<List<TargetDeviceEntity>>

    @Query("SELECT * FROM target_devices WHERE id = :id")
    suspend fun getDeviceById(id: Long): TargetDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: TargetDeviceEntity): Long

    @Update
    suspend fun updateDevice(device: TargetDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: TargetDeviceEntity)

    @Query("DELETE FROM target_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)
}

@Dao
interface ButtonMappingDao {
    @Query("SELECT * FROM button_mappings WHERE device_id = :deviceId")
    fun getMappingsForDevice(deviceId: Long): Flow<List<ButtonMappingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: ButtonMappingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<ButtonMappingEntity>)

    @Update
    suspend fun updateMapping(mapping: ButtonMappingEntity)

    @Delete
    suspend fun deleteMapping(mapping: ButtonMappingEntity)

    @Query("DELETE FROM button_mappings WHERE device_id = :deviceId")
    suspend fun deleteMappingsForDevice(deviceId: Long)
}

@Database(
    entities = [TargetDeviceEntity::class, ButtonMappingEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun targetDeviceDao(): TargetDeviceDao
    abstract fun buttonMappingDao(): ButtonMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remote_control_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
