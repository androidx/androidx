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
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.compat.seekToStartTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalImageVectorCache
import androidx.compose.ui.platform.LocalResourceIdCache

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

    // Query the current configuration in order to recompose during configuration changes
    LocalConfiguration.current
    val res = context.resources
    val resourceIdCache = LocalResourceIdCache.current
    val value = resourceIdCache.resolveResourcePath(res, id)

    val path = value.string
    // Assume .xml suffix implies loading a VectorDrawable resource
    return if (path?.endsWith(".xml") == true) {
        val imageVector = loadVectorResource(context.theme, res, id, value.changingConfigurations)
        rememberVectorPainter(imageVector)
    } else {
        // Otherwise load the bitmap resource
        val imageBitmap = remember(path, id, context.theme) {
            loadImageBitmapResource(path, res, id)
        }
        BitmapPainter(imageBitmap)
    }
}

/**
 * Helper method to validate that the xml resource is a vector drawable then load
 * the ImageVector. Because this throws exceptions we cannot have this implementation as part of
 * the composable implementation it is invoked in.
 */
@Composable
private fun loadVectorResource(
    theme: Resources.Theme,
    res: Resources,
    id: Int,
    changingConfigurations: Int
): ImageVector {
    val imageVectorCache = LocalImageVectorCache.current
    val key = ImageVectorCache.Key(theme, id)
    var imageVectorEntry = imageVectorCache[key]
    if (imageVectorEntry == null) {
        @Suppress("ResourceType") val parser = res.getXml(id)
        if (parser.seekToStartTag().name != "vector") {
            throw IllegalArgumentException(errorMessage)
        }
        imageVectorEntry = loadVectorResourceInner(theme, res, parser, changingConfigurations)
        imageVectorCache[key] = imageVectorEntry
    }
    return imageVectorEntry.imageVector
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
