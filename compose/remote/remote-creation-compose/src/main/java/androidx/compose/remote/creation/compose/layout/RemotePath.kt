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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

public fun Path.moveTo(x: RemoteFloat, y: RemoteFloat) {
    this.moveTo(x.id, y.id)
}

public fun Path.lineTo(x: RemoteFloat, y: RemoteFloat) {
    this.lineTo(x.id, y.id)
}

public fun Path.addArc(
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
    startAngle: RemoteFloat,
    sweepAngle: RemoteFloat,
) {
    this.addArc(Rect(left.id, top.id, right.id, bottom.id), startAngle.id, sweepAngle.id)
}

public fun Path.quadraticTo(x1: RemoteFloat, y1: RemoteFloat, x2: RemoteFloat, y2: RemoteFloat) {
    this.quadraticTo(x1.id, y1.id, x2.id, y2.id)
}

public fun Path.cubicTo(
    x1: RemoteFloat,
    y1: RemoteFloat,
    x2: RemoteFloat,
    y2: RemoteFloat,
    x3: RemoteFloat,
    y3: RemoteFloat,
) {
    this.cubicTo(x1.id, y1.id, x2.id, y2.id, x3.id, y3.id)
}

public fun RemotePath.moveTo(x: RemoteFloat, y: RemoteFloat) {
    this.moveTo(x.id, y.id)
}

public fun RemotePath.lineTo(x: RemoteFloat, y: RemoteFloat) {
    this.lineTo(x.id, y.id)
}

public fun RemotePath.quadraticTo(
    x1: RemoteFloat,
    y1: RemoteFloat,
    x2: RemoteFloat,
    y2: RemoteFloat,
) {
    this.quadTo(x1.id, y1.id, x2.id, y2.id)
}

public fun RemotePath.cubicTo(
    x1: RemoteFloat,
    y1: RemoteFloat,
    x2: RemoteFloat,
    y2: RemoteFloat,
    x3: RemoteFloat,
    y3: RemoteFloat,
) {
    this.cubicTo(x1.id, y1.id, x2.id, y2.id, x3.id, y3.id)
}

public fun RemotePath.conicTo(
    x1: RemoteFloat,
    y1: RemoteFloat,
    x2: RemoteFloat,
    y2: RemoteFloat,
    weight: RemoteFloat,
) {
    this.conicTo(x1.id, y1.id, x2.id, y2.id, weight.id)
}
