/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.text.PositionedGlyphs;
import android.graphics.text.TextRunShaper;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LruCache;
import androidx.core.content.res.FontResourcesParserCompat;
import androidx.core.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import androidx.core.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import androidx.core.content.res.FontResourcesParserCompat.ProviderResourceEntry;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.provider.FontRequest;
import androidx.core.provider.FontsContractCompat;
import androidx.core.provider.FontsContractCompat.FontInfo;
import androidx.core.util.Preconditions;
import androidx.tracing.Trace;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Helper for accessing features in {@link Typeface}.
 */
public class TypefaceCompat {
    @RestrictTo(LIBRARY)
    public static final boolean DOWNLOADABLE_FALLBACK_DEBUG = false;

    @RestrictTo(LIBRARY)
    public static final boolean DOWNLOADABLE_FONT_TRACING = true;

    private static final String TAG = "TypefaceCompat";

    static {
        Trace.beginSection("TypefaceCompat static init");
    }
    private static final TypefaceCompatBaseImpl sTypefaceCompatImpl;
    static {
        if (Build.VERSION.SDK_INT >= 31) {
            sTypefaceCompatImpl = new TypefaceCompatApi31Impl();
        } else if (Build.VERSION.SDK_INT >= 29) {
            sTypefaceCompatImpl = new TypefaceCompatApi29Impl();
        } else if (Build.VERSION.SDK_INT >= 28) {
            sTypefaceCompatImpl = new TypefaceCompatApi28Impl();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sTypefaceCompatImpl = new TypefaceCompatApi26Impl();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && TypefaceCompatApi24Impl.isUsable()) {
            sTypefaceCompatImpl = new TypefaceCompatApi24Impl();
        } else {
            sTypefaceCompatImpl = new TypefaceCompatApi21Impl();
        }
    }

    /**
     * Cache for Typeface objects dynamically loaded from assets,
     * keyed by {@link #createResourceUid(Resources, int, String, int, int)}
     */
    private static final LruCache<String, Typeface> sTypefaceCache = new LruCache<>(16);

    private TypefaceCompat() {}

    /**
     * Find from internal cache.
     *
     * @return null if not found.
     */
    @RestrictTo(LIBRARY)
    public static @Nullable Typeface findFromCache(@NonNull Resources resources, int id,
            @Nullable String path, int cookie, int style) {
        return sTypefaceCache.get(createResourceUid(resources, id, path, cookie, style));
    }

    /**
     * Find from internal cache.
     *
     * @return null if not found.
     * @deprecated Use {@link #findFromCache(Resources, int, String, int, int)} method
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Deprecated
    public static @Nullable Typeface findFromCache(@NonNull Resources resources, int id,
            int style) {
        return findFromCache(resources, id, null, 0, style);
    }

    /**
     * Create a unique id for a given Resource and id.
     *
     * @param resources Resources instance
     * @param id a resource id
     * @param style style to be used for this resource, -1 if not available.
     * @return Unique id for a given resource and id.
     */
    private static String createResourceUid(final Resources resources, int id, String path,
            int cookie, int style) {
        return resources.getResourcePackageName(id)
                + '-'
                + path
                + '-'
                + cookie
                + '-'
                + id
                + '-'
                + style;
    }

    private static Paint sCachedPaint = null;
    /**
     * Heuristics used for guessing the primary font in the typeface.
     * There is no API to obtain underlying font families and fonts from the typeface instance.
     * To get the primary font, query the whitespace letter (U+0020) to the text shaper based on the
     * fact that most of the font supports whitespace.
     */
    private static final String REFERENCE_CHAR_FOR_PRIMARY_FONT = " ";

    /**
     * This function guesses the primary font for the given typeface.
     *
     * Returns null if font cannot be retrieved.
     */
    @RestrictTo(LIBRARY)
    @Nullable
    @RequiresApi(31)
    public static Font guessPrimaryFont(@Nullable Typeface typeface) {
        if (sCachedPaint == null) {
            sCachedPaint = new Paint();
        }
        sCachedPaint.setTextSize(10f);
        sCachedPaint.setTypeface(typeface);
        PositionedGlyphs glyphs = TextRunShaper.shapeTextRun(
                REFERENCE_CHAR_FOR_PRIMARY_FONT,  // text
                0, REFERENCE_CHAR_FOR_PRIMARY_FONT.length(),  // range
                0, REFERENCE_CHAR_FOR_PRIMARY_FONT.length(),  // context range
                0f, 0f,  // position. (we don't care in this context)
                false,  // LTR. (we don't care in this context)
                sCachedPaint);
        if (glyphs.glyphCount() == 0) {
            return null;
        }
        return glyphs.getFont(0);
    }

