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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.window.sample.R

class DemoAdapter(private val demoItems: List<DemoItem>) : RecyclerView.Adapter<DemoVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoVH {
        val inflater = LayoutInflater.from(parent.context)
        val root = inflater.inflate(R.layout.view_holder_demo_item, parent, false)
        return DemoVH(root)
    }

    override fun onBindViewHolder(holder: DemoVH, position: Int) {
        holder.bind(demoItems[position])
    }

    override fun getItemCount(): Int {
        return demoItems.size
    }
}