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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.lazy.scrollTransform
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class ScrollTransformTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun itemPlacementWithReducedMotion() {
        lateinit var state: TransformingLazyColumnState

        setContentWithTheme {
            state = rememberTransformingLazyColumnState()
            CompositionLocalProvider(LocalReduceMotion provides true) {
                TransformingLazyColumn(
                    state = state,
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(space = 4.dp, alignment = Alignment.Top),
                ) {
                    items(count = 20) {
                        // Should fit exactly 10 of these (16.dp item size + 4.dp of spacing) if the
                        // scrollTransform is turned off.
                        Box(modifier = Modifier.size(16.dp).scrollTransform(this))
                    }
                }
            }
        }

        rule.waitForIdle()

        assertThat(state.layoutInfo.visibleItems.size).isEqualTo(10)
    }

    private fun setContentWithTheme(composable: @Composable BoxScope.() -> Unit) {
        // Use constant size modifier to limit relative color percentage ranges.
        rule.setContentWithTheme(modifier = Modifier.size(COMPONENT_SIZE)) {
            ScreenConfiguration(SCREEN_SIZE_LARGE) { composable() }
        }
    }
}

private val COMPONENT_SIZE = 200.dp
