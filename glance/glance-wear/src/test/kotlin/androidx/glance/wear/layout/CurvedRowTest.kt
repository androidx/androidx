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

package androidx.glance.wear.layout

import androidx.glance.Modifier
import androidx.glance.findModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.padding
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import androidx.glance.wear.runTestingComposition
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CurvedRowTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun createComposableArc() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            CurvedRow(
                modifier = Modifier.padding(1.dp),
                anchorDegrees = 5f,
                anchorType = AnchorType.End,
                radialAlignment = RadialAlignment.Center
            ) {}
        }

        assertThat(root.children).hasSize(1)

        val arc = assertIs<EmittableCurvedRow>(root.children[0])
        assertThat(arc.children).hasSize(0)
        assertThat(arc.anchorDegrees).isEqualTo(5f)
        assertThat(arc.anchorType).isEqualTo(AnchorType.End)
        assertThat(arc.radialAlignment).isEqualTo(RadialAlignment.Center)
        assertThat(arc.modifier.findModifier<PaddingModifier>()).isNotNull()
    }

    @Test
    fun createComposableArcText() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            CurvedRow {
                CurvedText(
                    text = "Hello World",
                    modifier = Modifier.padding(5.dp),
                    textStyle = CurvedTextStyle(fontSize = 24.sp)
                )
            }
        }

        val arc = assertIs<EmittableCurvedRow>(root.children[0])
        val arcText = assertIs<EmittableCurvedText>(arc.children[0])

        assertThat(arcText.text).isEqualTo("Hello World")
        assertThat(arcText.modifier.findModifier<PaddingModifier>()).isNotNull()
        assertThat(arcText.textStyle).isNotNull()
        assertThat(arcText.textStyle!!.fontSize).isEqualTo(24.sp)
    }
}
