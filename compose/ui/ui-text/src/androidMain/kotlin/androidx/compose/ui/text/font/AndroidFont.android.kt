/*
 * Copyright 2021 The Android Open Source Project
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

@file:JvmName("AndroidFontKt")

package androidx.compose.ui.text.font

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import java.io.File

/**
 * Create a Font declaration from a file in the assets directory. The content of the [File] is
 * read during construction.
 *
 * @param path full path starting from the assets directory (i.e. dir/myfont.ttf for
 * assets/dir/myfont.ttf).
 * @param assetManager Android AssetManager
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param variationSettings on API 26 and above these settings are applied to a variable font when
 * the font is loaded
 */
@Stable
fun Font(
    path: String,
    assetManager: AssetManager,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings = FontVariation.Settings(weight, style)
): Font = AndroidAssetFont(assetManager, path, weight, style, variationSettings)

/**
 * Create a Font declaration from a file. The content of the [File] is read during construction.
 *
 * @param file the font file.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param variationSettings on API 26 and above these settings are applied to a variable font when
 * the font is loaded
 */
@Stable
@Suppress("StreamFiles")
fun Font(
    file: File,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings = FontVariation.Settings(weight, style)
): Font = AndroidFileFont(file, weight, style, variationSettings)

/**
 * Create a Font declaration from a [ParcelFileDescriptor]. The content of the
 * [ParcelFileDescriptor] is read during construction.
 *
 * @param fileDescriptor the file descriptor for the font file.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param variationSettings these settings are applied to a variable font when the font is loaded
 */
@RequiresApi(26)
@Stable
fun Font(
    fileDescriptor: ParcelFileDescriptor,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings = FontVariation.Settings(weight, style)
): Font = AndroidFileDescriptorFont(fileDescriptor, weight, style, variationSettings)

/**
 * Font for use on Android.
 *
 * All [AndroidFont] produce an [android.graphics.Typeface] which may be used to draw text on
 * Android. This is the main low-level API for introducing a new Font description to Compose on
 * Android for both blocking and async load.
 *
 * You may subclass this to add new types of font descriptions that may be used in
 * [FontListFontFamily]. For example, you can add a [FontLoadingStrategy.Blocking] font that
 * returns a Typeface from a local resource not supported by an existing [Font]. Or, you can create
 * an [FontLoadingStrategy.Async] font that loads a font file from your server.
 *
 * When introducing new font descriptors, it is recommended to follow the patterns of providing a
 * public Font constructor and a private implementation class:
 *
 * 1. Declare an internal or private subclass of AndroidFont
 * 2. Expose a public Font(...) constructor that returns your new type.
 *
 * Font constructors are
 *
 * 1. Regular functions named `Font` that return type `Font`
 * 2. The first argument is the font name, or similar object that describes the font uniquely
 * 3. If the font has a provider, loader, or similar argument, put it after the font name.
 * 4. The last two arguments are FontWeight and FontStyle.
 *
 * Examples of Font constructors:
 *
 * ```
 * fun Font("myIdentifier", MyFontLoader, FontWeight, FontStyle): Font
 * fun Font(CustomFontDescription(...), MyFontLoader, FontWeight, FontStyle): Font
 * fun Font(CustomFontDescription(...), FontWeight, FontStyle): Font
 *```
 *
 * @param loadingStrategy loadingStrategy this font will provide in fallback chains
 * @param typefaceLoader a loader that knows how to load this [AndroidFont], may be shared between
 * several fonts
 */
