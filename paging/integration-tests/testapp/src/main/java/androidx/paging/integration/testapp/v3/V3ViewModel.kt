/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging.integration.testapp.v3

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.paging.Config
import androidx.paging.PagedData
import androidx.paging.PagedDataFlowBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class V3ViewModel : ViewModel() {
    fun flow(): Flow<PagedData<Item>> =
        PagedDataFlowBuilder(ItemPagedSource.Factory, Config(pageSize = 10))
            .build()
            .map {
                it.insertSeparators { before: Item?, after: Item? ->
                    if (after == null || (after.id / 3) == (before?.id ?: 0) / 3) {
                        // no separator, because at bottom or not needed yet
                        null
                    } else {
                        Item(
                            id = -1,
                            text = "DIVIDER" + after.id / 3,
                            bgColor = Color.DKGRAY
                        )
                    }
                }.insertSeparators { before: Item?, _: Item? ->
                    if (before != null && before.id == -1) {
                        Item(
                            id = -2,
                            text = "RIGHT BELOW DIVIDER",
                            bgColor = Color.BLACK
                        )
                    } else null
                }
            }
    // TODO: buffer!
}
