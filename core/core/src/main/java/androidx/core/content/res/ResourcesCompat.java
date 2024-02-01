/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.core.content.res;

import static android.os.Build.VERSION.SDK_INT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.content.res.XmlResourceParser;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;

import androidx.annotation.AnyRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DoNotInline;
import androidx.annotation.DrawableRes;
import androidx.annotation.FontRes;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.provider.FontsContractCompat.FontRequestCallback;
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FontRequestFailReason;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * Helper for accessing features in {@link Resources}.
 */
@SuppressWarnings("unused")
public final class ResourcesCompat {
    private static final String TAG = "ResourcesCompat";
    private static final ThreadLocal<TypedValue> sTempTypedValue = new ThreadLocal<>();

    @GuardedBy("sColorStateCacheLock")
    private static final WeakHashMap<ColorStateListCacheKey, SparseArray<ColorStateListCacheEntry>>
            sColorStateCaches = new WeakHashMap<>(0);
    private static final Object sColorStateCacheLock = new Object();

    /**
     * The {@code null} resource ID. This denotes an invalid resource ID that is returned by the
     * system when a resource is not found or the value is set to {@code @null} in XML.
     */
    @AnyRes
    public static final int ID_NULL = 0;

    /**
     * Clears cached values associated with the specified {@link Theme}.
     * <p>
     * This method allows developers to work around issues related to stale cached resources that
     * may occur on API level 32 and below following a call to
     * {@link Theme#applyStyle(int, boolean)}. If you are not explicitly calling {@code
     * applyStyle} in your code, you probably do not need to call this method.
     * <p>
     * Starting in Android T, the Theme class correctly implements {@link Theme#hashCode()} and
     * cached values will be appropriately cleared when the contents of a Theme object change.
     *
     * @param theme the theme for which associated values should be cleared from the resource cache
     */
    public static void clearCachesForTheme(@NonNull Theme theme) {
        synchronized (sColorStateCacheLock) {
            Iterator<ColorStateListCacheKey> keys = sColorStateCaches.keySet().iterator();
            while (keys.hasNext()) {
                ColorStateListCacheKey key = keys.next();
                if (key != null && theme.equals(key.mTheme)) {
                    keys.remove();
                }
            }
        }
    }

    /**
     * Return a drawable object associated with a particular resource ID and
     * styled for the specified theme. Various types of objects will be
     * returned depending on the underlying resource -- for example, a solid
     * color, PNG image, scalable image, etc.
     * <p>
     * Prior to API level 21, the theme will not be applied and this method
     * simply calls through to {@link Resources#getDrawable(int)}.
     *
     * @param res   resources to use for getting the drawable.
     * @param id    The desired resource identifier, as generated by the aapt
     *              tool. This integer encodes the package, type, and resource
     *              entry. The value 0 is an invalid identifier.
     * @param theme The theme used to style the drawable attributes, may be
     *              {@code null}.
     * @return Drawable An object that can be used to draw this resource.
     * @throws NotFoundException Throws NotFoundException if the given ID does
     *                           not exist.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static Drawable getDrawable(@NonNull Resources res, @DrawableRes int id,
            @Nullable Theme theme) throws NotFoundException {
        if (SDK_INT >= 21) {
            return Api21Impl.getDrawable(res, id, theme);
        } else {
            return res.getDrawable(id);
        }
    }


    /**
     * Return a drawable object associated with a particular resource ID for
     * the given screen density in DPI and styled for the specified theme.
     * <p>
     * Prior to API level 15, the theme and density will not be applied and
     * this method simply calls through to {@link Resources#getDrawable(int)}.
     * <p>
     * Prior to API level 21, the theme will not be applied and this method
     * calls through to Resources#getDrawableForDensity(int, int).
     *
     * @param res resources to use for getting the drawable.
     * @param id      The desired resource identifier, as generated by the aapt
     *                tool. This integer encodes the package, type, and resource
     *                entry. The value 0 is an invalid identifier.
     * @param density The desired screen density indicated by the resource as
     *                found in {@link DisplayMetrics}.
     * @param theme   The theme used to style the drawable attributes, may be
     *                {@code null}.
     * @return Drawable An object that can be used to draw this resource.
     * @throws NotFoundException Throws NotFoundException if the given ID does
     *                           not exist.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static Drawable getDrawableForDensity(@NonNull Resources res, @DrawableRes int id,
            int density, @Nullable Theme theme) throws NotFoundException {
        if (SDK_INT >= 21) {
            return Api21Impl.getDrawableForDensity(res, id, density, theme);
        } else {
            return res.getDrawableForDensity(id, density);
        }
    }

    /**
     * Returns a themed color integer associated with a particular resource ID.
     * If the resource holds a complex {@link ColorStateList}, then the default
     * color from the set is returned.
     * <p>
     * Prior to API level 23, the theme will not be applied and this method
     * calls through to {@link Resources#getColor(int)}.
     *
     * @param res resources to use for getting the color.
     * @param id    The desired resource identifier, as generated by the aapt
     *              tool. This integer encodes the package, type, and resource
     *              entry. The value 0 is an invalid identifier.
     * @param theme The theme used to style the color attributes, may be
     *              {@code null}.
     * @return A single color value in the form {@code 0xAARRGGBB}.
     * @throws NotFoundException Throws NotFoundException if the given ID does
     *                           not exist.
     */
    @ColorInt
    @SuppressWarnings("deprecation")
    public static int getColor(@NonNull Resources res, @ColorRes int id, @Nullable Theme theme)
            throws NotFoundException {
        if (SDK_INT >= 23) {
            return ResourcesCompat.Api23Impl.getColor(res, id, theme);
        } else {
            return res.getColor(id);
        }
    }

