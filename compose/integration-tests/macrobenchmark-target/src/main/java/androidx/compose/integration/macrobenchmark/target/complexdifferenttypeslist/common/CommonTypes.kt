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

import android.view.ViewGroup
import androidx.compose.integration.macrobenchmark.target.R
import androidx.compose.runtime.Composable

/** Enumeration for common view types in a list. */
enum class CommonAdapterItemType(
    val layoutResId: Int,
    val composableLayout: @Composable () -> Unit,
) : BaseAdapterItemType {
    SPACE_12(R.layout.item_space_12, { CommonSpace12() }),
    SPACE_24(R.layout.item_space_24, { CommonSpace24() }),
    SPACE_30(R.layout.item_space_30, { CommonSpace30() }),
}

/** Simple view holder for common type items that does nothing on bind. */
class CommonViewHolder(parent: ViewGroup, type: CommonAdapterItemType) :
    BaseViewHolder<Any>(parent, type.layoutResId) {
    override fun bind(viewModel: Any) {}
}
