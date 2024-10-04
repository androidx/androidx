/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.hero.jetsnack.implementation.views

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.integration.hero.jetsnack.implementation.R
import androidx.compose.integration.hero.jetsnack.implementation.Snack
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

val gradients: List<Color> =
    listOf(
        Color(0xff7057f5),
        Color(0xff86f7fa),
        Color(0xffc8bbfd),
        Color(0xff86f7fa),
        Color(0xff7057f5)
    )

val gradientColors = gradients.map { it.toArgb() }.toIntArray()

class DessertAdapter(private val snacks: List<Snack>) :
    Adapter<DessertAdapter.DessertViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DessertViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val root = inflater.inflate(R.layout.item_snack_card_view, parent, false)
        return DessertViewHolder(root)
    }

    override fun onBindViewHolder(holder: DessertViewHolder, position: Int) {
        holder.bind(snacks[position])
    }

    override fun getItemCount(): Int {
        return snacks.size
    }

    class DessertViewHolder(rootView: View) : ViewHolder(rootView) {
        private val collectionBox = rootView.findViewById<View>(R.id.collectionBox)
        private val snackImageView = rootView.findViewById<ImageView>(R.id.snackImageView)
        private val snackNameTextView = rootView.findViewById<TextView>(R.id.snackNameTextView)
        private val snackTagLineTextView =
            rootView.findViewById<TextView>(R.id.snackTagLineTextView)

        private val drawable =
            GradientDrawable().apply {
                colors = gradientColors
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                gradientType = GradientDrawable.LINEAR_GRADIENT
                shape = GradientDrawable.RECTANGLE
            }

        fun bind(snack: Snack) {
            collectionBox.background = drawable
            snackImageView.setImageResource(snack.imageDrawable)
            snackNameTextView.text = snack.name
            snackTagLineTextView.text = snack.tagline
        }
    }
}

class SnackAdapter(private val snacks: List<Snack>) : Adapter<SnackAdapter.SnackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnackViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val root = inflater.inflate(R.layout.item_snack_view, parent, false)
        return SnackViewHolder(root)
    }

    override fun onBindViewHolder(holder: SnackViewHolder, position: Int) {
        holder.bind(snacks[position])
    }

    override fun getItemCount(): Int {
        return snacks.size
    }

    class SnackViewHolder(rootView: View) : ViewHolder(rootView) {
        private val snackImageView = rootView.findViewById<ImageView>(R.id.snackImageView)
        private val snackNameTextView = rootView.findViewById<TextView>(R.id.snackNameTextView)

        fun bind(snack: Snack) {
            snackImageView.setImageResource(snack.imageDrawable)
            snackNameTextView.text = snack.name
        }
    }
}
