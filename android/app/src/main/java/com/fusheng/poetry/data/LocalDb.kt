package com.fusheng.poetry.data

// 本地数据底座：诗集 + 展品（照片文件在 filesDir/photos/<photoId>.jpg）
// deletedAt 墓碑：App 是唯一编辑端，本地置墓碑后由局域网同步推给归档 API
import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "poems")
data class PoemEntity(
    @PrimaryKey val id: String,
    val poemKey: String, // 内置词库 id（自填为空串）
    val title: String,
    val author: String,
    val dynasty: String,
    val content: String, // 全文，句间以 \n 分隔
    val focusLine: String,
    val tags: String,
    val createdAt: String, // ISO 时间
    val deletedAt: String? = null,
)

@Entity(tableName = "exhibits")
data class ExhibitEntity(
    @PrimaryKey val id: String,
    val photoId: String,
    val note: String,
    val poemId: String,
    val focusLine: String,
    val source: String, // manual | ai
    val createdAt: String,
    val deletedAt: String? = null,
)

/** 浮生馆卡片：展品 + 关联的诗（出处） */
data class ExhibitWithPoem(
    @Embedded val exhibit: ExhibitEntity,
    @Relation(parentColumn = "poemId", entityColumn = "id")
    val poem: PoemEntity?,
)

@Dao
interface FushengDao {
    @Transaction
    @Query("SELECT * FROM exhibits WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun exhibitCards(): Flow<List<ExhibitWithPoem>>

    @Query("SELECT * FROM poems WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pickedPoems(): Flow<List<PoemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoem(poem: PoemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExhibit(exhibit: ExhibitEntity)

    // 诗集标签整理：整行写回 tags（逗号分隔）
    @Query("UPDATE poems SET tags = :tags WHERE id = :id")
    suspend fun updatePoemTags(id: String, tags: String)

    @Query("UPDATE exhibits SET deletedAt = :at WHERE id = :id")
    suspend fun removeExhibit(id: String, at: String)

    @Query("UPDATE poems SET deletedAt = :at WHERE id = :id")
    suspend fun removePoem(id: String, at: String)

    // 供局域网同步（含墓碑行）
    @Query("SELECT * FROM poems")
    suspend fun allPoemRows(): List<PoemEntity>

    @Query("SELECT * FROM exhibits")
    suspend fun allExhibitRows(): List<ExhibitEntity>
}

@Database(entities = [PoemEntity::class, ExhibitEntity::class], version = 1)
abstract class FushengDb : RoomDatabase() {
    abstract fun dao(): FushengDao

    companion object {
        @Volatile
        private var instance: FushengDb? = null

        fun get(context: Context): FushengDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FushengDb::class.java,
                    "fusheng.db",
                ).build().also { instance = it }
            }
    }
}
