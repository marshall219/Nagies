package com.wNagiesEducationalCenterj_9905.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wNagiesEducationalCenterj_9905.R
import com.wNagiesEducationalCenterj_9905.common.ItemCallback
import com.wNagiesEducationalCenterj_9905.common.ViewFilesAction
import com.wNagiesEducationalCenterj_9905.common.utils.ServerPathUtil
import com.wNagiesEducationalCenterj_9905.vo.IFileModel
import kotlinx.android.synthetic.main.list_upload_files.view.*

class UploadFileAdapter : ListAdapter<IFileModel, UploadFileVH>(UploadFileDiff()) {
    private var itemCallback: ItemCallback<Triple<ViewFilesAction, Int?, String?>>? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadFileVH {
        return UploadFileVH(LayoutInflater.from(parent.context).inflate(R.layout.list_upload_files, parent, false))
    }

    override fun onBindViewHolder(holder: UploadFileVH, position: Int) {
        holder.bind(getItem(position), itemCallback)
    }

    fun setItemCallback(callback: ItemCallback<Triple<ViewFilesAction, Int?, String?>>) {
        itemCallback = callback
    }
}

class UploadFileVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: IFileModel?, itemCallback: ItemCallback<Triple<ViewFilesAction, Int?, String?>>?) {
        when (item?.format) {
            "pdf" -> {
                itemView.img_item_logo.setImageResource(R.drawable.ic_picture_as_pdf_black_24dp)
            }
            "image" -> {
                itemView.img_item_logo.setImageResource(R.drawable.ic_image_black_24dp)
            }
        }
        val sender = "Upload by: ${item?.teacherEmail}"
        val level = "Received: ${item?.studentName}"
        val date = "Date uploaded: ${item?.date}"
        item?.fileUrl?.let { fileUrl ->
            val fileName = "Filename: " + fileUrl.substring(fileUrl.lastIndexOf("/") + 1, fileUrl.length)
            itemView.tv_item_file_name.text = fileName
        }
        itemView.tv_item_subject.text = level
        itemView.tv_item_name.text = sender
        itemView.tv_item_date.text = date
        itemView.btn_download.setOnClickListener {
            itemCallback?.onClick(
                Triple(
                    ViewFilesAction.DOWNLOAD,
                    item?.id,
                    ServerPathUtil.setCorrectPath(item?.fileUrl)
                )
            )
        }
        itemView.btn_delete.setOnClickListener {
            itemCallback?.onClick(Triple(ViewFilesAction.DELETE, item?.id, item?.fileUrl))
        }
    }
}

class UploadFileDiff : DiffUtil.ItemCallback<IFileModel>() {
    override fun areItemsTheSame(oldItem: IFileModel, newItem: IFileModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IFileModel, newItem: IFileModel): Boolean {
        return oldItem.id == newItem.id
    }
}