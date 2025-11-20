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

import androidx.viewbinding.ViewBinding

/** Base view holder class for all views with view binding in recycler view adapter. */
abstract class BaseViewBindingHolder<VB, T>(protected val binding: VB) :
    BaseViewHolder<T>(binding.root) where VB : ViewBinding {

    /** Overrides bind fun without view binding to call one with view binding. */
    override fun bind(viewModel: T) {
        binding.bind(viewModel)
    }

    /** Binds view binding with view model. */
    abstract fun VB.bind(viewModel: T)
}
