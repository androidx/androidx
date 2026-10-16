/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.graphics.shapes.RoundedPolygon

/** Draws a rounded polygon. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteDrawScope.drawRoundedPolygon(roundedPolygon: RoundedPolygon, paint: RemotePaint?) {
    remoteCanvas.drawRoundedPolygon(roundedPolygon, paint)
}

/** Draws a morph between two rounded polygons. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteDrawScope.drawRoundedPolygonMorph(
    from: RoundedPolygon,
    to: RoundedPolygon,
    progress: RemoteFloat,
    paint: RemotePaint?,
) {
    remoteCanvas.drawRoundedPolygonMorph(from, to, progress, paint)
}
