/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.testapp.ui.v2

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.pdf.testapp.R
import androidx.recyclerview.widget.RecyclerView

class ThumbnailAdapter(private val onThumbnailClicked: (Int) -> Unit) :
    RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder>() {

    private var thumbnails: List<Bitmap> = emptyList()
    private var selectedPage: Int = 0

    fun submitList(newThumbnails: List<Bitmap>) {
        thumbnails = newThumbnails
        notifyDataSetChanged()
    }

    fun updateSelectedPage(page: Int) {
        if (selectedPage == page) return
        val oldSelected = selectedPage
        selectedPage = page

        // Only update visible items to avoid unnecessary redraws and flicker
        if (oldSelected in 0 until itemCount) notifyItemChanged(oldSelected)
        if (selectedPage in 0 until itemCount) notifyItemChanged(selectedPage)
    }

    fun clearThumbnails() {
        thumbnails = emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.thumbnail_item, parent, false)
        return ThumbnailViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(thumbnails[position], position, position == selectedPage)
    }

    override fun getItemCount(): Int = thumbnails.size

    inner class ThumbnailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.thumbnail_image)

        fun bind(bitmap: Bitmap, pageIndex: Int, isSelected: Boolean) {
            itemView.foreground = null
            imageView.setImageBitmap(bitmap)
            itemView.contentDescription =
                itemView.context.getString(
                    R.string.thumbnail_content_description_page_n,
                    pageIndex + 1,
                )
            itemView.setOnClickListener { onThumbnailClicked(pageIndex) }

            // Highlight selected thumbnail using foreground
            itemView.foreground =
                if (isSelected) {
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.thumbnail_highlight_border,
                    )
                } else {
                    null
                }
        }
    }
}