abstract class AndroidFont constructor(
    final override val loadingStrategy: FontLoadingStrategy,
    val typefaceLoader: TypefaceLoader,
    variationSettings: FontVariation.Settings,
) : Font {

    @Deprecated(
        "Replaced with fontVariation constructor",
        ReplaceWith(
            "AndroidFont(loadingStrategy, typefaceLoader, FontVariation.Settings())"
        )
    )
    constructor(
        loadingStrategy: FontLoadingStrategy,
        typefaceLoader: TypefaceLoader,
    ) : this(loadingStrategy, typefaceLoader, FontVariation.Settings())

    /**
     * The settings that will be applied to this font, if supported by the font.
     *
     * If the font does not support a [FontVariation.Setting], it has no effect.
     *
     * Subclasses are required to apply these variation settings during font loading path on
     * appropriate API levels, for example by using [Typeface.Builder.setFontVariationSettings].
     *
     * Subclasses may safely apply all variation settings without querying the font file. Android
     * will ignore any unsupported axis.
     */
    val variationSettings: FontVariation.Settings = variationSettings

    /**
     * Loader for loading an [AndroidFont] and producing an [android.graphics.Typeface].
     *
     * This interface is not intended to be used by application developers for text display. To load
     * a typeface for display use [FontFamily.Resolver].
     *
     * [TypefaceLoader] allows the introduction of new types of font descriptors for use in
     * [FontListFontFamily]. A [TypefaceLoader] allows a new subclass of [AndroidFont] to be used by
     * normal compose text rendering methods.
     *
     * Examples of new types of fonts that [TypefaceLoader] can add:
     * - [FontLoadingStrategy.Blocking] [Font] that loads Typeface from a local resource not
     * supported by an existing font
     * - [FontLoadingStrategy.OptionalLocal] [Font] that "loads" a platform Typeface only available
     * on some devices.
     * - [FontLoadingStrategy.Async] [Font] that loads a font from a backend via a network request.
     *
     * During resolution from [FontFamily.Resolver], an [AndroidFont] subclass will be queried for
     * an appropriate loader.
     *
     * The loader attached to an instance of an [AndroidFont] is only required to be able to load
     * that instance, though it is advised to create one loader for all instances of the same
     * subclass and share them between [AndroidFont] instances to avoid allocations or allow
     * caching.
     */
    interface TypefaceLoader {
        /**
         * Immediately load the font in a blocking manner such that it will be available this frame.
         *
         * This method will be called on a UI-critical thread, however it has been determined that
         * this font is required for the current frame. This method is allowed to perform small
         * amounts of I/O to load a font file from disk.
         *
         * This method should never perform expensive I/O operations, such as loading from a remote
         * source. If expensive operations are required to complete the font, this method may choose
         * to throw. Note that this method will never be called for fonts with
         * [FontLoadingStrategy.Async].
         *
         * It is possible for [loadBlocking] to be called for the same instance of [AndroidFont] in
         * parallel. Implementations should support parallel concurrent loads, or de-dup.
         *
         * @param context current Android context for loading the font
         * @param font the font to load which contains this loader as [AndroidFont.typefaceLoader]
         * @return [android.graphics.Typeface] for loaded font, or null if the font fails to load
         */
        fun loadBlocking(context: Context, font: AndroidFont): Typeface?

        /**
         * Asynchronously load the font, from either local or remote sources such that it will cause
         * text reflow when loading completes.
         *
         * This method will be called on a UI-critical thread, and should not block the thread for
         * font loading from sources slower than the local filesystem. More expensive loads should
         * dispatch to an appropriate thread.
         *
         * This method is always called in a timeout context and must return it's final value within
         * 15 seconds. If the Typeface is not resolved within 15 seconds, the async load is
         * cancelled and considered a permanent failure. Implementations should use structured
         * concurrency to cooperatively cancel work.
         *
         * Compose does not know what resources are required to satisfy a font load.
         * Subclasses implementing [FontLoadingStrategy.Async] behavior should ensure requests are
         * de-duped for the same resource.
         *
         * It is possible for [awaitLoad] to be called for the same instance of [AndroidFont] in
         * parallel. Implementations should support parallel concurrent loads, or de-dup.
         *
         * @param context current Android context for loading the font
         * @param font the font to load which contains this loader as [AndroidFont.typefaceLoader]
         * @return [android.graphics.Typeface] for loaded font, or null if not available
         *
         */
        suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface?
    }
}

// keep generating AndroidFontKt to avoid API change
private fun generateAndroidFontKtForApiCompatibility() {}
