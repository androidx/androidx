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
@file:Suppress("RestrictedApiAndroidX")

package androidx.glance.appwidget.remotecompose.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.ColorInt
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BitmapImageProvider
import androidx.glance.ColorFilterParams
import androidx.glance.EmittableImage
import androidx.glance.GlanceModifier
import androidx.glance.IconImageProvider
import androidx.glance.ImageProvider
import androidx.glance.TintColorFilterParams
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.DebugRemoteCompose
import androidx.glance.appwidget.remotecompose.TAG
import androidx.glance.appwidget.remotecompose.TranslationContext
import androidx.glance.appwidget.remotecompose.convertGlanceModifierToRemoteComposeModifier
import androidx.glance.layout.ContentScale
import androidx.glance.unit.FixedColorProvider

// todo: refactor constructors so we don't need this class
internal sealed interface Either<out T1, out T2> {
    class A<T1>(val value: T1) : Either<T1, Nothing>

    class B<T2>(val value: T2) : Either<Nothing, T2>
}

/**
 * Intermediate Image representation during Glance to RemoteCompose translation.
 *
 * @param provider The [ImageProvider] with the desired image.
 * @param colorFilterParams Optional color filter to apply to the image.
 * @param translationContext
 * @param targetDecodeWidth the width for the image, or -1 to ignore. Used during bitmap loading.
 * @param targetDecodeHeight The height for the image, or -1 to ignore. Used during bitmap loading.
 * @param aModifier [Either] a [RecordingModifier] or a [GlanceModifier]. Some code paths have a
 *   glance modifier while others do not.
 * @constructor Creates an RcImage from an [ImageProvider]
 */
internal class RcImage
private constructor(
    provider: ImageProvider,
    colorFilterParams: ColorFilterParams?,
    translationContext: TranslationContext,
    targetDecodeWidth: Int = -1,
    targetDecodeHeight: Int = -1,
    aModifier: Either<RecordingModifier, GlanceModifier>,
    private val scaleType: Int, // see RemoteComposeWriter's IMAGE_SCALE_* constants
) : RcElement(translationContext) {

    private val bitmapId: Int?
    override val outputModifier: RecordingModifier

    /** Use [EmittableImage] as the basis for this RcImage. */
    constructor(
        emittable: EmittableImage,
        translationContext: TranslationContext,
    ) : this(
        provider = emittable.provider!!,
        colorFilterParams = emittable.colorFilterParams,
        translationContext = translationContext,
        aModifier = Either.B(emittable.modifier),
        scaleType = emittable.contentScale.toRemoteComposeEnum(),
    )

    constructor(
        provider: ImageProvider,
        tint: Int?,
        translationContext: TranslationContext,
        targetDecodeWidth: Int,
        targetDecodeHeight: Int,
        recordingModifier: RecordingModifier,
        contentScale: ContentScale = ContentScale.Fit, // Match  glance Image's default
    ) : this(
        provider = provider,
        colorFilterParams = tint?.let { TintColorFilterParams(FixedColorProvider(Color(tint))) },
        translationContext = translationContext,
        targetDecodeWidth = targetDecodeWidth,
        targetDecodeHeight = targetDecodeHeight,
        aModifier = Either.A(recordingModifier),
        scaleType = contentScale.toRemoteComposeEnum(),
    )

    init {
        outputModifier =
            when (aModifier) {
                is Either.A -> {
                    val recordingModifier: RecordingModifier = aModifier.value
                    recordingModifier
                }
                is Either.B -> {
                    val glanceModifier: GlanceModifier = aModifier.value
                    convertGlanceModifierToRemoteComposeModifier(
                        modifiers = glanceModifier,
                        translationContext = translationContext,
                    )
                }
            }

        val context = translationContext.context
        with(translationContext.remoteComposeContext) {
            val bitmap: Bitmap? =
                bitmapFromImageProvider(
                    provider,
                    tint =
                        (colorFilterParams as? TintColorFilterParams)
                            ?.colorProvider
                            ?.getColor(context)
                            ?.toArgb(),
                    context = context,
                    targetWidth = targetDecodeWidth,
                    targetHeight = targetDecodeHeight,
                )
            if (bitmap != null) {
                bitmapId = this.addBitmap(bitmap)
            } else {
                bitmapId = null
            }
        } // end-with
    }

    override fun writeComponent(translationContext: TranslationContext) {
        if (bitmapId != null) {
            translationContext.remoteComposeContext.image(
                outputModifier, // modifier
                bitmapId, // id
                scaleType,
                1f, // alpha. TODO: set this
            )
        } else {
            // TODO, figure out behavior for null bitmap
            // TODO: maybe we emit a spacer? or set a boolean that makes the write
            translationContext.remoteComposeContext.box(outputModifier)
        }
    }
}

private fun bitmapFromImageProvider(
    provider: ImageProvider,
    @ColorInt tint: Int?,
    context: Context,
    targetWidth: Int,
    targetHeight: Int,
): Bitmap? {
    if (DebugRemoteCompose) {
        Log.d(
            TAG,
            "RcImage: bitmapFromImageProvider() called with: provider = $provider, tint = $tint, context = $context, targetWidth = $targetWidth, targetHeight = $targetHeight",
        )
    }

    return when (provider) {
        is AndroidResourceImageProvider -> {
            //  TODO: eagerly loads the resource
            val resId = provider.resId
            val drawable: Drawable? = context.getDrawable(resId)
            try {
                drawable ?: return null

                val w = if (targetWidth < 0) drawable.intrinsicWidth else targetWidth
                val h = if (targetHeight < 0) drawable.intrinsicHeight else targetHeight

                val intermediate: Bitmap = drawable.toBitmap(w, h)
                if (tint == null) {
                    intermediate
                } else {
                    tintImage(intermediate, tint)
                }
            } catch (e: Exception) {
                val name = context.resources.getResourceName(resId)
                val message = "RcImage could not load resource: $resId, $name"
                throw RuntimeException(message, e)
            }
        }
        is BitmapImageProvider -> {
            if (tint == null) {
                provider.bitmap
            } else {
                tintImage(provider.bitmap, tint)
            }
        }
        is IconImageProvider -> {
            val drawable = provider.icon.loadDrawable(context) ?: return null // todo correct?
            drawable.toBitmap()
            val intermediate: Bitmap = drawable.toBitmap()
            if (tint == null) {
                intermediate
            } else {
                tintImage(intermediate, tint)
            }
        }
        else -> null
    }.also {
        if (DebugRemoteCompose) {
            Log.d(TAG, "RcImage\tBitmap size: ${it?.allocationByteCount}")
        }
    }
}

private fun tintImage(inputBitmap: Bitmap, @ColorInt color: Int): Bitmap {
    val paint = Paint()
    paint.setColorFilter(PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN))
    val output =
        Bitmap.createBitmap(inputBitmap.getWidth(), inputBitmap.getHeight(), Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawBitmap(inputBitmap, 0f, 0f, paint)
    return output
}

private fun ContentScale.toRemoteComposeEnum(): Int {
    return when {
        this == ContentScale.Crop -> {
            RemoteComposeWriter.IMAGE_SCALE_CROP
        }
        this == ContentScale.Fit -> {
            RemoteComposeWriter.IMAGE_SCALE_FIT
        }
        this == ContentScale.FillBounds -> {
            RemoteComposeWriter.IMAGE_SCALE_FILL_BOUNDS
        }
        else -> {
            Log.w(TAG, "RcImage: Unknown scale type $this")
            RemoteComposeWriter.IMAGE_SCALE_NONE
        }
    }
}