    /**
     * Returns Typeface if the system has the font family with the name [familyName]. For example
     * querying with "sans-serif" would check if the "sans-serif" family is defined in the system
     * and return the Typeface if so.
     *
     * @param familyName The name of the font family.
     */
    @RestrictTo(LIBRARY)
    @Nullable
    public static Typeface getSystemFontFamily(@Nullable String familyName) {
        if (familyName == null || familyName.isEmpty()) return null;
        Typeface typeface = Typeface.create(familyName, Typeface.NORMAL);
        Typeface defaultTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        return typeface != null && !typeface.equals(defaultTypeface) ? typeface : null;
    }

    /**
     * Gets or constructs a Typeface from the system installed font families.
     *
     * This function first tries to find the fully fallback compatible system font family which is
     * specified with the [fontProviderSystemFontFamily] attribute in the [font-family] element
     * in XML.
     * If not found, this function tries to construct the fallback Typeface instance from the
     * specified [fallback] elements in XML.
     * If each [fallback] element can be constructed from the system font, this function will
     * return the newly built Typeface which has fallback as specified in the XML.
     * If even one fallback cannot be constructed from the system font, this function will
     * return null, indicating that an asynchronous font fetch is required.
     *
     * @param entry The font provider entry.
     * @return The Typeface if it can be constructed from system fonts, otherwise null.
     */
    @Nullable
    private static Typeface getSystemFontFamilyWithFallback(@NonNull ProviderResourceEntry entry) {
        final String familyName = entry.getSystemFontFamilyName();

        // If fully compatible system font family is installed, just use it.
        if (!TextUtils.isEmpty(familyName)) {
            Typeface typeface = getSystemFontFamily(familyName);
            if (typeface != null) {
                return typeface;
            }
        }

        // No fully compatible system font found, try to construct the fallbacks.
        final List<FontRequest> requests = entry.getRequests();

        // Trivial Case: Single fallback.
        if (requests.size() == 1) {
            // getSystemFontFamily returns null for null input.
            return getSystemFontFamily(requests.get(0).getSystemFont());
        }

        // To identify main font, need TextRunShaper API which is available from API 31
        if (Build.VERSION.SDK_INT < 31) {
            return null;
        }

        // We can create Typeface synchronously only when the all system fonts are available.
        for (int i = 0; i < requests.size(); ++i) {
            if (getSystemFontFamily(requests.get(i).getSystemFont()) == null) {
                // If the system font is not available, need to query to font provider. Therefore
                // don't instantiate synchronously.
                return null;
            }
        }

        // Main loop of making fallback Typeface from system font.
        Typeface.CustomFallbackBuilder builder = null;
        for (int i = 0; i < requests.size(); ++i) {
            FontRequest fr = requests.get(i);

            // Keeping family name instead of getting font file because the last fallback font
            // family can be specified as the system font fallback with the [setSystemFallback] API.
            // This API can be used only when the font variation settings is not specified
            if (i == requests.size() - 1 && TextUtils.isEmpty(fr.getVariationSettings())) {
                builder.setSystemFallback(fr.getSystemFont());
                break;
            }

            // We already checked system has the specified font, so getSystemFontFamily won't
            // return null here.
            final Font font = guessPrimaryFont(getSystemFontFamily(fr.getSystemFont()));

            if (font == null) {
                Log.w(TAG, "Unable identify the primary font for " + fr.getSystemFont() + "."
                        + " Falling back to provider font.");
                return null;
            }

            FontFamily family;
            if (TextUtils.isEmpty(fr.getVariationSettings())) {
                try {
                    family = new FontFamily.Builder(
                            new Font.Builder(font).setFontVariationSettings(
                                    fr.getVariationSettings()).build()).build();
                } catch (IOException e) {
                    // This unlikely happen since the font is already opened and mmaped.
                    Log.e(TAG, "Failed to clone Font instance. Fall back to provider font.");
                    return null;
                }
            } else {
                family = new FontFamily.Builder(font).build();
            }

            if (builder == null) {
                builder = new Typeface.CustomFallbackBuilder(family);
            } else {
                builder.addCustomFallback(family);
            }
        }
        return builder.build();
    }

