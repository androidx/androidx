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

import androidx.glance.Modifier
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
class RowTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun createComposableRow() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Row {}
        }

        assertThat(root.children).hasSize(1)
        assertThat(root.children[0]).isInstanceOf(EmittableRow::class.java)
        assertThat((root.children[0] as EmittableRow).children).hasSize(0)
    }

    @Test
    fun createComposableRowWithParams() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalAlignment = Alignment.End
            ) {}
        }

        val innerRow = root.children[0] as EmittableRow
        val paddingModifier = requireNotNull(innerRow.modifier.findModifier<PaddingModifier>())
        assertThat(paddingModifier.top).isEqualTo(2.dp)
        assertThat(innerRow.verticalAlignment).isEqualTo(Alignment.Bottom)
        assertThat(innerRow.horizontalAlignment).isEqualTo(Alignment.End)
    }

    @Test
    fun createComposableRowWithChildren() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Row {
                Box(contentAlignment = Alignment.BottomCenter) {}
                Box(contentAlignment = Alignment.TopCenter) {}
            }
        }

        val innerRow = root.children[0] as EmittableRow
        val leafBox0 = innerRow.children[0] as EmittableBox
        val leafBox1 = innerRow.children[1] as EmittableBox

        assertThat(leafBox0.contentAlignment).isEqualTo(Alignment.BottomCenter)
        assertThat(leafBox1.contentAlignment).isEqualTo(Alignment.TopCenter)
    }

    @Test
    fun createComposableRowWithWeightChildren() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Row {
                Box(modifier = Modifier.defaultWeight()) { }
            }
        }

        val row = assertIs<EmittableRow>(root.children[0])
        val box = assertIs<EmittableBox>(row.children[0])

        val widthModifier = checkNotNull(box.modifier.findModifier<WidthModifier>())
        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Expand)
        assertThat(box.modifier.findModifier<HeightModifier>()).isNull()
    }
}