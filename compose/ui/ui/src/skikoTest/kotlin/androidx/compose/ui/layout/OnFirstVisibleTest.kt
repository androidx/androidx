/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class OnFirstVisibleTest {

    @Test
    fun callOnFirstVisible() = runSkikoComposeUiTest(Size(100f, 200f)) {
        class ItemState(
            var value: Int = 0
        )
        val data = List(50) { ItemState(0) }
        lateinit var lazyListState: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            lazyListState = rememberLazyListState()
            scope = rememberCoroutineScope()
            LazyColumn(Modifier.fillMaxSize(), lazyListState) {
                items(items = data, key = { it }) {
                    Box(Modifier.size(100.dp).onFirstVisible {
                        it.value++
                    })
                }
            }
        }

        scope.launch {
            repeat(50) {
                lazyListState.animateScrollToItem(it)
            }
        }

        waitForIdle()
        assertThat(data.map { it.value }).isEqualTo(List(50) { 1 })
    }
}
