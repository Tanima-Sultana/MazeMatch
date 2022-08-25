package com.example.imageapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imageapplication.R
import com.example.imageapplication.data.Upload
import com.squareup.picasso.Picasso

class ImageAdapter(private var imageList:List<Upload>): RecyclerView.Adapter<ImageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_images, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
       return imageList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.iv_uploaded_Image)
        val textView: TextView = itemView.findViewById(R.id.tv_imageName)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Picasso.get().load(imageList[position].fileUrl).centerCrop().fit()
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(holder.imageView)
        holder.textView.text = imageList[position].fileName
    }

}