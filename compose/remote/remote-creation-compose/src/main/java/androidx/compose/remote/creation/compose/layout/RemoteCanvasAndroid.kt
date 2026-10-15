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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcPlatformServices.RcPathArrayCreator
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.shapes.MorphTweenUtility
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.PathFillType
import androidx.graphics.shapes.RoundedPolygon

/** Draws a path using the specified [pathFillType] and [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawPath(
    path: RemotePath,
    pathFillType: PathFillType,
    paint: RemotePaint? = null,
) {
    drawPath(
        path,
        if (pathFillType == PathFillType.EvenOdd) RemoteCanvas.WINDING_EVEN_ODD
        else RemoteCanvas.WINDING_NON_ZERO,
        paint,
    )
}

/** Clips the current canvas state to the specified rectangle. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.clipRect(
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
    @Suppress("UNUSED_PARAMETER") clipOp: ClipOp,
) {
    clipRect(left, top, right, bottom)
}

/** Clips the current canvas state to the specified [path]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.clipPath(
    path: RemotePath,
    @Suppress("UNUSED_PARAMETER") clipOp: ClipOp,
) {
    clipPath(path)
}

/** Draws a [RoundedPolygon] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRoundedPolygon(roundedPolygon: RoundedPolygon, paint: RemotePaint?) {
    recordRenderingOp(paint) {
        val pathData = MorphTweenUtility.cubicsToPathData(roundedPolygon.cubics)
        val id =
            document.addPathData(
                object : RcPathArrayCreator {
                    override fun createFloatArray(): FloatArray = pathData
                }
            )
        document.buffer.addDrawPath(id)
    }
}

/** Draws a morph between two [RoundedPolygon]s using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRoundedPolygonMorph(
    from: RoundedPolygon,
    to: RoundedPolygon,
    progress: RemoteFloat,
    paint: RemotePaint?,
) {
    val op =
        recordRenderingOp(paint) {
            MorphTweenUtility.emitMorphAsTweens(
                document,
                from,
                to,
                progress.getFloatIdForCreationState(creationState),
            )
        }
    buffer.addRoots(op, progress)
}
