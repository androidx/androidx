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
        assertThat(root.children[0]).isInstanceOf(EmittableColumn::class.java)
        assertThat((root.children[0] as EmittableColumn).children).hasSize(0)
    }

    @Test
    fun createComposableRowWithParams() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Column(
                modifier = Modifier.padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {}
        }

        val innerColumn = root.children[0] as EmittableColumn
        val paddingModifier = requireNotNull(innerColumn.modifier.findModifier<PaddingModifier>())
        assertThat(paddingModifier.top).isEqualTo(2.dp)
        assertThat(innerColumn.horizontalAlignment).isEqualTo(Alignment.CenterHorizontally)
        assertThat(innerColumn.verticalAlignment).isEqualTo(Alignment.CenterVertically)
    }

    @Test
    fun createComposableRowWithChildren() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Column {
                Box(contentAlignment = Alignment.BottomCenter) {}
                Box(contentAlignment = Alignment.TopCenter) {}
            }
        }

        val innerColumn = root.children[0] as EmittableColumn
        val leafBox0 = innerColumn.children[0] as EmittableBox
        val leafBox1 = innerColumn.children[1] as EmittableBox

        assertThat(leafBox0.contentAlignment).isEqualTo(Alignment.BottomCenter)
        assertThat(leafBox1.contentAlignment).isEqualTo(Alignment.TopCenter)
    }
}