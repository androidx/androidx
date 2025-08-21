/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.layout

import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

fun Path.moveTo(x: Number, y: Number) {
    val ix: Float = if (x is RemoteFloat) x.internalAsFloat() else x.toFloat()
    val iy: Float = if (y is RemoteFloat) y.internalAsFloat() else y.toFloat()
    this.moveTo(ix, iy)
}

fun Path.lineTo(x: Number, y: Number) {
    val ix: Float = if (x is RemoteFloat) x.internalAsFloat() else x.toFloat()
    val iy: Float = if (y is RemoteFloat) y.internalAsFloat() else y.toFloat()
    this.lineTo(ix, iy)
}

fun Path.addArc(
    left: Number,
    top: Number,
    right: Number,
    bottom: Number,
    startAngle: Number,
    sweepAngle: Number,
) {
    val iLeft: Float = if (left is RemoteFloat) left.internalAsFloat() else left.toFloat()
    val iTop: Float = if (top is RemoteFloat) top.internalAsFloat() else top.toFloat()
    val iRight: Float = if (right is RemoteFloat) right.internalAsFloat() else right.toFloat()
    val iBottom: Float = if (bottom is RemoteFloat) bottom.internalAsFloat() else bottom.toFloat()
    val iStartAngle: Float =
        if (startAngle is RemoteFloat) startAngle.internalAsFloat() else startAngle.toFloat()
    val iSweepAngle: Float =
        if (sweepAngle is RemoteFloat) sweepAngle.internalAsFloat() else sweepAngle.toFloat()
    this.addArc(Rect(iLeft, iTop, iRight, iBottom), iStartAngle, iSweepAngle)
}

fun Path.quadraticTo(x1: Number, y1: Number, x2: Number, y2: Number) {
    val ix1: Float = if (x1 is RemoteFloat) x1.internalAsFloat() else x1.toFloat()
    val iy1: Float = if (y1 is RemoteFloat) y1.internalAsFloat() else y1.toFloat()
    val ix2: Float = if (x2 is RemoteFloat) x2.internalAsFloat() else x2.toFloat()
    val iy2: Float = if (y2 is RemoteFloat) y2.internalAsFloat() else y2.toFloat()
    this.quadraticTo(ix1, iy1, ix2, iy2)
}

fun Path.cubicTo(x1: Number, y1: Number, x2: Number, y2: Number, x3: Number, y3: Number) {
    val ix1: Float = if (x1 is RemoteFloat) x1.internalAsFloat() else x1.toFloat()
    val iy1: Float = if (y1 is RemoteFloat) y1.internalAsFloat() else y1.toFloat()
    val ix2: Float = if (x2 is RemoteFloat) x2.internalAsFloat() else x2.toFloat()
    val iy2: Float = if (y2 is RemoteFloat) y2.internalAsFloat() else y2.toFloat()
    val ix3: Float = if (x3 is RemoteFloat) x3.internalAsFloat() else x3.toFloat()
    val iy3: Float = if (y3 is RemoteFloat) y3.internalAsFloat() else y3.toFloat()
    this.cubicTo(ix1, iy1, ix2, iy2, ix3, iy3)
}

fun RemotePath.moveTo(x: Number, y: Number) {
    val ix: Float = if (x is RemoteFloat) x.internalAsFloat() else x.toFloat()
    val iy: Float = if (y is RemoteFloat) y.internalAsFloat() else y.toFloat()
    this.moveTo(ix, iy)
}

fun RemotePath.lineTo(x: Number, y: Number) {
    val ix: Float = if (x is RemoteFloat) x.internalAsFloat() else x.toFloat()
    val iy: Float = if (y is RemoteFloat) y.internalAsFloat() else y.toFloat()
    this.lineTo(ix, iy)
}

fun RemotePath.quadraticTo(x1: Number, y1: Number, x2: Number, y2: Number) {
    val ix1: Float = if (x1 is RemoteFloat) x1.internalAsFloat() else x1.toFloat()
    val iy1: Float = if (y1 is RemoteFloat) y1.internalAsFloat() else y1.toFloat()
    val ix2: Float = if (x2 is RemoteFloat) x2.internalAsFloat() else x2.toFloat()
    val iy2: Float = if (y2 is RemoteFloat) y2.internalAsFloat() else y2.toFloat()
    this.quadraticTo(ix1, iy1, ix2, iy2)
}

fun RemotePath.cubicTo(x1: Number, y1: Number, x2: Number, y2: Number, x3: Number, y3: Number) {
    val ix1: Float = if (x1 is RemoteFloat) x1.internalAsFloat() else x1.toFloat()
    val iy1: Float = if (y1 is RemoteFloat) y1.internalAsFloat() else y1.toFloat()
    val ix2: Float = if (x2 is RemoteFloat) x2.internalAsFloat() else x2.toFloat()
    val iy2: Float = if (y2 is RemoteFloat) y2.internalAsFloat() else y2.toFloat()
    val ix3: Float = if (x3 is RemoteFloat) x3.internalAsFloat() else x3.toFloat()
    val iy3: Float = if (y3 is RemoteFloat) y3.internalAsFloat() else y3.toFloat()
    this.cubicTo(ix1, iy1, ix2, iy2, ix3, iy3)
}
