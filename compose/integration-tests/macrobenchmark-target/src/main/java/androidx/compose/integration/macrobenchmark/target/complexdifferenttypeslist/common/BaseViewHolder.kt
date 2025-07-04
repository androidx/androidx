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

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

/** Base view holder class for all views in adapter. */
abstract class BaseViewHolder<T>(private val containerView: View) :
    RecyclerView.ViewHolder(containerView) {

    constructor(
        parent: ViewGroup,
        @LayoutRes layoutId: Int,
        attachToRoot: Boolean = false,
    ) : this(LayoutInflater.from(parent.context).inflate(layoutId, parent, attachToRoot))

    val context: Context = itemView.context

    /** Binds view model to a view. */
    abstract fun bind(viewModel: T)

    /** Called when view is attached to the window. */
    open fun onViewAttached(saveStateBundle: Bundle) {}

    /** Called when view is detached from the window. */
    open fun onViewDetached(saveStateBundle: Bundle) {}
}
