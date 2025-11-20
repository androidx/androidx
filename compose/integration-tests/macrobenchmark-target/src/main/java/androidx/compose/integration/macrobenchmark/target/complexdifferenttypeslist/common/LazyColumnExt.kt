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

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable

fun <T> LazyListScope.populate(
    listData: List<AdapterItemWrapper>,
    factory: @Composable LazyItemScope.(viewType: T, data: Any?, id: Any?) -> Unit,
) where T : BaseAdapterItemType, T : Enum<T> {
    items(
        items = listData,
        key = { it.id!! },
        contentType = { it.type },
        itemContent = { item ->
            @Suppress("UNCHECKED_CAST")
            when (item.type) {
                is CommonAdapterItemType -> item.type.composableLayout.invoke()
                else -> factory(item.type as T, item.data, item.id)
            }
        },
    )
}