    /**
     * Returns a themed color state list associated with a particular resource
     * ID. The resource may contain either a single raw color value or a
     * complex {@link ColorStateList} holding multiple possible colors.
     *
     * @param res resources to use for getting the color state list.
     * @param id    The desired resource identifier of a {@link ColorStateList},
     *              as generated by the aapt tool. This integer encodes the
     *              package, type, and resource entry. The value 0 is an invalid
     *              identifier.
     * @param theme The theme used to style the color attributes, may be
     *              {@code null}.
     * @return A themed ColorStateList object containing either a single solid
     * color or multiple colors that can be selected based on a state.
     * @throws NotFoundException Throws NotFoundException if the given ID does
     *                           not exist.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static ColorStateList getColorStateList(@NonNull Resources res, @ColorRes int id,
            @Nullable Theme theme) throws NotFoundException {
        // We explicitly do not attempt to use the platform Resources impl on S+
        // in case the CSL is using only app:lStar

        // First, try and handle the inflation ourselves
        ColorStateListCacheKey key = new ColorStateListCacheKey(res, theme);
        ColorStateList csl = getCachedColorStateList(key, id);
        if (csl != null) {
            return csl;
        }
        // Cache miss, so try and inflate it ourselves
        csl = inflateColorStateList(res, id, theme);
        if (csl != null) {
            // If we inflated it, add it to the cache and return
            addColorStateListToCache(key, id, csl, theme);
            return csl;
        }
        // If we reach here then we couldn't inflate it, so let the framework handle it
        if (SDK_INT >= 23) {
            return ResourcesCompat.Api23Impl.getColorStateList(res, id, theme);
        } else {
            return res.getColorStateList(id);
        }
    }

    /**
     * Inflates a {@link ColorStateList} from resources, honouring theme attributes.
     */
    @Nullable
    private static ColorStateList inflateColorStateList(Resources resources, int resId,
            @Nullable Theme theme) {
        if (isColorInt(resources, resId)) {
            // The resource is a color int, we can't handle it so return null
            return null;
        }
        final XmlPullParser xml = resources.getXml(resId);
        try {
            return ColorStateListInflaterCompat.createFromXml(resources, xml, theme);
        } catch (Exception e) {
            Log.w(TAG, "Failed to inflate ColorStateList, leaving it to the framework", e);
        }
        return null;
    }

