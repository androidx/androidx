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

package androidx.glance.layout

import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.unit.Dimension
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpacerTest {
    private lateinit var fakeCoroutineScope: TestScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    fun createSpacerWithWidth() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Spacer(GlanceModifier.width(10.dp))
        }

        assertThat(root.children).hasSize(1)
        assertThat(root.children[0]).isInstanceOf(EmittableSpacer::class.java)

        val spacer = root.children[0] as EmittableSpacer
        val widthModifier = checkNotNull(spacer.modifier.findModifier<WidthModifier>())
        val width = assertIs<Dimension.Dp>(widthModifier.width)
        assertThat(width.dp).isEqualTo(10.dp)
    }

    @Test
    fun createSpacerWithHeight() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Spacer(GlanceModifier.height(10.dp))
        }

        assertThat(root.children).hasSize(1)
        assertThat(root.children[0]).isInstanceOf(EmittableSpacer::class.java)

        val spacer = root.children[0] as EmittableSpacer
        val heightModifier = checkNotNull(spacer.modifier.findModifier<HeightModifier>())
        val height = assertIs<Dimension.Dp>(heightModifier.height)
        assertThat(height.dp).isEqualTo(10.dp)
    }

    @Test
    fun createSpacerWithSize() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Spacer(GlanceModifier.size(10.dp, 15.dp))
        }

        assertThat(root.children).hasSize(1)
        assertThat(root.children[0]).isInstanceOf(EmittableSpacer::class.java)

        val spacer = root.children[0] as EmittableSpacer

        val widthModifier = checkNotNull(spacer.modifier.findModifier<WidthModifier>())
        val width = assertIs<Dimension.Dp>(widthModifier.width)
        assertThat(width.dp).isEqualTo(10.dp)

        val heightModifier = checkNotNull(spacer.modifier.findModifier<HeightModifier>())
        val height = assertIs<Dimension.Dp>(heightModifier.height)
        assertThat(height.dp).isEqualTo(15.dp)
    }
}
