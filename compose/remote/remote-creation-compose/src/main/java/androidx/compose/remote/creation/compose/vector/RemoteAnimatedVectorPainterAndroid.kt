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

package androidx.compose.remote.creation.compose.vector

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp

/**
 * Creates a [RemotePainter] to render an XML Animated Vector Drawable resource at [progress].
 * Defaults to animating continually based on [RemoteTimeVariables.animationTime].
 *
 * @param context The Android context used to load the resource.
 * @param id The XML drawable resource ID.
 * @param progress The [RemoteFloat] animation progress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun painterRemoteAnimatedVector(
    context: Context,
    @DrawableRes id: Int,
    progress: RemoteFloat = defaultProgress(RemoteAnimatedVector.fromXml(context.resources, id)),
): RemotePainter {
    val avd = RemoteAnimatedVector.fromXml(context.resources, id)
    return RemoteAnimatedVectorPainter(avd, progress)
}

/**
 * Remembers a [RemotePainter] for an XML Animated Vector Drawable resource at the given [progress].
 * Defaults to animating continually based on [RemoteTimeVariables.animationTime].
 *
 * @param id The XML drawable resource ID.
 * @param progress The [RemoteFloat] animation progress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberRemoteAnimatedVectorPainter(
    @DrawableRes id: Int,
    progress: RemoteFloat =
        defaultProgress(RemoteAnimatedVector.fromXml(LocalResources.current, id)),
): RemotePainter {
    val resources = LocalResources.current
    return remember(id, progress) {
        val avd = RemoteAnimatedVector.fromXml(resources, id)
        RemoteAnimatedVectorPainter(avd, progress)
    }
}

/**
 * Converts a [RemoteVectorSnapshot] to a standard Compose [ImageVector].
 *
 * This allows rendering the evaluated state of an animated vector in standard Compose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteVectorSnapshot.toImageVector(): ImageVector {
    val builder =
        ImageVector.Builder(
            defaultWidth = width.dp,
            defaultHeight = height.dp,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )

    fun addPath(b: ImageVector.Builder, path: RemoteEvaluatedPath) {
        b.addPath(
            pathData =
                if (path.pathData.isNotEmpty()) {
                    PathParser().parsePathString(path.pathData).toNodes()
                } else {
                    emptyList()
                },
            name = path.name,
            fill = path.fillColor?.let { SolidColor(Color(it)) },
            fillAlpha = path.fillAlpha,
            stroke = path.strokeColor?.let { SolidColor(Color(it)) },
            strokeAlpha = path.strokeAlpha,
            strokeLineWidth = path.strokeWidth,
            strokeLineCap =
                when (path.strokeLineCap) {
                    1 -> StrokeCap.Round
                    2 -> StrokeCap.Square
                    else -> StrokeCap.Butt
                },
            strokeLineJoin =
                when (path.strokeLineJoin) {
                    1 -> StrokeJoin.Round
                    2 -> StrokeJoin.Bevel
                    else -> StrokeJoin.Miter
                },
            strokeLineMiter = path.strokeLineMiter,
            trimPathStart = path.trimPathStart,
            trimPathEnd = path.trimPathEnd,
            trimPathOffset = path.trimPathOffset,
        )
    }

    fun addGroup(b: ImageVector.Builder, group: RemoteEvaluatedGroup) {
        b.addGroup(
            name = group.name,
            rotate = group.rotation,
            pivotX = group.pivotX,
            pivotY = group.pivotY,
            scaleX = group.scaleX,
            scaleY = group.scaleY,
            translationX = group.translationX,
            translationY = group.translationY,
            clipPathData =
                if (group.clipPathData.isNotEmpty()) {
                    PathParser().parsePathString(group.clipPathData).toNodes()
                } else {
                    emptyList()
                },
        )
        for (i in group.children.indices) {
            when (val child = group.children[i]) {
                is RemoteEvaluatedGroup -> addGroup(b, child)
                is RemoteEvaluatedPath -> addPath(b, child)
            }
        }
        b.clearGroup()
    }

    for (i in rootGroup.children.indices) {
        when (val child = rootGroup.children[i]) {
            is RemoteEvaluatedGroup -> addGroup(builder, child)
            is RemoteEvaluatedPath -> addPath(builder, child)
        }
    }
    return builder.build()
}