    @Nullable
    private static ColorStateList getCachedColorStateList(@NonNull ColorStateListCacheKey key,
            @ColorRes int resId) {
        synchronized (sColorStateCacheLock) {
            final SparseArray<ColorStateListCacheEntry> entries = sColorStateCaches.get(key);
            if (entries != null && entries.size() > 0) {
                final ColorStateListCacheEntry entry = entries.get(resId);
                if (entry != null) {
                    if (entry.mConfiguration.equals(key.mResources.getConfiguration())
                            && ((key.mTheme == null && entry.mThemeHash == 0)
                            || (key.mTheme != null && entry.mThemeHash == key.mTheme.hashCode()))) {
                        // If the current configuration matches the entry's, we can use it
                        return entry.mValue;
                    } else {
                        // Otherwise we'll remove the entry
                        entries.remove(resId);
                    }
                }
            }
        }
        return null;
    }

    private static void addColorStateListToCache(@NonNull ColorStateListCacheKey key,
            @ColorRes int resId,
            @NonNull ColorStateList value,
            @Nullable Theme theme) {
        synchronized (sColorStateCacheLock) {
            SparseArray<ColorStateListCacheEntry> entries = sColorStateCaches.get(key);
            if (entries == null) {
                entries = new SparseArray<>();
                sColorStateCaches.put(key, entries);
            }
            entries.append(resId, new ColorStateListCacheEntry(value,
                    key.mResources.getConfiguration(), theme));
        }
    }

    private static boolean isColorInt(@NonNull Resources resources, @ColorRes int resId) {
        final TypedValue value = getTypedValue();
        resources.getValue(resId, value, true);
        return value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

    @NonNull
    private static TypedValue getTypedValue() {
        TypedValue tv = sTempTypedValue.get();
        if (tv == null) {
            tv = new TypedValue();
            sTempTypedValue.set(tv);
        }
        return tv;
    }

    private static final class ColorStateListCacheKey {
        final Resources mResources;
        final Theme mTheme;

        ColorStateListCacheKey(@NonNull Resources resources, @Nullable Theme theme) {
            mResources = resources;
            mTheme = theme;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColorStateListCacheKey that = (ColorStateListCacheKey) o;
            return mResources.equals(that.mResources)
                    && ObjectsCompat.equals(mTheme, that.mTheme);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mResources, mTheme);
        }
    }

    private static class ColorStateListCacheEntry {
        final ColorStateList mValue;
        final Configuration mConfiguration;
        final int mThemeHash;

        ColorStateListCacheEntry(@NonNull ColorStateList value,
                @NonNull Configuration configuration,
                @Nullable Theme theme) {
            mValue = value;
            mConfiguration = configuration;
            mThemeHash = theme == null ? 0 : theme.hashCode();
        }
    }

