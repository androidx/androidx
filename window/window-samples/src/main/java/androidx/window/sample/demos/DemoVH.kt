/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.sample.demos

import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.window.sample.R

class DemoVH(view: View) : RecyclerView.ViewHolder(view) {
    private val description = view.findViewById<TextView>(R.id.demo_description)
    private val launchButton = view.findViewById<Button>(R.id.start_demo_button)

    fun bind(item: DemoItem) {
        description.text = item.description
        launchButton.text = item.buttonTitle
        launchButton.setOnClickListener { view ->
            val context = view.context
            val intent = Intent(context, item.clazz)
            view.context.startActivity(intent)
        }
    }
}