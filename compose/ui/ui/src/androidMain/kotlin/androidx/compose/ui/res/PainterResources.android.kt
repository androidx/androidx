/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.res

import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.GroupComponent
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.compat.seekToStartTag
import androidx.compose.ui.graphics.vector.createGroupComponent
import androidx.compose.ui.graphics.vector.createVectorPainterFromImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalImageVectorCache
import androidx.compose.ui.res.ImageVectorCache.ImageVectorEntry

/**
 * Create a [Painter] from an Android resource id. This can load either an instance of
 * [BitmapPainter] or [VectorPainter] for [ImageBitmap] based assets or vector based assets
 * respectively. The resources with the given id must point to either fully rasterized
 * images (ex. PNG or JPG files) or VectorDrawable xml assets. API based xml Drawables
 * are not supported here.
 *
 * Example:
 * @sample androidx.compose.ui.samples.PainterResourceSample
 *
 * Alternative Drawable implementations can be used with compose by calling
 * [drawIntoCanvas] and drawing with the Android framework canvas provided through [nativeCanvas]
 *
 * Example:
 * @sample androidx.compose.ui.samples.AndroidDrawableInDrawScopeSample
 *
 * @param id Resources object to query the image file from
 *
 * @return [Painter] used for drawing the loaded resource
 */
@Composable
fun painterResource(@DrawableRes id: Int): Painter {
    val context = LocalContext.current
    val res = resources()
    val value = remember { TypedValue() }
    res.getValue(id, value, true)
    val path = value.string
    // Assume .xml suffix implies loading a VectorDrawable resource
    return if (path?.endsWith(".xml") == true) {
        obtainVectorPainter(context.theme, res, id, value.changingConfigurations)
    } else {
        // Otherwise load the bitmap resource
        val imageBitmap = remember(path, id, context.theme) {
            loadImageBitmapResource(path, res, id)
        }
        BitmapPainter(imageBitmap)
    }
}

/**
 * Helper method to load the previously cached VectorPainter instance if it exists, otherwise
 * this parses the xml into an ImageVector and creates a new VectorPainter inserting it into the
 * cache for reuse
 */
@Composable
private fun obtainVectorPainter(
    theme: Resources.Theme,
    res: Resources,
    id: Int,
    changingConfigurations: Int
): VectorPainter {
    val imageVectorCache = LocalImageVectorCache.current
    val density = LocalDensity.current
    val key = remember(theme, id, density) {
        ImageVectorCache.Key(theme, id, density)
    }
    val imageVectorEntry = imageVectorCache[key]
    var imageVector = imageVectorEntry?.imageVector
    if (imageVector == null) {
        @Suppress("ResourceType") val parser = res.getXml(id)
        if (parser.seekToStartTag().name != "vector") {
            throw IllegalArgumentException(errorMessage)
        }
        imageVector = loadVectorResourceInner(theme, res, parser)
    }

    var rootGroup = imageVectorEntry?.rootGroup
    if (rootGroup == null) {
        rootGroup = GroupComponent().apply {
            createGroupComponent(imageVector.root)
        }
        imageVectorCache[key] = ImageVectorEntry(imageVector, changingConfigurations, rootGroup)
    }
    return remember(key) {
        createVectorPainterFromImageVector(density, imageVector, rootGroup)
    }
}

/**
 * Helper method to validate the asset resource is a supported resource type and returns
 * an ImageBitmap resource. Because this throws exceptions we cannot have this implementation
 * as part of the composable implementation it is invoked in.
 */
private fun loadImageBitmapResource(path: CharSequence, res: Resources, id: Int): ImageBitmap {
    try {
        return ImageBitmap.imageResource(res, id)
    } catch (exception: Exception) {
        throw ResourceResolutionException("Error attempting to load resource: $path", exception)
    }
}

/**
 * [Throwable] that is thrown in situations where a resource failed to load.
 */
class ResourceResolutionException(
    message: String,
    cause: Throwable
) : RuntimeException(message, cause)

private const val errorMessage =
    "Only VectorDrawables and rasterized asset types are supported ex. PNG, JPG, WEBP"