    /**
     * Create Typeface from XML resource which root node is font-family.
     *
     * @return null if failed to create.
     */
    @RestrictTo(LIBRARY)
    public static @Nullable Typeface createFromResourcesFamilyXml(
            @NonNull Context context, @NonNull FamilyResourceEntry entry,
            @NonNull Resources resources, int id, @Nullable String path,
            int cookie, int style,
            ResourcesCompat.@Nullable FontCallback fontCallback, @Nullable Handler handler,
            boolean isRequestFromLayoutInflator) {
        Typeface typeface;
        if (entry instanceof ProviderResourceEntry) {
            ProviderResourceEntry providerEntry = (ProviderResourceEntry) entry;

            Typeface fontFamilyTypeface = getSystemFontFamilyWithFallback(providerEntry);
            if (fontFamilyTypeface != null) {
                if (fontCallback != null) {
                    fontCallback.callbackSuccessAsync(fontFamilyTypeface, handler);
                }
                sTypefaceCache.put(createResourceUid(resources, id, path, cookie, style),
                        fontFamilyTypeface);
                return fontFamilyTypeface;
            }

            final boolean isBlocking = isRequestFromLayoutInflator
                    ? providerEntry.getFetchStrategy()
                    == FontResourcesParserCompat.FETCH_STRATEGY_BLOCKING
                    : fontCallback == null;
            final int timeout = isRequestFromLayoutInflator ? providerEntry.getTimeout()
                    : FontResourcesParserCompat.INFINITE_TIMEOUT_VALUE;

            Handler newHandler = ResourcesCompat.FontCallback.getHandler(handler);
            ResourcesCallbackAdapter newCallback = new ResourcesCallbackAdapter(fontCallback);
            List<FontRequest> requests = providerEntry.getRequests();
            typeface = FontsContractCompat.requestFont(context, requests,
                    style, isBlocking, timeout, newHandler, newCallback);
        } else {
            typeface = sTypefaceCompatImpl.createFromFontFamilyFilesResourceEntry(
                    context, (FontFamilyFilesResourceEntry) entry, resources, style);
            if (fontCallback != null) {
                if (typeface != null) {
                    fontCallback.callbackSuccessAsync(typeface, handler);
                } else {
                    fontCallback.callbackFailAsync(
                            FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR,
                            handler);
                }
            }
        }
        if (typeface != null) {
            sTypefaceCache.put(createResourceUid(resources, id, path, cookie, style), typeface);
        }
        return typeface;
    }

    /**
     * Create Typeface from XML resource which root node is font-family.
     *
     * @return null if failed to create.
     * @deprecated Use {@link #createFromResourcesFamilyXml(Context, FamilyResourceEntry,
     * Resources, int, String, int, int, ResourcesCompat.FontCallback, Handler, boolean)} method
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Deprecated
    public static @Nullable Typeface createFromResourcesFamilyXml(
            @NonNull Context context, @NonNull FamilyResourceEntry entry,
            @NonNull Resources resources, int id, int style,
            ResourcesCompat.@Nullable FontCallback fontCallback, @Nullable Handler handler,
            boolean isRequestFromLayoutInflator) {
        return createFromResourcesFamilyXml(context, entry, resources, id, null, 0, style,
                fontCallback, handler, isRequestFromLayoutInflator);
    }

    /**
     * Used by Resources to load a font resource of type font file.
     */
    @RestrictTo(LIBRARY)
    public static @Nullable Typeface createFromResourcesFontFile(
            @NonNull Context context, @NonNull Resources resources, int id, String path, int cookie,
            int style) {
        Typeface typeface = sTypefaceCompatImpl.createFromResourcesFontFile(
                context, resources, id, path, style);
        if (typeface != null) {
            final String resourceUid = createResourceUid(resources, id, path, cookie, style);
            sTypefaceCache.put(resourceUid, typeface);
        }
        return typeface;
    }

