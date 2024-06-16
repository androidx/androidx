/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LayoutReuseTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun reuseBox() {
        assertNoRemeasureOnReuse { modifier -> Box(modifier.size(10.dp)) }
    }

    @Test
    fun reuseBoxWithNonDefaultAlignment() {
        assertNoRemeasureOnReuse { modifier ->
            Box(modifier.size(10.dp), contentAlignment = Alignment.Center) {}
        }
    }

    @Test
    fun reuseRow() {
        assertNoRemeasureOnReuse { modifier -> Row(modifier.size(10.dp)) {} }
    }

    @Test
    fun reuseRowWithNonDefaultAlignment() {
        assertNoRemeasureOnReuse { modifier ->
            Row(modifier.size(10.dp), verticalAlignment = Alignment.CenterVertically) {}
        }
    }

    @Test
    fun reuseColumn() {
        assertNoRemeasureOnReuse { modifier -> Column(modifier.size(10.dp)) {} }
    }

    @Test
    fun reuseColumnWithNonDefaultAlignment() {
        assertNoRemeasureOnReuse { modifier ->
            Column(modifier.size(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {}
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun reuseFlowRow() {
        assertNoRemeasureOnReuse { modifier -> FlowRow(modifier.size(10.dp)) {} }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun reuseFlowColumn() {
        assertNoRemeasureOnReuse { modifier -> FlowColumn(modifier.size(10.dp)) {} }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun reuseSpacer() {
        assertNoRemeasureOnReuse { modifier -> Spacer(modifier.size(10.dp)) }
    }

    private fun assertNoRemeasureOnReuse(content: @Composable (Modifier) -> Unit) {
        var measureCount = 0
        val layoutModifier =
            Modifier.layout { measurable, constraints ->
                measureCount++
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        var key by mutableStateOf(0)
        rule.setContent { ReusableContent(key = key) { content(layoutModifier) } }
        rule.runOnIdle {
            measureCount = 0
            key++
        }

        rule.runOnIdle { assertThat(measureCount).isEqualTo(0) }
    }
}
