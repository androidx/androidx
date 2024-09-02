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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyLayoutAnimateScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListAnimateScrollScope(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    @Test
    fun animateToItem_stickyHeader_shouldNotConsiderItemFound() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState(initialFirstVisibleItemIndex = 3)
            LazyColumnOrRow(Modifier.crossAxisSize(150.dp).mainAxisSize(100.dp), state) {
                stickyHeader { Box(Modifier.size(150.dp)) }
                items(20) { Box(Modifier.size(150.dp)) }
            }
        }

        val animatedScrollScope = LazyLayoutAnimateScrollScope(state)
        /**
         * Sticky item is considered non visible whilst sticking, distance should be best effort,
         * average size * (target pos - current pos)
         */
        assertThat(animatedScrollScope.calculateDistanceTo(0))
            .isEqualTo(-3 * with(rule.density) { 150.dp.roundToPx() })
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
