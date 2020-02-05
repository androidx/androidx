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

import androidx.test.filters.MediumTest
import androidx.ui.core.FirstBaseline
import androidx.ui.core.LastBaseline
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.Text
import androidx.ui.core.globalPosition
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Wrap
import androidx.ui.test.assertIsVisible
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByText
import androidx.ui.test.positionInParent
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.round
import androidx.ui.unit.toPx
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    val longText = "Message Is very long and long and long and long and long " +
            "and long and long and long and long and long and long"

    @Test
    fun defaultSnackbar_semantics() {
        var clicked = false
        composeTestRule.setMaterialContent {
            Wrap {
                Snackbar(text = "Message", actionText = "UNDO", onActionClick = { clicked = true })
            }
        }

        findByText("Message")
            .assertIsVisible()

        assertThat(clicked).isFalse()

        findByText("UNDO")
            .assertIsVisible()
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
                    OnChildPositioned(onPositioned = { textCoords = it }) {
                        Text("Message")
                    }
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
                assertThat(it.positionInParent.y.round() + it[FirstBaseline]!!)
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
            OnChildPositioned(onPositioned = { snackCoords = it }) {
                Snackbar(
                    text = {
                        OnChildPositioned(onPositioned = { textCoords = it }) {
                            Text("Message")
                        }
                    },
                    action = {
                        OnChildPositioned(onPositioned = { buttonCoords = it }) {
                            TextButton(onClick = {}) {
                                OnChildPositioned(onPositioned = { buttonTextCoords = it }) {
                                    Text("Undo")
                                }
                            }
                        }
                    }
                )
            }
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
                    localTextCoords.globalPosition.y.round() +
                            localTextCoords[FirstBaseline]!!
                ).isEqualTo(30.dp.toIntPx())
                assertThat(
                    buttonTextPos.y.round() + localButtonTextCoords[FirstBaseline]!!
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
                    OnChildPositioned(onPositioned = { textCoords = it }) {
                        Text(longText, maxLines = 2)
                    }
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
                assertThat(
                    it.positionInParent.y.round() + it[FirstBaseline]!!
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
            OnChildPositioned(onPositioned = { snackCoords = it }) {
                Snackbar(
                    text = {
                        OnChildPositioned(onPositioned = { textCoords = it }) {
                            Text(longText, maxLines = 2)
                        }
                    },
                    action = {
                        OnChildPositioned(onPositioned = { buttonCoords = it }) {
                            TextButton(onClick = {}) {
                                Text("Undo")
                            }
                        }
                    }
                )
            }
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
                    buttonPositionInSnack.y + localButtonCoords.size.height / 2

                assertThat(localTextCoords[FirstBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(localTextCoords[LastBaseline]).isNotEqualTo(IntPx.Zero)
                assertThat(localTextCoords[FirstBaseline])
                    .isNotEqualTo(localTextCoords[LastBaseline])
                assertThat(
                    localTextCoords.globalPosition.y.round() +
                            localTextCoords[FirstBaseline]!!
                ).isEqualTo(30.dp.toIntPx())

                assertThat(buttonCenter).isEqualTo((localSnackCoords.size.height / 2).toPx())
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
            OnChildPositioned(onPositioned = { snackCoords = it }) {
                Snackbar(
                    text = {
                        OnChildPositioned(onPositioned = { textCoords = it }) {
                            Text("Message")
                        }
                    },
                    action = {
                        OnChildPositioned(onPositioned = { buttonCoords = it }) {
                            TextButton(onClick = {}) {
                                Text("Undo")
                            }
                        }
                    },
                    actionOnNewLine = true
                )
            }
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
                    textPositionInSnack.y.round() + localTextCoords[FirstBaseline]!!
                ).isEqualTo(30.dp.toIntPx())

                assertThat(
                    buttonPositionInSnack.y.round() - textPositionInSnack.y.round() -
                            localTextCoords[LastBaseline]!!
                ).isEqualTo(18.dp.toIntPx())

                assertThat(
                    localSnackCoords.size.height - buttonPositionInSnack.y.round() -
                            localButtonCoords.size.height
                ).isEqualTo(8.dp.toIntPx())

                assertThat(
                    localSnackCoords.size.width - buttonPositionInSnack.x.round() -
                            localButtonCoords.size.width
                ).isEqualTo(8.dp.toIntPx())
            }
        }
    }
}