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

package androidx.glance.wear.tiles.curved

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import androidx.glance.wear.tiles.runTestingComposition
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CurvedRowTest {
    private lateinit var fakeCoroutineScope: TestScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    fun equality() {
        assertThat(
            CurvedTextStyle(
                color = ColorProvider(Color.Magenta),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic
            )
        ).isEqualTo(
            CurvedTextStyle(
                color = ColorProvider(Color.Magenta),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic
            )
        )

        assertThat(CurvedTextStyle(color = ColorProvider(Color.Magenta)))
            .isNotEqualTo(CurvedTextStyle(color = ColorProvider(Color.Red)))
    }

    @Test
    fun createComposableArc() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            CurvedRow(
                modifier = GlanceModifier.padding(1.dp),
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
    fun createComposableArcText() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            CurvedRow {
                CurvedText(
                    text = "Hello World",
                    modifier = GlanceModifier.padding(5.dp),
                    textStyle = CurvedTextStyle(color = ColorProvider(Color.Gray), fontSize = 24.sp)
                )
            }
        }

        val arc = assertIs<EmittableCurvedRow>(root.children[0])
        val arcText = assertIs<EmittableCurvedText>(arc.children[0])

        assertThat(arcText.text).isEqualTo("Hello World")
        assertThat(arcText.modifier.findModifier<PaddingModifier>()).isNotNull()
        assertThat(arcText.textStyle).isNotNull()
        assertThat(arcText.textStyle!!.fontSize).isEqualTo(24.sp)
        assertThat(arcText.textStyle!!.color).isEqualTo(ColorProvider(Color.Gray))
    }
}
