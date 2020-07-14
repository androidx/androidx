/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.fragment.testapp.kittenfragmenttransitions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.testapp.R
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapts Views containing kittens to RecyclerView cells
 */
class KittenGridAdapter(
    private val listener: (holder: KittenViewHolder, position: Int) -> Unit,
    private val size: Int = 6
) : RecyclerView.Adapter<KittenViewHolder>() {

    override fun onCreateViewHolder(container: ViewGroup, position: Int): KittenViewHolder {
        val inflater = LayoutInflater.from(container.context)
        val cell = inflater.inflate(R.layout.kitten_grid_item, container, false)
        return KittenViewHolder(cell)
    }

    override fun onBindViewHolder(viewHolder: KittenViewHolder, position: Int) {
        when (position % 6) {
            0 -> viewHolder.image.setImageResource(R.drawable.placekitten_1)
            1 -> viewHolder.image.setImageResource(R.drawable.placekitten_2)
            2 -> viewHolder.image.setImageResource(R.drawable.placekitten_3)
            3 -> viewHolder.image.setImageResource(R.drawable.placekitten_4)
            4 -> viewHolder.image.setImageResource(R.drawable.placekitten_5)
            5 -> viewHolder.image.setImageResource(R.drawable.placekitten_6)
        }
        // It is important that each shared element in the source screen has a unique transition
        // name. For example, we can't just give all the images in our grid the transition name
        // "kittenImage" because then we would have conflicting transition names. By appending
        // "_image" to the position, we can support having multiple shared elements in each grid
        // cell in the future.
        ViewCompat.setTransitionName(viewHolder.image, position.toString() + "_image")
        viewHolder.image.setOnClickListener { listener(viewHolder, position) }
    }

    override fun getItemCount() = size
}
