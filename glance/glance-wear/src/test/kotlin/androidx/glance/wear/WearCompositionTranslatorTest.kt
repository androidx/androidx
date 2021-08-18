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

package androidx.glance.wear

import androidx.compose.runtime.Composable
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.unit.dp
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_TOP
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

@OptIn(GlanceInternalApi::class, ExperimentalCoroutinesApi::class)
class WearCompositionTranslatorTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun canTranslateBox() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box {}
        }

        // runAndTranslate wraps the result in a Box...ensure that the layout generated two Boxes
        val outerBox = content as LayoutElementBuilders.Box
        assertThat(outerBox.contents).hasSize(1)

        assertThat(outerBox.contents[0]).isInstanceOf(LayoutElementBuilders.Box::class.java)
    }

    @Test
    fun canTranslateAlignment() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box(contentAlignment = Alignment.Center) {}
        }

        val innerBox =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box

        assertThat(innerBox.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_CENTER)
        assertThat(innerBox.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)
    }

    @Test
    fun canTranslateBoxWithChildren() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box {
                Box(contentAlignment = Alignment.TopCenter) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }

        val innerBox =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box
        val leaf0 = innerBox.contents[0] as LayoutElementBuilders.Box
        val leaf1 = innerBox.contents[1] as LayoutElementBuilders.Box

        assertThat(leaf0.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_TOP)
        assertThat(leaf0.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)

        assertThat(leaf1.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_BOTTOM)
        assertThat(leaf1.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_END)
    }

    @Test
    fun canTranslatePaddingModifier() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box(modifier = Modifier.padding(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp)) {}
        }

        val innerBox =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box
        val padding = requireNotNull(innerBox.modifiers!!.padding)

        assertThat(padding.start!!.value).isEqualTo(1f)
        assertThat(padding.top!!.value).isEqualTo(2f)
        assertThat(padding.end!!.value).isEqualTo(3f)
        assertThat(padding.bottom!!.value).isEqualTo(4f)
    }

    private suspend fun runAndTranslate(
        content: @Composable () -> Unit
    ): LayoutElementBuilders.LayoutElement {
        val root = runTestingComposition(content)

        return translateComposition(root)
    }
}