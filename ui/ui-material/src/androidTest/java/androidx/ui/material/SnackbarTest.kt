/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import android.os.Build
import androidx.compose.Providers
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.text.FirstBaseline
import androidx.ui.text.LastBaseline
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInParent
import androidx.ui.core.semantics.semantics
import androidx.ui.core.testTag
import androidx.ui.foundation.Text
import androidx.ui.foundation.shape.corner.CutCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.compositeOver
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Stack
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@MediumTest
@RunWith(JUnit4::class)
class SnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val longText = "Message is very long and long and long and long and long " +
            "and long and long and long and long and long and long"

    @Test
    fun defaultSnackbar_semantics() {
        var clicked = false
        composeTestRule.setMaterialContent {
            Stack {
                Snackbar(text = { Text("Message") }, action = {
                    TextButton(onClick = { clicked = true }) {
                        Text("UNDO")
                    }
                })
            }
        }

        findByText("Message")
            .assertExists()

        assertThat(clicked).isFalse()

        findByText("UNDO")
            .doClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun snackbar_shortTextOnly_sizes() {
        var textCoords: LayoutCoordinates? = null
        val sizes = composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    Text("Message",
                        Modifier.onPositioned { textCoords = it }
                    )
                }
            )
        }
        sizes
            .assertHeightEqualsTo(48.dp)
            .assertWidthEqualsTo(300.dp)
        assertThat(textCoords).isNotNull()
        textCoords?.let {
            with(composeTestRule.density) {
                assertThat(it[FirstBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(it[FirstBaseline])
                    .isEqualTo(it[LastBaseline])
                // TODO(aelias): Remove 'parentCoordinates!!' when Semantics no longer using PassThroughLayout
                assertThat(it.parentCoordinates!!.positionInParent.y.roundToInt().ipx +
                        it[FirstBaseline]!!)
                    .isEqualTo(30.dp.toIntPx())
            }
        }
    }

    @Test
    fun snackbar_shortTextAndButton_alignment() {
        var snackCoords: LayoutCoordinates? = null
        var textCoords: LayoutCoordinates? = null
        var buttonCoords: LayoutCoordinates? = null
        var buttonTextCoords: LayoutCoordinates? = null
        val sizes = composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                modifier = Modifier.onPositioned { snackCoords = it },
                text = {
                    Text("Message",
                        Modifier.onPositioned { textCoords = it }
                    )
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.onPositioned { buttonCoords = it }
                    ) {
                        Text("Undo",
                            Modifier.onPositioned { buttonTextCoords = it }
                        )
                    }
                }
            )
        }
        sizes
            .assertHeightEqualsTo(48.dp)
            .assertWidthEqualsTo(300.dp)
        assertThat(textCoords).isNotNull()
        assertThat(buttonCoords).isNotNull()
        assertThat(buttonTextCoords).isNotNull()
        assertThat(snackCoords).isNotNull()
        val localTextCoords = textCoords
        val localButtonCoords = buttonCoords
        val localButtonTextCoords = buttonTextCoords
        val localSnackCoords = snackCoords

        if (localTextCoords != null &&
            localButtonCoords != null &&
            localButtonTextCoords != null &&
            localSnackCoords != null
        ) {
            with(composeTestRule.density) {
                val buttonTextPos =
                    localSnackCoords.childToLocal(localButtonTextCoords, PxPosition.Origin)
                assertThat(localTextCoords[FirstBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(localButtonTextCoords[FirstBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(
                    localTextCoords.globalPosition.y.roundToInt().ipx +
                            localTextCoords[FirstBaseline]!!
                ).isEqualTo(30.dp.toIntPx())
                assertThat(
                    buttonTextPos.y.roundToInt().ipx + localButtonTextCoords[FirstBaseline]!!
                ).isEqualTo(30.dp.toIntPx())
            }
        }
    }

    @Test
    fun snackbar_longText_sizes() {
        var textCoords: LayoutCoordinates? = null
        val sizes = composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    Text(longText,
                        Modifier.onPositioned { textCoords = it }, maxLines = 2)
                }
            )
        }
        sizes
            .assertHeightEqualsTo(68.dp)
            .assertWidthEqualsTo(300.dp)
        assertThat(textCoords).isNotNull()
        textCoords?.let {
            with(composeTestRule.density) {
                assertThat(it[FirstBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(it[LastBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(it[FirstBaseline]).isNotEqualTo(it[LastBaseline])
                // TODO(aelias): Remove 'parentCoordinates!!' when Semantics no longer using PassThroughLayout
                assertThat(
                    it.parentCoordinates!!.positionInParent.y.roundToInt().ipx +
                            it[FirstBaseline]!!
                ).isEqualTo(30.dp.toIntPx())
            }
        }
    }

    @Test
    fun snackbar_longTextAndButton_alignment() {
        var snackCoords: LayoutCoordinates? = null
        var textCoords: LayoutCoordinates? = null
        var buttonCoords: LayoutCoordinates? = null
        val sizes = composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                modifier = Modifier.onPositioned { snackCoords = it },
                text = {
                    Text(longText,
                        Modifier.onPositioned { textCoords = it }, maxLines = 2)
                },
                action = {
                    TextButton(
                        modifier = Modifier.onPositioned { buttonCoords = it },
                        onClick = {}
                    ) {
                        Text("Undo")
                    }
                }
            )
        }
        sizes
            .assertHeightEqualsTo(68.dp)
            .assertWidthEqualsTo(300.dp)
        assertThat(textCoords).isNotNull()
        assertThat(buttonCoords).isNotNull()
        assertThat(snackCoords).isNotNull()
        val localTextCoords = textCoords
        val localButtonCoords = buttonCoords
        val localSnackCoords = snackCoords

        if (localTextCoords != null && localButtonCoords != null && localSnackCoords != null) {
            with(composeTestRule.density) {
                val buttonPositionInSnack =
                    localSnackCoords.childToLocal(localButtonCoords, PxPosition.Origin)
                val buttonCenter =
                    buttonPositionInSnack.y.roundToInt() +
                            (localButtonCoords.size.height / 2).value.toFloat()

                assertThat(localTextCoords[FirstBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(localTextCoords[LastBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(localTextCoords[FirstBaseline])
                    .isNotEqualTo(localTextCoords[LastBaseline])
                assertThat(
                    localTextCoords.globalPosition.y.roundToInt().ipx +
                            localTextCoords[FirstBaseline]!!
                ).isEqualTo(30.dp.toIntPx())

                assertThat(buttonCenter).isEqualTo(
                    (localSnackCoords.size.height / 2).value.toFloat()
                )
            }
        }
    }

    @Test
    fun snackbar_textAndButtonOnSeparateLine_alignment() {
        var snackCoords: LayoutCoordinates? = null
        var textCoords: LayoutCoordinates? = null
        var buttonCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                modifier = Modifier.onPositioned { snackCoords = it },
                text = {
                    Text("Message",
                        Modifier.onPositioned { textCoords = it }
                    )
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.onPositioned { buttonCoords = it }
                    ) {
                        Text("Undo")
                    }
                },
                actionOnNewLine = true
            )
        }
        assertThat(textCoords).isNotNull()
        assertThat(buttonCoords).isNotNull()
        assertThat(snackCoords).isNotNull()
        val localTextCoords = textCoords
        val localButtonCoords = buttonCoords
        val localSnackCoords = snackCoords

        if (localTextCoords != null && localButtonCoords != null && localSnackCoords != null) {
            with(composeTestRule.density) {
                val buttonPositionInSnack =
                    localSnackCoords.childToLocal(localButtonCoords, PxPosition.Origin)
                val textPositionInSnack =
                    localSnackCoords.childToLocal(localTextCoords, PxPosition.Origin)

                assertThat(
                    textPositionInSnack.y.roundToInt().ipx + localTextCoords[FirstBaseline]!!
                ).isEqualTo(30.dp.toIntPx())

                assertThat(
                    buttonPositionInSnack.y.roundToInt().ipx -
                            textPositionInSnack.y.roundToInt().ipx -
                            localTextCoords[LastBaseline]!!
                ).isEqualTo(18.dp.toIntPx())

                assertThat(
                    localSnackCoords.size.height - buttonPositionInSnack.y.roundToInt().ipx -
                            localButtonCoords.size.height
                ).isEqualTo(8.dp.toIntPx())

                assertThat(
                    localSnackCoords.size.width - buttonPositionInSnack.x.roundToInt().ipx -
                            localButtonCoords.size.width
                ).isEqualTo(8.dp.toIntPx())
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shapeAndColorFromThemeIsUsed() {
        val shape = CutCornerShape(8.dp)
        var background = Color.Yellow
        var snackBarColor = Color.Transparent
        composeTestRule.setMaterialContent {
            Stack {
                background = MaterialTheme.colors.surface
                // Snackbar has a background color of onSurface with an alpha applied blended
                // on top of surface
                snackBarColor = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    .compositeOver(background)
                Providers(ShapesAmbient provides Shapes(medium = shape)) {
                    Snackbar(modifier = Modifier
                        .semantics(mergeAllDescendants = true)
                        .testTag("snackbar"),
                        text = { Text("") }
                    )
                }
            }
        }

        findByTag("snackbar")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = shape,
                shapeColor = snackBarColor,
                backgroundColor = background,
                shapeOverlapPixelCount = with(composeTestRule.density) { 2.dp.toPx() }
            )
    }
}