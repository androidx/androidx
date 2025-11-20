/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater

import android.graphics.Canvas
import android.graphics.RectF

/**
 * Represents a single arc line path that will be drawn via [Canvas.drawArc].
 *
 * A few of these elements can be used to form a full ProtoLayout
 * [androidx.wear.protolayout.proto.LayoutElementProto.ArcLine] when drawn with [Canvas.drawArc].
 *
 * Note that this is needed because drawing an Arc can be done the legacy way with having
 * [android.graphics.Path] and using [Canvas.drawPath] as that implementation has a bug when the
 * path is an Arc which causes the aliasing issue (b/393971851).
 *
 * @param oval The bounds of oval used to define the size of the arc
 * @param startAngle The starting angle (in degrees) where the arc begins
 * @param sweepAngle The sweep angle (in degrees) measured clockwise
 */
public data class ArcLinePath(val oval: RectF, val startAngle: Float, val sweepAngle: Float)
