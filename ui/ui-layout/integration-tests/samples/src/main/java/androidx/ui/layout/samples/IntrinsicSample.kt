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

package androidx.ui.layout.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.Column
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.LayoutExpandedHeight
import androidx.ui.layout.LayoutExpandedWidth
import androidx.ui.layout.MaxIntrinsicHeight
import androidx.ui.layout.MaxIntrinsicWidth
import androidx.ui.layout.MinIntrinsicHeight
import androidx.ui.layout.MinIntrinsicWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Wrap

/**
 * Builds a layout containing three [ConstrainedBox] having the same width as the widest one.
 *
 * Here [MinIntrinsicWidth] is adding a speculative width measurement pass for the [Column],
 * whose minimum intrinsic width will correspond to the preferred width of the largest
 * [ConstrainedBox]. Then [MinIntrinsicWidth] will measure the [Column] with tight width, the same
 * as the premeasured minimum intrinsic width, which due to [LayoutExpandedWidth] will force
 * the [ConstrainedBox]s to use the same width.
 */
@Sampled
@Composable
fun SameWidthBoxes() {
    Wrap {
        MinIntrinsicWidth {
            Column(LayoutExpandedHeight) {
                ConstrainedBox(
                    constraints = DpConstraints.tightConstraints(width = 20.dp, height = 10.dp),
                    modifier = LayoutExpandedWidth
                ) {
                    DrawShape(RectangleShape, Color.Gray)
                }
                ConstrainedBox(
                    constraints = DpConstraints.tightConstraints(width = 30.dp, height = 10.dp),
                    modifier = LayoutExpandedWidth
                ) {
                    DrawShape(RectangleShape, Color.Blue)
                }
                ConstrainedBox(
                    constraints = DpConstraints.tightConstraints(width = 10.dp, height = 10.dp),
                    modifier = LayoutExpandedWidth
                ) {
                    DrawShape(RectangleShape, Color.Magenta)
                }
            }
        }
    }
}

/**
 * Builds a layout containing two pieces of text separated by a divider, where the divider
 * is sized according to the height of the longest text.
 *
 * Here [MinIntrinsicHeight] is adding a speculative height measurement pass for the [Row],
 * whose minimum intrinsic height will correspond to the height of the largest [Text]. Then
 * [MinIntrinsicHeight] will measure the [Row] with tight height, the same as the premeasured
 * minimum intrinsic height, which due to [LayoutExpandedHeight] will force the [Text]s and
 * the divider to use the same height.
 */
@Sampled
@Composable
fun MatchParentDividerForText() {
    Wrap {
        MinIntrinsicHeight {
            Row {
                Text(
                    text = "This is a really short text",
                    modifier = LayoutFlexible(1f) + LayoutExpandedHeight
                )
                Container(width = 1.dp, modifier = LayoutExpandedHeight) {
                    DrawShape(RectangleShape, Color.Black)
                }
                Text(
                    text = "This is a much much much much much much much much much much" +
                            " much much much much much much longer text",
                    modifier = LayoutFlexible(1f) + LayoutExpandedHeight
                )
            }
        }
    }
}

/**
 * Builds a layout containing three [Text] boxes having the same width as the widest one.
 *
 * Here [MaxIntrinsicWidth] is adding a speculative width measurement pass for the [Column],
 * whose maximum intrinsic width will correspond to the preferred width of the largest
 * [ConstrainedBox]. Then [MaxIntrinsicWidth] will measure the [Column] with tight width, the same
 * as the premeasured maximum intrinsic width, which due to [LayoutExpandedWidth] modifiers will
 * force the [Container]s to use the same width.
 */
@Sampled
@Composable
fun SameWidthTextBoxes() {
    Wrap {
        MaxIntrinsicWidth {
            Column(LayoutExpandedHeight) {
                Container(LayoutExpandedWidth) {
                    DrawShape(RectangleShape, Color.Gray)
                    Text("Short text")
                }
                Container(LayoutExpandedWidth) {
                    DrawShape(RectangleShape, Color.Blue)
                    Text("Extremely long text giving the width of its siblings")
                }
                Container(LayoutExpandedWidth) {
                    DrawShape(RectangleShape, Color.Magenta)
                    Text("Medium length text")
                }
            }
        }
    }
}

/**
 * Builds a layout containing two [LayoutAspectRatio]s separated by a divider, where the divider
 * is sized according to the height of the taller [LayoutAspectRatio].
 *
 * Here [MaxIntrinsicHeight] is adding a speculative height measurement pass for the [Row], whose
 * maximum intrinsic height will correspond to the height of the taller [LayoutAspectRatio]. Then
 * [MaxIntrinsicHeight] will measure the [Row] with tight height, the same as the premeasured
 * maximum intrinsic height, which due to [LayoutExpandedHeight] modifier will force the
 * [LayoutAspectRatio]s and the divider to use the same height.
 */
@Sampled
@Composable
fun MatchParentDividerForAspectRatio() {
    Wrap {
        MaxIntrinsicHeight {
            Row {
                val modifier = LayoutExpandedHeight + LayoutFlexible(1f)
                Container(modifier + LayoutAspectRatio(2f)) {
                    DrawShape(RectangleShape, Color.Gray)
                }
                Container(width = 1.dp, modifier = LayoutExpandedHeight) {
                    DrawShape(RectangleShape, Color.Black)
                }
                Container(modifier + LayoutAspectRatio(1f)) {
                    DrawShape(RectangleShape, Color.Blue)
                }
            }
        }
    }
}