    /**
     * Used by Resources to load a font resource of type font file.
     * @deprecated Use {@link #createFromResourcesFontFile(Context, Resources, int, String,
     * int, int)} method
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Deprecated
    public static @Nullable Typeface createFromResourcesFontFile(
            @NonNull Context context, @NonNull Resources resources, int id, String path,
            int style) {
        return createFromResourcesFontFile(context, resources, id, path, 0, style);
    }

    /**
     * Create a Typeface from a given FontInfo list and a map that matches them to ByteBuffers.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static @Nullable Typeface createFromFontInfo(@NonNull Context context,
            @Nullable CancellationSignal cancellationSignal, FontInfo @NonNull [] fonts,
            int style) {
        if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
            Trace.beginSection("TypefaceCompat.createFromFontInfo");
        }
        try {
            return sTypefaceCompatImpl.createFromFontInfo(context, cancellationSignal, fonts,
                    style);
        } finally {
            if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
                Trace.endSection();
            }
        }
    }

    /**
     * Create a Typeface from a given list of FontInfo lists.
     * <p>
     * This currently throws an exception if used below API 29.
     */
    @RestrictTo(LIBRARY)
    @RequiresApi(29)
    public static @Nullable Typeface createFromFontInfoWithFallback(@NonNull Context context,
            @Nullable CancellationSignal cancellationSignal, @NonNull List<FontInfo[]> fonts,
            int style) {
        if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
            Trace.beginSection("TypefaceCompat.createFromFontInfoWithFallback");
        }
        try {
            return sTypefaceCompatImpl.createFromFontInfoWithFallback(
                    context, cancellationSignal, fonts,
                    style);
        } finally {
            if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
                Trace.endSection();
            }
        }
    }

    /**
     * Retrieves the best matching typeface given the family, style and context.
     * If null is passed for the family, then the "default" font will be chosen.
     *
     * @param family The font family. May be null.
     * @param style  The style of the typeface. e.g. NORMAL, BOLD, ITALIC, BOLD_ITALIC
     * @param context The context used to retrieve the font.
     * @return The best matching typeface.
     */
    public static @NonNull Typeface create(final @NonNull Context context,
            final @Nullable Typeface family, final int style) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        return Typeface.create(family, style);
    }

    /**
     * Creates a typeface object that best matches the specified existing typeface and the specified
     * weight and italic style
     * <p>Below are numerical values and corresponding common weight names.</p>
     * <table>
     * <thead>
     * <tr><th>Value</th><th>Common weight name</th></tr>
     * </thead>
     * <tbody>
     * <tr><td>100</td><td>Thin</td></tr>
     * <tr><td>200</td><td>Extra Light</td></tr>
     * <tr><td>300</td><td>Light</td></tr>
     * <tr><td>400</td><td>Normal</td></tr>
     * <tr><td>500</td><td>Medium</td></tr>
     * <tr><td>600</td><td>Semi Bold</td></tr>
     * <tr><td>700</td><td>Bold</td></tr>
     * <tr><td>800</td><td>Extra Bold</td></tr>
     * <tr><td>900</td><td>Black</td></tr>
     * </tbody>
     * </table>
     *
     * <p>
     * This method is thread safe.
     * </p>
     *
     * @param context context to use for the creation.
     * @param family An existing {@link Typeface} object. In case of {@code null}, the default
     *               typeface is used instead.
     * @param weight The desired weight to be drawn.
     * @param italic {@code true} if italic style is desired to be drawn. Otherwise, {@code false}
     * @return A {@link Typeface} object for drawing specified weight and italic style. Never
     *         returns {@code null}
     *
     * @see Typeface#getWeight()
     * @see Typeface#isItalic()
     */
    public static @NonNull Typeface create(@NonNull Context context, @Nullable Typeface family,
            @IntRange(from = 1, to = 1000) int weight, boolean italic) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        Preconditions.checkArgumentInRange(weight, 1, 1000, "weight");
        if (family == null) {
            family = Typeface.DEFAULT;
        }
        return sTypefaceCompatImpl.createWeightStyle(context, family, weight, italic);
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @VisibleForTesting
    public static void clearCache() {
        sTypefaceCache.evictAll();
    }

    /**
     * Converts {@link androidx.core.provider.FontsContractCompat.FontRequestCallback} callback
     * functions into {@link androidx.core.content.res.ResourcesCompat.FontCallback} equivalents.
     *
     * RestrictTo(LIBRARY) since it is used by the deprecated
     * {@link FontsContractCompat#getFontSync} function.
     *
     */
    @RestrictTo(LIBRARY)
    public static class ResourcesCallbackAdapter extends FontsContractCompat.FontRequestCallback {
        private ResourcesCompat.@Nullable FontCallback mFontCallback;

        public ResourcesCallbackAdapter(ResourcesCompat.@Nullable FontCallback fontCallback) {
            mFontCallback = fontCallback;
        }

        @Override
        public void onTypefaceRetrieved(@NonNull Typeface typeface) {
            if (mFontCallback != null) {
                mFontCallback.onFontRetrieved(typeface);
            }
        }

        @Override
        public void onTypefaceRequestFailed(int reason) {
            if (mFontCallback != null) {
                mFontCallback.onFontRetrievalFailed(reason);
            }
        }
    }

    static {
        Trace.endSection();
    }
}
