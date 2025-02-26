package li.songe.gkd.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import li.songe.gkd.debug.SnapshotExt
import java.io.File

@Entity(
    tableName = "snapshot",
)
@Serializable
data class Snapshot(
    @PrimaryKey @ColumnInfo(name = "id") override val id: Long,

    @ColumnInfo(name = "app_id") override val appId: String?,
    @ColumnInfo(name = "activity_id") override val activityId: String?,
    @ColumnInfo(name = "app_name") override val appName: String?,
    @ColumnInfo(name = "app_version_code") override val appVersionCode: Int?,
    @ColumnInfo(name = "app_version_name") override val appVersionName: String?,

    @ColumnInfo(name = "screen_height") override val screenHeight: Int,
    @ColumnInfo(name = "screen_width") override val screenWidth: Int,
    @ColumnInfo(name = "is_landscape") override val isLandscape: Boolean,

    ) : BaseSnapshot {

    val screenshotFile by lazy {
        File(
            SnapshotExt.getScreenshotPath(
                id
            )
        )
    }

    @Dao
    interface SnapshotDao {
        @Update
        suspend fun update(vararg objects: Snapshot): Int

        @Insert
        suspend fun insert(vararg users: Snapshot): List<Long>

        @Delete
        suspend fun delete(vararg users: Snapshot): Int

        @Query("SELECT * FROM snapshot")
        fun query(): Flow<List<Snapshot>>
    }
}





