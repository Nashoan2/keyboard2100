package org.fossify.keyboard.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.keyboard.models.Clip

@Dao
interface ClipsDao {
    @Query("SELECT * FROM clips ORDER BY is_pinned DESC, id DESC")
    fun getClips(): List<Clip>

    @Query("SELECT * FROM clips WHERE is_pinned = 1 ORDER BY id DESC")
    fun getPinnedClips(): List<Clip>

    @Query("SELECT * FROM clips WHERE is_pinned = 0 ORDER BY id DESC")
    fun getRecentClips(): List<Clip>

    @Query("SELECT id FROM clips WHERE value = :value")
    fun getClipWithValue(value: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(clip: Clip): Long

    @Query("UPDATE clips SET is_pinned = :isPinned WHERE id = :id")
    fun updatePinned(id: Long, isPinned: Boolean)

    // IMPORTANT: Rule specified by user:
    // Pinned texts in the clipboard MUST NOT be deleted until unpinned!
    @Query("DELETE FROM clips WHERE id = :id AND is_pinned = 0")
    fun delete(id: Long)

    @Query("DELETE FROM clips WHERE id = :id")
    fun forceDelete(id: Long)

    // Deleting all clipboard texts deletes ONLY unpinned clips! Pinned items are protected!
    @Query("DELETE FROM clips WHERE is_pinned = 0")
    fun deleteAll()
}
