package com.wNagiesEducationalCenterj_9905.data.db.Entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wNagiesEducationalCenterj_9905.vo.IMessageModel

@Entity(tableName = "announcement")
data class AnnouncementEntity(
    override val sender: String,
    override val level: String,
    override val content: String,
    override val read: String?,
    override var token: String?,
    override val date: String

) : IMessageModel {
    @PrimaryKey(autoGenerate = true)
    override var id: Int = 0
    override var uid: Int= 0
}