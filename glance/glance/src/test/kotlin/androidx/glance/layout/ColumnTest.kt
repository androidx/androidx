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
import androidx.glance.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ColumnTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun createComposableRow() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Column {}
        }

        assertThat(root.children).hasSize(1)
        val column = assertIs<EmittableColumn>(root.children[0])
        assertThat(column.children).hasSize(0)
    }

    @Test
    fun createComposableRowWithParams() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Column(
                modifier = GlanceModifier.padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {}
        }

        val innerColumn = assertIs<EmittableColumn>(root.children[0])
        val paddingModifier = requireNotNull(innerColumn.modifier.findModifier<PaddingModifier>())
        assertThat(paddingModifier.top).isEqualTo(PaddingDimension(2.dp))
        assertThat(innerColumn.horizontalAlignment).isEqualTo(Alignment.CenterHorizontally)
        assertThat(innerColumn.verticalAlignment).isEqualTo(Alignment.CenterVertically)
    }

    @Test
    fun createComposableColumnWithChildren() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Column {
                Box(contentAlignment = Alignment.BottomCenter) {}
                Box(contentAlignment = Alignment.TopCenter) {}
            }
        }

        val innerColumn = assertIs<EmittableColumn>(root.children[0])
        val leafBox0 = assertIs<EmittableBox>(innerColumn.children[0])
        val leafBox1 = assertIs<EmittableBox>(innerColumn.children[1])

        assertThat(leafBox0.contentAlignment).isEqualTo(Alignment.BottomCenter)
        assertThat(leafBox1.contentAlignment).isEqualTo(Alignment.TopCenter)
    }

    @Test
    fun createComposableColumnWithWeightChildren() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Column {
                Box(modifier = GlanceModifier.defaultWeight()) { }
            }
        }

        val column = assertIs<EmittableColumn>(root.children[0])
        val box = assertIs<EmittableBox>(column.children[0])

        val heightModifier = checkNotNull(box.modifier.findModifier<HeightModifier>())
        assertThat(heightModifier.height).isSameInstanceAs(Dimension.Expand)
        assertThat(box.modifier.findModifier<WidthModifier>()).isNull()
    }
}