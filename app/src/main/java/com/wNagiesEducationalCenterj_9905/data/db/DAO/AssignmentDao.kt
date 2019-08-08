package com.wNagiesEducationalCenterj_9905.data.db.DAO

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wNagiesEducationalCenterj_9905.data.db.Entities.AssignmentEntity

@Dao
interface AssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAssignment(assignmentEntityList: List<AssignmentEntity>)

    @Query("SELECT * FROM assignment WHERE  token = :token AND format = :format ORDER BY id DESC")
    fun getStudentAssignmentPDF(token: String, format: String = "pdf"): LiveData<List<AssignmentEntity>>

    @Query("DELETE FROM assignment WHERE format = :format")
    fun deleteAssignmentPDF(format: String = "pdf")

    @Query("SELECT * FROM assignment WHERE token = :token AND format = :format ORDER BY id DESC")
    fun getAssignmentImage(token: String, format: String = "image"): LiveData<List<AssignmentEntity>>

    @Query("DELETE FROM assignment WHERE format = :format")
    fun deleteAssignmentImage(format: String = "image")

    @Query("UPDATE assignment SET path = :path WHERE id = :id")
    fun updateAssignmentPath(path: String, id: Int): Int

    @Query("DELETE FROM assignment WHERE id = :id")
    fun deleteAssignmentById(id: Int)
}