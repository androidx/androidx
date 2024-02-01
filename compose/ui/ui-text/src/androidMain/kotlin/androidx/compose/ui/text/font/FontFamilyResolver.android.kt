/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.RestrictTo
import androidx.compose.runtime.State
import androidx.compose.ui.text.InternalTextApi
import kotlin.coroutines.CoroutineContext

/**
 * Create a new fontFamilyResolver for use outside of composition context
 *
 * Example usages:
 * - Application.onCreate to preload fonts
 * - Creating Paragraph objects on background thread
 *
 * Usages inside of Composition should use LocalFontFamilyResolver.current
 *
 * All instances of FontFamily.Resolver created by [createFontFamilyResolver] share the same
 * typeface caches.
 */
fun createFontFamilyResolver(
    context: Context
): FontFamily.Resolver {
    return FontFamilyResolverImpl(
        AndroidFontLoader(context),
        AndroidFontResolveInterceptor(context)
    )
}

/**
 * Create a new fontFamilyResolver for use outside of composition context with a coroutine context.
 *
 * Example usages:
 * - Application.onCreate to preload fonts
 * - Creating Paragraph objects on background thread
 * - Configuring LocalFontFamilyResolver with a different CoroutineScope
 *
 * Usages inside of Composition should use LocalFontFamilyResolver.current
 *
 * Any [kotlinx.coroutines.CoroutineExceptionHandler] provided will be called with
 * exceptions related to fallback font loading. These exceptions are not fatal, and indicate
 * that font fallback continued to the next font load.
 *
 * If no [kotlinx.coroutines.CoroutineExceptionHandler] is provided, a default implementation will
 * be added that ignores all exceptions.
 *
 * All instances of FontFamily.Resolver created by [createFontFamilyResolver] share the same
 * typeface caches.
 *
 * @param context Android context for resolving fonts
 * @param coroutineContext context to launch async requests in during resolution.
 */
fun createFontFamilyResolver(
    context: Context,
    coroutineContext: CoroutineContext
): FontFamily.Resolver {
    return FontFamilyResolverImpl(
        AndroidFontLoader(context),
        AndroidFontResolveInterceptor(context),
        GlobalTypefaceRequestCache,
        FontListFontFamilyTypefaceAdapter(
            GlobalAsyncTypefaceCache,
            coroutineContext
        )
    )
}

/**
 * Create a FontFamily.resolver that does not share a cache with other FontFamily.Resolvers.
 *
 * This is primarily useful for testing or benchmarking.
 *
 */
@InternalTextApi // exposed for benchmarking, not a stable API.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun emptyCacheFontFamilyResolver(context: Context): FontFamily.Resolver {
    return FontFamilyResolverImpl(
        AndroidFontLoader(context),
        typefaceRequestCache = TypefaceRequestCache(),
        fontListFontFamilyTypefaceAdapter = FontListFontFamilyTypefaceAdapter(
            AsyncTypefaceCache()
        )
    )
}

/**
 * Resolve a font to an Android Typeface
 *
 * On Android, font resolution always produces an [android.graphics.Typeface].
 *
 * This convenience method converts State<Any> to State<Typeface> to avoid casting the result.
 *
 * @param fontFamily fontFamily to resolve from
 * @param fontWeight font weight to resolve in [fontFamily], will use closest match if not exact
 * @param fontStyle italic or upright text, to resolve in [fontFamily]
 * @param fontSynthesis allow font synthesis if [fontFamily] or [fontStyle] don't have an exact
 * match. This will allow "fake bold" (drawing with too wide a brush) and "fake italic" (drawing
 * then skewing) to be applied when no exact match is present for the weight and style.
 */
fun FontFamily.Resolver.resolveAsTypeface(
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    fontSynthesis: FontSynthesis = FontSynthesis.All
): State<Typeface> {
    // this unchecked cast is done here to avoid callers having to do it at every call site
    @Suppress("UNCHECKED_CAST")
    return resolve(fontFamily, fontWeight, fontStyle, fontSynthesis) as State<Typeface>
}
