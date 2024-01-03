/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.slidingpanelayout

import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.demo.R

class ItemViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

    private val titleTextView = rootView.findViewById<TextView>(R.id.title_textview)
    private val descriptionTextView = rootView.findViewById<TextView>(R.id.description_textview)
    private val launchButton = rootView.findViewById<Button>(R.id.button_launch)

    fun bind(item: DemoItem<*>) {
        titleTextView.text = item.title
        descriptionTextView.text = item.description
        launchButton.setOnClickListener { view ->
            val context = view.context
            val intent = Intent(context, item.clazz)
            context.startActivity(intent)
        }
    }
}
