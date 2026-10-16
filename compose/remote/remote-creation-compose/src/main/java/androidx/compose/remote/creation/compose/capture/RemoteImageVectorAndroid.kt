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

@file:JvmName("RemoteImageVectorKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.DrawableRes
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.toRemotePathNodes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.isSpecified

/**
 * Load a [RemoteImageVector] from an Android vector resource.
 *
 * @param id the drawable resource identifier of the vector drawable
 * @return the [RemoteImageVector] associated with the resource
 */
@Composable
public fun RemoteImageVector.Companion.vectorResource(@DrawableRes id: Int): RemoteImageVector {
    val imageVector = ImageVector.vectorResource(id)
    return remember(imageVector) { imageVector.toRemoteImageVector() }
}

/** Converts a Compose UI [ImageVector] to a [RemoteImageVector]. */
public fun ImageVector.toRemoteImageVector(): RemoteImageVector {
    val defWidth = if (defaultWidth.isSpecified) defaultWidth.asRdp() else DefaultIconSize.rdp
    val defHeight = if (defaultHeight.isSpecified) defaultHeight.asRdp() else DefaultIconSize.rdp
    val vWidth = if (viewportWidth.isNaN()) defWidth.value else viewportWidth.rf
    val vHeight = if (viewportHeight.isNaN()) defHeight.value else viewportHeight.rf

    val builder =
        RemoteImageVector.Builder(
            defaultWidth = defWidth,
            defaultHeight = defHeight,
            viewportWidth = vWidth,
            viewportHeight = vHeight,
            tintColor = RemoteColor(tintColor),
            name = name,
            tintBlendMode = tintBlendMode,
            autoMirror = autoMirror,
        )

    fun addGroupToBuilder(group: VectorGroup) {
        for (i in 0 until group.size) {
            val node = group[i]
            if (node is VectorPath) {
                builder.addPath(
                    pathData = node.pathData.toRemotePathNodes(),
                    pathFillType = node.pathFillType,
                    name = node.name,
                    fill = node.fill,
                    fillAlpha = node.fillAlpha.rf,
                    stroke = node.stroke,
                    strokeAlpha = node.strokeAlpha.rf,
                    strokeLineWidth = node.strokeLineWidth.rf,
                    strokeLineCap = node.strokeLineCap,
                    strokeLineJoin = node.strokeLineJoin,
                    strokeLineMiter = node.strokeLineMiter.rf,
                    trimPathStart = node.trimPathStart.rf,
                    trimPathEnd = node.trimPathEnd.rf,
                    trimPathOffset = node.trimPathOffset.rf,
                )
            } else if (node is VectorGroup) {
                builder.addGroup(
                    name = node.name,
                    rotate = node.rotation.rf,
                    pivotX = node.pivotX.rf,
                    pivotY = node.pivotY.rf,
                    scaleX = node.scaleX.rf,
                    scaleY = node.scaleY.rf,
                    translationX = node.translationX.rf,
                    translationY = node.translationY.rf,
                    clipPathData = node.clipPathData.toRemotePathNodes(),
                )
                addGroupToBuilder(node)
                builder.clearGroup()
            }
        }
    }

    addGroupToBuilder(root)
    return builder.build()
}
