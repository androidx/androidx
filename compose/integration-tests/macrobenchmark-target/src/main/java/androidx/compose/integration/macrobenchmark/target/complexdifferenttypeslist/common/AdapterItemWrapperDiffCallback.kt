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

import androidx.recyclerview.widget.DiffUtil

/** Diff callback for [AdapterItemWrapper]. */
class AdapterItemWrapperDiffCallback : DiffUtil.ItemCallback<AdapterItemWrapper>() {

    /** Validates that items are the same base on item hash code. */
    override fun areItemsTheSame(
        oldItem: AdapterItemWrapper,
        newItem: AdapterItemWrapper,
    ): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    /** Validates that items contents the same base on item id, type and data. */
    override fun areContentsTheSame(
        oldItem: AdapterItemWrapper,
        newItem: AdapterItemWrapper,
    ): Boolean {
        return oldItem == newItem
    }
}