    /**
     * Retrieve a floating-point value for a particular resource ID.
     *
     * @param res resources to use for getting the value.
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     * @return Returns the floating-point value contained in the resource.
     * @throws NotFoundException Throws NotFoundException if the given ID does
     *                           not exist or is not a floating-point value.
     */
    public static float getFloat(@NonNull Resources res, @DimenRes int id) {
        if (SDK_INT >= 29) {
            return Api29Impl.getFloat(res, id);
        }

        TypedValue value = getTypedValue();
        res.getValue(id, value, true);
        if (value.type == TypedValue.TYPE_FLOAT) {
            return value.getFloat();
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id)
                + " type #0x" + Integer.toHexString(value.type) + " is not valid");
    }

    /**
     * Returns a font Typeface associated with a particular resource ID.
     * <p>
     * This method will block the calling thread to retrieve the requested font, including if it
     * is from a font provider. If you wish to not have this behavior, use
     * {@link #getFont(Context, int, FontCallback, Handler)} instead.
     * <p>
     * Prior to API level 23, font resources with more than one font in a family will only load the
     * font closest to a regular weight typeface.
     *
     * @param context A context to retrieve the Resources from.
     * @param id      The desired resource identifier of a {@link Typeface},
     *                as generated by the aapt tool. This integer encodes the
     *                package, type, and resource entry. The value 0 is an invalid
     *                identifier.
     * @return A font Typeface object.
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     * @see #getFont(Context, int, FontCallback, Handler)
     */
    @Nullable
    public static Typeface getFont(@NonNull Context context, @FontRes int id)
            throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, new TypedValue(), Typeface.NORMAL, null /* callback */,
                null /* handler */, false /* isXmlRequest */, false /* isCachedOnly */);
    }

    /**
     * Returns a cached font Typeface associated with a particular resource ID.
     * <p>
     * This method returns non-null Typeface if the requested font is already fetched. Otherwise
     * immediately returns null without requesting to font provider.
     * <p>
     * Prior to API level 23, font resources with more than one font in a family will only load the
     * font closest to a regular weight typeface.
     *
     * @param context A context to retrieve the Resources from.
     * @param id      The desired resource identifier of a {@link Typeface},
     *                as generated by the aapt tool. This integer encodes the
     *                package, type, and resource entry. The value 0 is an invalid
     *                identifier.
     * @return A font Typeface object.
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     * @see #getFont(Context, int, FontCallback, Handler)
     */
    @Nullable
    public static Typeface getCachedFont(@NonNull Context context, @FontRes int id)
            throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, new TypedValue(), Typeface.NORMAL, null /* callback */,
                null /* handler */, false /* isXmlRequest */, true);
    }

    /**
     * Interface used to receive asynchronous font fetching events.
     */
    public abstract static class FontCallback {

        /**
         * Called when an asynchronous font was finished loading.
         *
         * @param typeface The font that was loaded.
         */
        public abstract void onFontRetrieved(@NonNull Typeface typeface);

        /**
         * Called when an asynchronous font failed to load.
         *
         * @param reason The reason the font failed to load. One of
         *               {@link FontRequestFailReason#FAIL_REASON_PROVIDER_NOT_FOUND},
         *               {@link FontRequestFailReason#FAIL_REASON_WRONG_CERTIFICATES},
         *               {@link FontRequestFailReason#FAIL_REASON_FONT_LOAD_ERROR},
         *               {@link FontRequestFailReason#FAIL_REASON_SECURITY_VIOLATION},
         *               {@link FontRequestFailReason#FAIL_REASON_FONT_NOT_FOUND},
         *               {@link FontRequestFailReason#FAIL_REASON_FONT_UNAVAILABLE} or
         *               {@link FontRequestFailReason#FAIL_REASON_MALFORMED_QUERY}.
         */
        public abstract void onFontRetrievalFailed(@FontRequestFailReason int reason);

        /**
         * Call {@link #onFontRetrieved(Typeface)} on the handler given, or the Ui Thread if it is
         * null.
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public final void callbackSuccessAsync(final @NonNull Typeface typeface,
                @Nullable Handler handler) {
            getHandler(handler).post(() -> onFontRetrieved(typeface));
        }

        /**
         * Call {@link #onFontRetrievalFailed(int)} on the handler given, or the Ui Thread if it is
         * null.
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public final void callbackFailAsync(
                @FontRequestFailReason final int reason, @Nullable Handler handler) {
            getHandler(handler).post(() -> onFontRetrievalFailed(reason));
        }

        @RestrictTo(LIBRARY)
        @NonNull
        public static Handler getHandler(@Nullable Handler handler) {
            return handler == null ? new Handler(Looper.getMainLooper()) : handler;
        }
    }

    /**
     * Returns a font Typeface associated with a particular resource ID asynchronously.
     * <p>
     * Prior to API level 23, font resources with more than one font in a family will only load the
     * font closest to a regular weight typeface.
     * </p>
     *
     * @param context      A context to retrieve the Resources from.
     * @param id           The desired resource identifier of a {@link Typeface}, as generated by
     *                    the aapt
     *                     tool. This integer encodes the package, type, and resource entry. The
     *                     value 0 is an
     *                     invalid identifier.
     * @param fontCallback A callback to receive async fetching of this font. The callback will be
     *                     triggered on the UI thread.
     * @param handler      A handler for the thread the callback should be called on. If null, the
     *                     callback will be called on the UI thread.
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     */
    public static void getFont(@NonNull Context context, @FontRes int id,
            @NonNull FontCallback fontCallback, @Nullable Handler handler)
            throws NotFoundException {
        Preconditions.checkNotNull(fontCallback);
        if (context.isRestricted()) {
            fontCallback.callbackFailAsync(
                    FontRequestCallback.FAIL_REASON_SECURITY_VIOLATION, handler);
            return;
        }
        loadFont(context, id, new TypedValue(), Typeface.NORMAL, fontCallback, handler,
                false /* isXmlRequest */, false /* isCacheOnly */);
    }

    /**
     * Used by TintTypedArray.
     *
     */
    @Nullable
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static Typeface getFont(@NonNull Context context, @FontRes int id,
            @NonNull TypedValue value, int style, @Nullable FontCallback fontCallback)
            throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, value, style, fontCallback, null /* handler */,
                true /* isXmlRequest */, false /* isCacheOnly */);
    }

    /**
     * @param context                     The Context to get Resources from
     * @param id                          The Resource id to load
     * @param value                       A TypedValue to use in the fetching
     * @param style                       The font style to load
     * @param fontCallback                A callback to trigger when the font is fetched or an
     *                                    error occurs
     * @param handler                     A handler to the thread the callback should be called on
     * @param isRequestFromLayoutInflator Whether this request originated from XML. This is used to
     *                                    determine if we use or ignore the
     *                                    fontProviderFetchStrategy attribute in
     *                                    font provider XML fonts.
     * @return The font as a Typeface
     * @throws NotFoundException if the resource ID could not be retrieved
     */
    private static Typeface loadFont(@NonNull Context context, int id, @NonNull TypedValue value,
            int style, @Nullable FontCallback fontCallback, @Nullable Handler handler,
            boolean isRequestFromLayoutInflator, boolean isCachedOnly) {
        final Resources resources = context.getResources();
        resources.getValue(id, value, true);
        Typeface typeface = loadFont(context, resources, value, id, style, fontCallback, handler,
                isRequestFromLayoutInflator, isCachedOnly);
        if (typeface == null && fontCallback == null && !isCachedOnly) {
            throw new NotFoundException("Font resource ID #0x"
                    + Integer.toHexString(id) + " could not be retrieved.");
        }
        return typeface;
    }

    /**
     * Load the given font. This method will always return null for asynchronous requests, which
     * provide a fontCallback, as there is no immediate result. When the callback is not provided,
     * the request is treated as synchronous and fails if async loading is required.
     *
     * @param context                     The Context to get Resources from
     * @param id                          The Resource id to load
     * @param value                       A TypedValue to use in the fetching
     * @param style                       The font style to load
     * @param fontCallback                A callback to trigger when the font is fetched or an
     *                                    error occurs
     * @param handler                     A handler to the thread the callback should be called on
     * @param isRequestFromLayoutInflator Whether this request originated from XML. This is used to
     *                                    determine if we use or ignore the
     *                                    fontProviderFetchStrategy attribute in
     *                                    font provider XML fonts.
     */
    private static Typeface loadFont(
            @NonNull Context context, Resources wrapper, @NonNull TypedValue value, int id,
            int style, @Nullable FontCallback fontCallback, @Nullable Handler handler,
            boolean isRequestFromLayoutInflator, boolean isCachedOnly) {
        if (value.string == null) {
            throw new NotFoundException("Resource \"" + wrapper.getResourceName(id) + "\" ("
                    + Integer.toHexString(id) + ") is not a Font: " + value);
        }

        final String file = value.string.toString();
        if (!file.startsWith("res/")) {
            // Early exit if the specified string is unlikely to be a resource path.
            if (fontCallback != null) {
                fontCallback.callbackFailAsync(
                        FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR, handler);
            }
            return null;
        }
        Typeface typeface = TypefaceCompat.findFromCache(wrapper, id, file, value.assetCookie,
                style);

        if (typeface != null) {
            if (fontCallback != null) {
                fontCallback.callbackSuccessAsync(typeface, handler);
            }
            return typeface;
        } else if (isCachedOnly) {
            return null;
        }

        try {
            if (file.toLowerCase().endsWith(".xml")) {
                final XmlResourceParser rp = wrapper.getXml(id);
                final FamilyResourceEntry familyEntry =
                        FontResourcesParserCompat.parse(rp, wrapper);
                if (familyEntry == null) {
                    Log.e(TAG, "Failed to find font-family tag");
                    if (fontCallback != null) {
                        fontCallback.callbackFailAsync(
                                FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR, handler);
                    }
                    return null;
                }
                return TypefaceCompat.createFromResourcesFamilyXml(context, familyEntry, wrapper,
                        id, file, value.assetCookie, style, fontCallback, handler,
                        isRequestFromLayoutInflator);
            }
            typeface = TypefaceCompat.createFromResourcesFontFile(
                    context, wrapper, id, file, value.assetCookie, style);
            if (fontCallback != null) {
                if (typeface != null) {
                    fontCallback.callbackSuccessAsync(typeface, handler);
                } else {
                    fontCallback.callbackFailAsync(
                            FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR, handler);
                }
            }
            return typeface;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Failed to parse xml resource " + file, e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read xml resource " + file, e);
        }
        if (fontCallback != null) {
            fontCallback.callbackFailAsync(
                    FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR, handler);
        }
        return null;
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static float getFloat(@NonNull Resources res, @DimenRes int id) {
            return res.getFloat(id);
        }
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        @NonNull
        static ColorStateList getColorStateList(@NonNull Resources res, @ColorRes int id,
                @Nullable Theme theme) {
            return res.getColorStateList(id, theme);
        }

        @DoNotInline
        static int getColor(Resources resources, int id, Theme theme) {
            return resources.getColor(id, theme);
        }
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Drawable getDrawable(Resources resources, int id, Theme theme) {
            return resources.getDrawable(id, theme);
        }

        @DoNotInline
        static Drawable getDrawableForDensity(Resources resources, int id, int density,
                Theme theme) {
            return resources.getDrawableForDensity(id, density, theme);
        }
    }

    private ResourcesCompat() {
    }

    /**
     * Provides backward-compatible implementations for new {@link Theme} APIs.
     */
    public static final class ThemeCompat {
        private ThemeCompat() {
        }

        /**
         * Rebases the theme against the parent Resource object's current configuration by
         * re-applying the styles passed to {@link Theme#applyStyle(int, boolean)}.
         * <p>
         * Compatibility behavior:
         * <ul>
         * <li>API 29 and above, this method matches platform behavior.
         * <li>API 23 through 28, this method attempts to match platform behavior by calling into
         *     hidden platform APIs, but is not guaranteed to succeed.
         * <li>API 22 and earlier, this method does nothing.
         * </ul>
         *
         * @param theme the theme to rebase
         */
        public static void rebase(@NonNull Theme theme) {
            if (SDK_INT >= 29) {
                Api29Impl.rebase(theme);
            } else if (SDK_INT >= 23) {
                Api23Impl.rebase(theme);
            }
        }

        @RequiresApi(29)
        static class Api29Impl {
            private Api29Impl() {
                // This class is not instantiable.
            }

            @DoNotInline
            static void rebase(@NonNull Theme theme) {
                theme.rebase();
            }
        }

        @RequiresApi(23)
        static class Api23Impl {
            private Api23Impl() {
                // This class is not instantiable.
            }

            private static final Object sRebaseMethodLock = new Object();

            private static Method sRebaseMethod;
            private static boolean sRebaseMethodFetched;

            @SuppressLint("BanUncheckedReflection") // @RequiresApiRange(min=23, max=29)
            static void rebase(@NonNull Theme theme) {
                synchronized (sRebaseMethodLock) {
                    if (!sRebaseMethodFetched) {
                        try {
                            sRebaseMethod = Theme.class.getDeclaredMethod("rebase");
                            sRebaseMethod.setAccessible(true);
                        } catch (NoSuchMethodException e) {
                            Log.i(TAG, "Failed to retrieve rebase() method", e);
                        }
                        sRebaseMethodFetched = true;
                    }
                    if (sRebaseMethod != null) {
                        try {
                            sRebaseMethod.invoke(theme);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            Log.i(TAG, "Failed to invoke rebase() method via reflection", e);
                            sRebaseMethod = null;
                        }
                    }
                }
            }
        }
    }
}
