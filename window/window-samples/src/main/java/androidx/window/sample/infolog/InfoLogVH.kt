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

package androidx.window.sample.infolog

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.window.sample.R

class InfoLogVH(view: View) : RecyclerView.ViewHolder(view) {
    val titleView: TextView = view.findViewById(R.id.title_view)
    val detailView: TextView = view.findViewById(R.id.detail_view)
}