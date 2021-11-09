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

import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BoxTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun createComposableBox() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Box {}
        }

        // Outer box (added by runTestingComposition) should have a single child box.
        assertThat(root.children).hasSize(1)
        val child = assertIs<EmittableBox>(root.children[0])

        // The Box added above should not have any other children.
        assertThat(child.children).hasSize(0)
    }

    @Test
    fun createComposableBoxWithModifier() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Box(modifier = GlanceModifier.padding(1.dp)) {}
        }

        val innerBox = assertIs<EmittableBox>(root.children[0])
        val paddingModifier = requireNotNull(innerBox.modifier.findModifier<PaddingModifier>())

        // Don't need to test all elements, that's covered in PaddingTest
        assertThat(paddingModifier.top).isEqualTo(PaddingDimension(1.dp))
    }

    @Test
    fun createComposableBoxWithAlignment() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Box(contentAlignment = Alignment.Center) {}
        }

        val innerBox = assertIs<EmittableBox>(root.children[0])

        assertThat(innerBox.contentAlignment).isEqualTo(Alignment.Center)
    }

    @Test
    fun createComposableBoxWithChildren() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Box(contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.BottomCenter) {}
                Box(contentAlignment = Alignment.TopCenter) {}
            }
        }

        val innerBox = assertIs<EmittableBox>(root.children[0])

        assertThat(innerBox.children).hasSize(2)

        val leafBox0 = assertIs<EmittableBox>(innerBox.children[0])
        val leafBox1 = assertIs<EmittableBox>(innerBox.children[1])

        assertThat(leafBox0.contentAlignment).isEqualTo(Alignment.BottomCenter)
        assertThat(leafBox1.contentAlignment).isEqualTo(Alignment.TopCenter)
    }
}