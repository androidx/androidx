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
import androidx.compose.composer
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.AspectRatio
import androidx.ui.layout.Column
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MaxIntrinsicHeight
import androidx.ui.layout.MaxIntrinsicWidth
import androidx.ui.layout.MinIntrinsicHeight
import androidx.ui.layout.MinIntrinsicWidth
import androidx.ui.layout.Wrap

/**
 * Builds a layout containing three [ConstrainedBox] having the same width as the widest one.
 *
 * Here [MinIntrinsicWidth] is adding a speculative width measurement pass for the [Column],
 * whose minimum intrinsic width will correspond to the preferred width of the largest
 * [ConstrainedBox]. Then [MinIntrinsicWidth] will measure the [Column] with tight width, the same
 * as the premeasured minimum intrinsic width, which due to [CrossAxisAlignment.Stretch] will force
 * the [ConstrainedBox]s to use the same width.
 */
@Sampled
@Composable
fun SameWidthBoxes() {
    Wrap {
        MinIntrinsicWidth {
            Column(crossAxisAlignment = CrossAxisAlignment.Stretch) {
                ConstrainedBox(DpConstraints.tightConstraints(width = 20.dp, height = 10.dp)) {
                    DrawShape(RectangleShape, Color.Gray)
                }
                ConstrainedBox(DpConstraints.tightConstraints(width = 30.dp, height = 10.dp)) {
                    DrawShape(RectangleShape, Color.Blue)
                }
                ConstrainedBox(DpConstraints.tightConstraints(width = 10.dp, height = 10.dp)) {
                    DrawShape(RectangleShape, Color.Fuchsia)
                }
            }
        }
    }
}

/*
 * Builds a layout containing two pieces of text separated by a divider, where the divider
 * is sized according to the height of the longest text.
 *
 * Here [MinIntrinsicHeight] is adding a speculative height measurement pass for the [FlexRow],
 * whose minimum intrinsic height will correspond to the height of the largest [Text]. Then
 * [MinIntrinsicHeight] will measure the [FlexRow] with tight height, the same as the premeasured
 * minimum intrinsic height, which due to [CrossAxisAlignment.Stretch] will force the [Text]s and
 * the divider to use the same height.
 */
@Sampled
@Composable
fun MatchParentDividerForText() {
    Wrap {
        MinIntrinsicHeight {
            FlexRow(crossAxisAlignment = CrossAxisAlignment.Stretch) {
                expanded(flex = 1f) {
                    Text("This is a really short text")
                }
                inflexible {
                    Container(width = 1.dp) { DrawShape(RectangleShape, Color.Black) }
                }
                expanded(flex = 1f) {
                    Text("This is a much much much much much much much much much much" +
                            " much much much much much much longer text")
                }
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
 * as the premeasured maximum intrinsic width, which due to [CrossAxisAlignment.Stretch] will force
 * the [ConstrainedBox]s to use the same width.
 */
@Sampled
@Composable
fun SameWidthTextBoxes() {
    Wrap {
        MaxIntrinsicWidth {
            Column(crossAxisAlignment = CrossAxisAlignment.Stretch) {
                Wrap {
                    DrawShape(RectangleShape, Color.Gray)
                    Text("Short text")
                }
                Wrap {
                    DrawShape(RectangleShape, Color.Blue)
                    Text("Extremely long text giving the width of its siblings")
                }
                Wrap {
                    DrawShape(RectangleShape, Color.Fuchsia)
                    Text("Medium length text")
                }
            }
        }
    }
}

/*
 * Builds a layout containing two [AspectRatio]s separated by a divider, where the divider
 * is sized according to the height of the taller [AspectRatio].
 *
 * Here [MaxIntrinsicHeight] is adding a speculative height measurement pass for the [FlexRow],
 * whose maximum intrinsic height will correspond to the height of the taller [AspectRatio]. Then
 * [MaxIntrinsicHeight] will measure the [FlexRow] with tight height, the same as the premeasured
 * maximum intrinsic height, which due to [CrossAxisAlignment.Stretch] will force the [AspectRatio]s
 * and the divider to use the same height.
 */
@Sampled
@Composable
fun MatchParentDividerForAspectRatio() {
    Wrap {
        MaxIntrinsicHeight {
            FlexRow(crossAxisAlignment = CrossAxisAlignment.Stretch) {
                expanded(flex = 1f) {
                    AspectRatio(2f) { DrawShape(RectangleShape, Color.Gray) }
                }
                inflexible {
                    Container(width = 1.dp) { DrawShape(RectangleShape, Color.Black) }
                }
                expanded(flex = 1f) {
                    AspectRatio(1f) { DrawShape(RectangleShape, Color.Blue) }
                }
            }
        }
    }
}
