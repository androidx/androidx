/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat;
import androidx.appcompat.resources.Compatibility;
import androidx.appcompat.resources.R;
import androidx.collection.LongSparseArray;
import androidx.collection.LruCache;
import androidx.collection.SimpleArrayMap;
import androidx.collection.SparseArrayCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public final class ResourceManagerInternal {
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public interface ResourceManagerHooks {
        @Nullable
        Drawable createDrawableFor(@NonNull ResourceManagerInternal appCompatDrawableManager,
                @NonNull Context context, @DrawableRes final int resId);
        boolean tintDrawable(@NonNull Context context, @DrawableRes int resId,
                @NonNull Drawable drawable);
        @Nullable
        ColorStateList getTintListForDrawableRes(@NonNull Context context, @DrawableRes int resId);
        boolean tintDrawableUsingColorFilter(@NonNull Context context,
                @DrawableRes final int resId, @NonNull Drawable drawable);
        @Nullable
        PorterDuff.Mode getTintModeForDrawableRes(final int resId);
    }

    private interface InflateDelegate {
        Drawable createFromXmlInner(@NonNull Context context, @NonNull XmlPullParser parser,
                @NonNull AttributeSet attrs, @Nullable Resources.Theme theme);
    }

    private static final String TAG = "ResourceManagerInternal";
    private static final boolean DEBUG = false;
    private static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;
    private static final String SKIP_DRAWABLE_TAG = "appcompat_skip_skip";

    private static final String PLATFORM_VD_CLAZZ = "android.graphics.drawable.VectorDrawable";

    private static ResourceManagerInternal INSTANCE;

    /**
     * Returns the singleton instance of this class.
     */
    public static synchronized ResourceManagerInternal get() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceManagerInternal();
            installDefaultInflateDelegates(INSTANCE);
        }
        return INSTANCE;
    }

    private static void installDefaultInflateDelegates(@NonNull ResourceManagerInternal manager) {
        // This sdk version check will affect src:appCompat code path.
        // Although VectorDrawable exists in Android framework from Lollipop, AppCompat will use
        // (Animated)VectorDrawableCompat before Nougat to utilize bug fixes & feature backports.
        if (Build.VERSION.SDK_INT < 24) {
            manager.addDelegate("vector", new VdcInflateDelegate());
            manager.addDelegate("animated-vector", new AvdcInflateDelegate());
            manager.addDelegate("animated-selector", new AsldcInflateDelegate());
            manager.addDelegate("drawable", new DrawableDelegate());
        }
    }

    private static final ColorFilterLruCache COLOR_FILTER_CACHE = new ColorFilterLruCache(6);

    private WeakHashMap<Context, SparseArrayCompat<ColorStateList>> mTintLists;
    private SimpleArrayMap<String, InflateDelegate> mDelegates;
    private SparseArrayCompat<String> mKnownDrawableIdTags;

    private final WeakHashMap<Context, LongSparseArray<WeakReference<ConstantState>>>
            mDrawableCaches = new WeakHashMap<>(0);

    private TypedValue mTypedValue;

    private boolean mHasCheckedVectorDrawableSetup;

    private ResourceManagerHooks mHooks;

    public synchronized void setHooks(ResourceManagerHooks hooks) {
        mHooks = hooks;
    }

    public synchronized Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        return getDrawable(context, resId, false);
    }

    synchronized Drawable getDrawable(@NonNull Context context, @DrawableRes int resId,
            boolean failIfNotKnown) {
        checkVectorDrawableSetup(context);

        Drawable drawable = loadDrawableFromDelegates(context, resId);
        if (drawable == null) {
            drawable = createDrawableIfNeeded(context, resId);
        }
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(context, resId);
        }

        if (drawable != null) {
            // Tint it if needed
            drawable = tintDrawable(context, resId, failIfNotKnown, drawable);
        }
        if (drawable != null) {
            // See if we need to 'fix' the drawable
            DrawableUtils.fixDrawable(drawable);
        }
        return drawable;
    }

    public synchronized void onConfigurationChanged(@NonNull Context context) {
        LongSparseArray<WeakReference<ConstantState>> cache = mDrawableCaches.get(context);
        if (cache != null) {
            // Crude, but we'll just clear the cache when the configuration changes
            cache.clear();
        }
    }

    private static long createCacheKey(TypedValue tv) {
        return (((long) tv.assetCookie) << 32) | tv.data;
    }

    private Drawable createDrawableIfNeeded(@NonNull Context context,
            @DrawableRes final int resId) {
        if (mTypedValue == null) {
            mTypedValue = new TypedValue();
        }
        final TypedValue tv = mTypedValue;
        context.getResources().getValue(resId, tv, true);
        final long key = createCacheKey(tv);

        Drawable dr = getCachedDrawable(context, key);
        if (dr != null) {
            // If we got a cached drawable, return it
            return dr;
        }

        // Else we need to try and create one...
        dr = (this.mHooks == null) ? null
            : this.mHooks.createDrawableFor(this, context, resId);

        if (dr != null) {
            dr.setChangingConfigurations(tv.changingConfigurations);
            // If we reached here then we created a new drawable, add it to the cache
            addDrawableToCache(context, key, dr);
        }

        return dr;
    }

    private Drawable tintDrawable(@NonNull Context context, @DrawableRes int resId,
            boolean failIfNotKnown, @NonNull Drawable drawable) {
        final ColorStateList tintList = getTintList(context, resId);
        if (tintList != null) {
            // First mutate the Drawable, then wrap it and set the tint list
            drawable = drawable.mutate();
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(drawable, tintList);

            // If there is a blending mode specified for the drawable, use it
            final PorterDuff.Mode tintMode = getTintMode(resId);
            if (tintMode != null) {
                DrawableCompat.setTintMode(drawable, tintMode);
            }
        } else if ((mHooks != null) && mHooks.tintDrawable(context, resId, drawable)) {
            // If we're here, the installed hooks reported successful tinting of the
            // specific drawable
        } else {
            final boolean tinted = tintDrawableUsingColorFilter(context, resId, drawable);
            if (!tinted && failIfNotKnown) {
                // If we didn't tint using a ColorFilter, and we're set to fail if we don't
                // know the id, return null
                drawable = null;
            }
        }
        return drawable;
    }

    private Drawable loadDrawableFromDelegates(@NonNull Context context, @DrawableRes int resId) {
        if (mDelegates != null && !mDelegates.isEmpty()) {
            if (mKnownDrawableIdTags != null) {
                final String cachedTagName = mKnownDrawableIdTags.get(resId);
                if (SKIP_DRAWABLE_TAG.equals(cachedTagName)
                        || (cachedTagName != null && mDelegates.get(cachedTagName) == null)) {
                    // If we don't have a delegate for the drawable tag, or we've been set to
                    // skip it, fail fast and return null
                    if (DEBUG) {
                        Log.d(TAG, "[loadDrawableFromDelegates] Skipping drawable: "
                                + context.getResources().getResourceName(resId));
                    }
                    return null;
                }
            } else {
                // Create an id cache as we'll need one later
                mKnownDrawableIdTags = new SparseArrayCompat<>();
            }

            if (mTypedValue == null) {
                mTypedValue = new TypedValue();
            }
            final TypedValue tv = mTypedValue;
            final Resources res = context.getResources();
            res.getValue(resId, tv, true);

            final long key = createCacheKey(tv);

            Drawable dr = getCachedDrawable(context, key);
            if (dr != null) {
                if (DEBUG) {
                    Log.i(TAG, "[loadDrawableFromDelegates] Returning cached drawable: " +
                            context.getResources().getResourceName(resId));
                }
                // We have a cached drawable, return it!
                return dr;
            }

            if (tv.string != null && tv.string.toString().endsWith(".xml")) {
                // If the resource is an XML file, let's try and parse it
                try {
                    @SuppressLint("ResourceType") final XmlPullParser parser = res.getXml(resId);
                    final AttributeSet attrs = Xml.asAttributeSet(parser);
                    int type;
                    while ((type = parser.next()) != XmlPullParser.START_TAG &&
                            type != XmlPullParser.END_DOCUMENT) {
                        // Empty loop
                    }
                    if (type != XmlPullParser.START_TAG) {
                        throw new XmlPullParserException("No start tag found");
                    }

                    final String tagName = parser.getName();
                    // Add the tag name to the cache
                    mKnownDrawableIdTags.append(resId, tagName);

                    // Now try and find a delegate for the tag name and inflate if found
                    final InflateDelegate delegate = mDelegates.get(tagName);
                    if (delegate != null) {
                        dr = delegate.createFromXmlInner(context, parser, attrs,
                                context.getTheme());
                    }
                    if (dr != null) {
                        // Add it to the drawable cache
                        dr.setChangingConfigurations(tv.changingConfigurations);
                        if (addDrawableToCache(context, key, dr) && DEBUG) {
                            Log.i(TAG, "[loadDrawableFromDelegates] Saved drawable to cache: " +
                                    context.getResources().getResourceName(resId));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception while inflating drawable", e);
                }
            }
            if (dr == null) {
                // If we reach here then the delegate inflation of the resource failed. Mark it as
                // bad so we skip the id next time
                mKnownDrawableIdTags.append(resId, SKIP_DRAWABLE_TAG);
            }
            return dr;
        }

        return null;
    }

    private synchronized Drawable getCachedDrawable(@NonNull final Context context,
            final long key) {
        final LongSparseArray<WeakReference<ConstantState>> cache = mDrawableCaches.get(context);
        if (cache == null) {
            return null;
        }

        final WeakReference<ConstantState> wr = cache.get(key);
        if (wr != null) {
            // We have the key, and the secret
            ConstantState entry = wr.get();
            if (entry != null) {
                return entry.newDrawable(context.getResources());
            } else {
                // Our entry has been purged
                cache.remove(key);
            }
        }
        return null;
    }

    private synchronized boolean addDrawableToCache(@NonNull final Context context, final long key,
            @NonNull final Drawable drawable) {
        final ConstantState cs = drawable.getConstantState();
        if (cs != null) {
            LongSparseArray<WeakReference<ConstantState>> cache = mDrawableCaches.get(context);
            if (cache == null) {
                cache = new LongSparseArray<>();
                mDrawableCaches.put(context, cache);
            }
            cache.put(key, new WeakReference<>(cs));
            return true;
        }
        return false;
    }

    synchronized Drawable onDrawableLoadedFromResources(@NonNull Context context,
            @NonNull VectorEnabledTintResources resources, @DrawableRes final int resId) {
        Drawable drawable = loadDrawableFromDelegates(context, resId);
        if (drawable == null) {
            drawable = resources.getDrawableCanonical(resId);
        }
        if (drawable != null) {
            return tintDrawable(context, resId, false, drawable);
        }
        return null;
    }

    boolean tintDrawableUsingColorFilter(@NonNull Context context,
            @DrawableRes final int resId, @NonNull Drawable drawable) {
        return (mHooks != null) && mHooks.tintDrawableUsingColorFilter(context, resId, drawable);
    }

    private void addDelegate(@NonNull String tagName, @NonNull InflateDelegate delegate) {
        if (mDelegates == null) {
            mDelegates = new SimpleArrayMap<>();
        }
        mDelegates.put(tagName, delegate);
    }

    PorterDuff.Mode getTintMode(final int resId) {
        return (mHooks == null) ? null : mHooks.getTintModeForDrawableRes(resId);
    }

    synchronized ColorStateList getTintList(@NonNull Context context, @DrawableRes int resId) {
        // Try the cache first (if it exists)
        ColorStateList tint = getTintListFromCache(context, resId);

        if (tint == null) {
            // ...if the cache did not contain a color state list, try and create one
            tint = (mHooks == null) ? null : mHooks.getTintListForDrawableRes(context, resId);

            if (tint != null) {
                addTintListToCache(context, resId, tint);
            }
        }
        return tint;
    }

    private ColorStateList getTintListFromCache(@NonNull Context context, @DrawableRes int resId) {
        if (mTintLists != null) {
            final SparseArrayCompat<ColorStateList> tints = mTintLists.get(context);
            return tints != null ? tints.get(resId) : null;
        }
        return null;
    }

    private void addTintListToCache(@NonNull Context context, @DrawableRes int resId,
            @NonNull ColorStateList tintList) {
        if (mTintLists == null) {
            mTintLists = new WeakHashMap<>();
        }
        SparseArrayCompat<ColorStateList> themeTints = mTintLists.get(context);
        if (themeTints == null) {
            themeTints = new SparseArrayCompat<>();
            mTintLists.put(context, themeTints);
        }
        themeTints.append(resId, tintList);
    }

    private static class ColorFilterLruCache extends LruCache<Integer, PorterDuffColorFilter> {

        public ColorFilterLruCache(int maxSize) {
            super(maxSize);
        }

        PorterDuffColorFilter get(int color, PorterDuff.Mode mode) {
            return get(generateCacheKey(color, mode));
        }

        PorterDuffColorFilter put(int color, PorterDuff.Mode mode, PorterDuffColorFilter filter) {
            return put(generateCacheKey(color, mode), filter);
        }

        private static int generateCacheKey(int color, PorterDuff.Mode mode) {
            int hashCode = 1;
            hashCode = 31 * hashCode + color;
            hashCode = 31 * hashCode + mode.hashCode();
            return hashCode;
        }
    }

    static void tintDrawable(Drawable drawable, TintInfo tint, int[] state) {
        int[] drawableState = drawable.getState();

        boolean mutated = drawable.mutate() == drawable;
        if (!mutated) {
            Log.d(TAG, "Mutated drawable is not the same instance as the input.");
            return;
        }

        // Workaround for b/232275112 where LayerDrawable loses its state on mutate().
        if (drawable instanceof LayerDrawable && drawable.isStateful()) {
            // Clear state first, otherwise setState() is a no-op.
            drawable.setState(new int[0]);
            drawable.setState(drawableState);
        }

        if (tint.mHasTintList || tint.mHasTintMode) {
            drawable.setColorFilter(createTintFilter(
                    tint.mHasTintList ? tint.mTintList : null,
                    tint.mHasTintMode ? tint.mTintMode : DEFAULT_MODE,
                    state));
        } else {
            drawable.clearColorFilter();
        }

        if (Build.VERSION.SDK_INT <= 23) {
            // Pre-v23 there is no guarantee that a state change will invoke an invalidation,
            // so we force it ourselves
            drawable.invalidateSelf();
        }
    }

    private static PorterDuffColorFilter createTintFilter(ColorStateList tint,
            PorterDuff.Mode tintMode, final int[] state) {
        if (tint == null || tintMode == null) {
            return null;
        }
        final int color = tint.getColorForState(state, Color.TRANSPARENT);
        return getPorterDuffColorFilter(color, tintMode);
    }

    public static synchronized PorterDuffColorFilter getPorterDuffColorFilter(
            int color, PorterDuff.Mode mode) {
        // First, let's see if the cache already contains the color filter
        PorterDuffColorFilter filter = COLOR_FILTER_CACHE.get(color, mode);

        if (filter == null) {
            // Cache miss, so create a color filter and add it to the cache
            filter = new PorterDuffColorFilter(color, mode);
            COLOR_FILTER_CACHE.put(color, mode, filter);
        }

        return filter;
    }

    private void checkVectorDrawableSetup(@NonNull Context context) {
        if (mHasCheckedVectorDrawableSetup) {
            // We've already checked so return now...
            return;
        }
        // Here we will check that a known Vector drawable resource inside AppCompat can be
        // correctly decoded
        mHasCheckedVectorDrawableSetup = true;
        final Drawable d = getDrawable(context, R.drawable.abc_vector_test);
        if (d == null || !isVectorDrawable(d)) {
            mHasCheckedVectorDrawableSetup = false;
            throw new IllegalStateException("This app has been built with an incorrect "
                    + "configuration. Please configure your build for VectorDrawableCompat.");
        }
    }

    private static boolean isVectorDrawable(@NonNull Drawable d) {
        return d instanceof VectorDrawableCompat
                || PLATFORM_VD_CLAZZ.equals(d.getClass().getName());
    }

    private static class VdcInflateDelegate implements InflateDelegate {
        VdcInflateDelegate() {
        }

        @Override
        public Drawable createFromXmlInner(@NonNull Context context, @NonNull XmlPullParser parser,
                @NonNull AttributeSet attrs, @Nullable Resources.Theme theme) {
            try {
                return VectorDrawableCompat
                        .createFromXmlInner(context.getResources(), parser, attrs, theme);
            } catch (Exception e) {
                Log.e("VdcInflateDelegate", "Exception while inflating <vector>", e);
                return null;
            }
        }
    }

    private static class AvdcInflateDelegate implements InflateDelegate {
        AvdcInflateDelegate() {
        }

        @Override
        public Drawable createFromXmlInner(@NonNull Context context, @NonNull XmlPullParser parser,
                @NonNull AttributeSet attrs, @Nullable Resources.Theme theme) {
            try {
                return AnimatedVectorDrawableCompat
                        .createFromXmlInner(context, context.getResources(), parser, attrs, theme);
            } catch (Exception e) {
                Log.e("AvdcInflateDelegate", "Exception while inflating <animated-vector>", e);
                return null;
            }
        }
    }

    static class AsldcInflateDelegate implements InflateDelegate {
        @Override
        public Drawable createFromXmlInner(@NonNull Context context, @NonNull XmlPullParser parser,
                @NonNull AttributeSet attrs, @Nullable Resources.Theme theme) {
            try {
                return AnimatedStateListDrawableCompat
                        .createFromXmlInner(context, context.getResources(), parser, attrs, theme);
            } catch (Exception e) {
                Log.e("AsldcInflateDelegate", "Exception while inflating <animated-selector>", e);
                return null;
            }
        }
    }

    static class DrawableDelegate implements InflateDelegate {
        @Override
        public Drawable createFromXmlInner(@NonNull Context context, @NonNull XmlPullParser parser,
                @NonNull AttributeSet attrs, @Nullable Resources.Theme theme) {
            String className = attrs.getClassAttribute();
            if (className != null) {
                try {
                    Class<? extends Drawable> drawableClass =
                            DrawableDelegate.class.getClassLoader().loadClass(className)
                                    .asSubclass(Drawable.class);
                    Drawable drawable = drawableClass.getDeclaredConstructor().newInstance();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Compatibility.Api21Impl.inflate(drawable, context.getResources(), parser,
                                attrs, theme);
                    } else {
                        drawable.inflate(context.getResources(), parser, attrs);
                    }
                    return drawable;
                } catch (Exception e) {
                    Log.e("DrawableDelegate", "Exception while inflating <drawable>", e);
                    return null;
                }
            }
            return null;
        }
    }
}
