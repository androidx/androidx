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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RowColumnModifierTest() {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testRow_updatesOnAlignmentChange() {
        var positionInParentY = 0f
        var alignment by mutableStateOf(Alignment.Top)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    Row(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp(), if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        }
                                    )
                                    .align(alignment)
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentY).isEqualTo(0)
            alignment = Alignment.CenterVertically
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentY).isEqualTo(5)
        }
    }

    @Test
    fun testRow_updatesOnAlignByBlockChange() {
        var positionInParentY = 0f
        val alignByBlock: (Measured) -> Int = { _ -> 5 }
        val alignByNewBlock: (Measured) -> Int = { _ -> 0 }
        var alignment by mutableStateOf(alignByBlock)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    Row(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp(), if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        }
                                    )
                                    .alignBy(
                                        if (index == 4) {
                                            alignment
                                        } else {
                                            alignByBlock
                                        }
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentY).isEqualTo(0)
            alignment = alignByNewBlock
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentY).isEqualTo(5)
        }
    }

    @Test
    fun testRow_updatesOnWeightChange() {
        var width = 0
        var fill by mutableStateOf(false)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    Row(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp()
                                    )
                                    .weight(1f, fill)
                                    .onSizeChanged {
                                        if (index > 0) {
                                            Truth
                                                .assertThat(it.width)
                                                .isEqualTo(width)
                                        } else {
                                            width = it.width
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(20)
            fill = true
        }

        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
        }
    }

    @Test
    fun testRow_updatesOnWeightAndAlignmentChange() {
        var width = 0
        var fill by mutableStateOf(false)
        var positionInParentY = 0f
        var alignment by mutableStateOf(Alignment.Top)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    Row(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                .size(
                                    20.toDp(), if (index == 4) {
                                        10.toDp()
                                    } else {
                                        20.toDp()
                                    }
                                )
                                .weight(1f, fill)
                                .onSizeChanged {
                                    if (index > 0) {
                                        Truth
                                            .assertThat(it.width)
                                            .isEqualTo(width)
                                    } else {
                                        width = it.width
                                    }
                                }
                                .align(alignment)
                                .onPlaced {
                                    if (index == 4) {
                                        val positionInParent = it.positionInParent()
                                        positionInParentY = positionInParent.y
                                    }
                                })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(20)
            Truth.assertThat(positionInParentY).isEqualTo(0)
            alignment = Alignment.CenterVertically
            fill = true
        }

        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(positionInParentY).isEqualTo(5)
        }
    }

    @Test
    fun testColumn_updatesOnAlignmentChange() {
        var positionInParentX = 0f
        var alignment by mutableStateOf(Alignment.Start)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    Column(
                        Modifier
                            .wrapContentWidth()
                            .wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        },
                                        20.toDp(),
                                    )
                                    .align(alignment)
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentX = positionInParent.x
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentX).isEqualTo(0)
            alignment = Alignment.CenterHorizontally
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentX).isEqualTo(5)
        }
    }

    @Test
    fun testColumn_updatesOnAlignByBlockChange() {
        var positionInParentX = 0f
        val alignByBlock: (Measured) -> Int = { _ -> 5 }
        val alignByNewBlock: (Measured) -> Int = { _ -> 0 }
        var alignment by mutableStateOf(alignByBlock)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    Column(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        }, 20.toDp()
                                    )
                                    .alignBy(
                                        if (index == 4) {
                                            alignment
                                        } else {
                                            alignByBlock
                                        }
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentX = positionInParent.x
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentX).isEqualTo(0)
            alignment = alignByNewBlock
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentX).isEqualTo(5)
        }
    }

    @Test
    fun testColumn_updatesOnWeightChange() {
        var height = 0
        var fill by mutableStateOf(false)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    Column(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp()
                                    )
                                    .weight(1f, fill)
                                    .onSizeChanged {
                                        if (index > 0) {
                                            Truth
                                                .assertThat(it.height)
                                                .isEqualTo(height)
                                        } else {
                                            height = it.height
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(20)
            fill = true
        }

        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
        }
    }

    @Test
    fun testColumn_updatesOnWeightAndAlignmentChange() {
        var height = 0
        var fill by mutableStateOf(false)
        var positionInParentX = 0f
        var alignment by mutableStateOf(Alignment.Start)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    Column(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                .size(
                                    if (index == 4) {
                                        10.toDp()
                                    } else {
                                        20.toDp()
                                    },
                                    20.toDp(),
                                )
                                .weight(1f, fill)
                                .onSizeChanged {
                                    if (index > 0) {
                                        Truth
                                            .assertThat(it.height)
                                            .isEqualTo(height)
                                    } else {
                                        height = it.height
                                    }
                                }
                                .align(alignment)
                                .onPlaced {
                                    if (index == 4) {
                                        val positionInParent = it.positionInParent()
                                        positionInParentX = positionInParent.x
                                    }
                                })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(20)
            Truth.assertThat(positionInParentX).isEqualTo(0)
            alignment = Alignment.CenterHorizontally
            fill = true
        }

        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(positionInParentX).isEqualTo(5)
        }
    }
}