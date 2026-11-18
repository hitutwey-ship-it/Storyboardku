package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryboardDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getAllProjectsOnce(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProject(projectId: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    // Characters
    @Query("SELECT * FROM characters WHERE projectId = :projectId ORDER BY id ASC")
    fun observeCharacters(projectId: Long): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getCharactersOnce(projectId: Long): List<CharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :characterId")
    suspend fun deleteCharacterById(characterId: Long)

    // Scenes
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY orderIndex ASC, id ASC")
    fun observeScenes(projectId: Long): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY orderIndex ASC, id ASC")
    suspend fun getScenesOnce(projectId: Long): List<SceneEntity>

    @Query("SELECT * FROM scenes")
    suspend fun getAllScenesOnce(): List<SceneEntity>

    @Query("SELECT * FROM characters")
    suspend fun getAllCharactersOnce(): List<CharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<SceneEntity>)

    @Update
    suspend fun updateScene(scene: SceneEntity)

    @Update
    suspend fun updateScenes(scenes: List<SceneEntity>)

    @Query("DELETE FROM scenes WHERE id = :sceneId")
    suspend fun deleteSceneById(sceneId: Long)

    @Transaction
    suspend fun reindexAndSaveScenes(scenes: List<SceneEntity>) {
        val reindexed = scenes.mapIndexed { idx, s -> s.copy(orderIndex = idx) }
        updateScenes(reindexed)
    }
}
