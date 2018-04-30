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

package androidx.core.graphics.drawable;

import static android.graphics.drawable.Icon.TYPE_ADAPTIVE_BITMAP;
import static android.graphics.drawable.Icon.TYPE_BITMAP;
import static android.graphics.drawable.Icon.TYPE_DATA;
import static android.graphics.drawable.Icon.TYPE_RESOURCE;
import static android.graphics.drawable.Icon.TYPE_URI;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.os.BuildCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;

/**
 * Helper for accessing features in {@link android.graphics.drawable.Icon}.
 */
public class IconCompat {

    private static final String TAG = "IconCompat";

    /**
     * Value returned when the type of an {@link Icon} cannot be determined.
     * @see #getType(Icon)
     */
    public static final int TYPE_UNKOWN = -1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({TYPE_UNKOWN, TYPE_BITMAP, TYPE_RESOURCE, TYPE_DATA, TYPE_URI, TYPE_ADAPTIVE_BITMAP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IconType {
    }

    // Ratio of expected size to actual icon size
    private static final float ADAPTIVE_ICON_INSET_FACTOR = 1 / 4f;
    private static final float DEFAULT_VIEW_PORT_SCALE = 1 / (1 + 2 * ADAPTIVE_ICON_INSET_FACTOR);
    private static final float ICON_DIAMETER_FACTOR = 176f / 192;
    private static final float BLUR_FACTOR = 0.5f / 48;
    private static final float KEY_SHADOW_OFFSET_FACTOR = 1f / 48;

    private static final int KEY_SHADOW_ALPHA = 61;
    private static final int AMBIENT_SHADOW_ALPHA = 30;

    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_OBJ = "obj";
    private static final String EXTRA_INT1 = "int1";
    private static final String EXTRA_INT2 = "int2";
    private static final String EXTRA_TINT_LIST = "tint_list";
    private static final String EXTRA_TINT_MODE = "tint_mode";

    private final int mType;

    // To avoid adding unnecessary overhead, we have a few basic objects that get repurposed
    // based on the value of mType.

    // TYPE_BITMAP: Bitmap
    // TYPE_ADAPTIVE_BITMAP: Bitmap
    // TYPE_RESOURCE: Context
    // TYPE_URI: String
    // TYPE_DATA: DataBytes
    private Object          mObj1;

    // TYPE_RESOURCE: resId
    // TYPE_DATA: data offset
    private int             mInt1;

    // TYPE_DATA: data length
    private int             mInt2;

    private ColorStateList  mTintList = null;
    static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.SRC_IN; // SRC_IN
    private PorterDuff.Mode mTintMode = DEFAULT_TINT_MODE;

    /**
     * Create an Icon pointing to a drawable resource.
     * @param context The context for the application whose resources should be used to resolve the
     *                given resource ID.
     * @param resId ID of the drawable resource
     * @see android.graphics.drawable.Icon#createWithResource(Context, int)
     */
    public static IconCompat createWithResource(Context context, @DrawableRes int resId) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null.");
        }
        final IconCompat rep = new IconCompat(TYPE_RESOURCE);
        rep.mInt1 = resId;
        rep.mObj1 = context.getPackageName();
        return rep;
    }

    /**
     * Create an Icon pointing to a bitmap in memory.
     * @param bits A valid {@link android.graphics.Bitmap} object
     * @see android.graphics.drawable.Icon#createWithBitmap(Bitmap)
     */
    public static IconCompat createWithBitmap(Bitmap bits) {
        if (bits == null) {
            throw new IllegalArgumentException("Bitmap must not be null.");
        }
        final IconCompat rep = new IconCompat(TYPE_BITMAP);
        rep.mObj1 = bits;
        return rep;
    }

    /**
     * Create an Icon pointing to a bitmap in memory that follows the icon design guideline defined
     * by {@link android.graphics.drawable.AdaptiveIconDrawable}.
     * @param bits A valid {@link android.graphics.Bitmap} object
     * @see android.graphics.drawable.Icon#createWithAdaptiveBitmap(Bitmap)
     */
    public static IconCompat createWithAdaptiveBitmap(Bitmap bits) {
        if (bits == null) {
            throw new IllegalArgumentException("Bitmap must not be null.");
        }
        final IconCompat rep = new IconCompat(TYPE_ADAPTIVE_BITMAP);
        rep.mObj1 = bits;
        return rep;
    }

    /**
     * Create an Icon pointing to a compressed bitmap stored in a byte array.
     * @param data Byte array storing compressed bitmap data of a type that
     *             {@link android.graphics.BitmapFactory}
     *             can decode (see {@link android.graphics.Bitmap.CompressFormat}).
     * @param offset Offset into <code>data</code> at which the bitmap data starts
     * @param length Length of the bitmap data
     * @see android.graphics.drawable.Icon#createWithData(byte[], int, int)
     */
    public static IconCompat createWithData(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }
        final IconCompat rep = new IconCompat(TYPE_DATA);
        rep.mObj1 = data;
        rep.mInt1 = offset;
        rep.mInt2 = length;
        return rep;
    }

    /**
     * Create an Icon pointing to an image file specified by URI.
     *
     * @param uri A uri referring to local content:// or file:// image data.
     * @see android.graphics.drawable.Icon#createWithContentUri(String)
     */
    public static IconCompat createWithContentUri(String uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri must not be null.");
        }
        final IconCompat rep = new IconCompat(TYPE_URI);
        rep.mObj1 = uri;
        return rep;
    }

    /**
     * Create an Icon pointing to an image file specified by URI.
     *
     * @param uri A uri referring to local content:// or file:// image data.
     * @see android.graphics.drawable.Icon#createWithContentUri(String)
     */
    public static IconCompat createWithContentUri(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri must not be null.");
        }
        return createWithContentUri(uri.toString());
    }

    private IconCompat(int mType) {
        this.mType = mType;
    }


    /**
     * Gets the type of the icon provided.
     * <p>
     * Note that new types may be added later, so callers should guard against other
     * types being returned.
     */
    @IconType
    public int getType() {
        if (mType == TYPE_UNKOWN && Build.VERSION.SDK_INT >= 23) {
            return getType((Icon) mObj1);
        }
        return mType;
    }

    /**
     * Gets the package used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_RESOURCE.
     * Note: This package may not be available if referenced in the future, and it is
     * up to the caller to ensure safety if this package is re-used and/or persisted.
     */
    @NonNull
    public String getResPackage() {
        if (mType == TYPE_UNKOWN && Build.VERSION.SDK_INT >= 23) {
            return getResPackage((Icon) mObj1);
        }
        if (mType != TYPE_RESOURCE) {
            throw new IllegalStateException("called getResPackage() on " + this);
        }
        return (String) mObj1;
    }

    /**
     * Gets the resource id used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_RESOURCE.
     * Note: This resource may not be available if the application changes at all, and it is
     * up to the caller to ensure safety if this resource is re-used and/or persisted.
     */
    @IdRes
    public int getResId() {
        if (mType == TYPE_UNKOWN && Build.VERSION.SDK_INT >= 23) {
            return getResId((Icon) mObj1);
        }
        if (mType != TYPE_RESOURCE) {
            throw new IllegalStateException("called getResId() on " + this);
        }
        return mInt1;
    }

    /**
     * Gets the uri used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_URI.
     * Note: This uri may not be available in the future, and it is
     * up to the caller to ensure safety if this uri is re-used and/or persisted.
     */
    @NonNull
    public Uri getUri() {
        if (mType == TYPE_UNKOWN && Build.VERSION.SDK_INT >= 23) {
            return getUri((Icon) mObj1);
        }
        return Uri.parse((String) mObj1);
    }

    /**
     * Store a color to use whenever this Icon is drawn.
     *
     * @param tint a color, as in {@link Drawable#setTint(int)}
     * @return this same object, for use in chained construction
     */
    public IconCompat setTint(@ColorInt int tint) {
        return setTintList(ColorStateList.valueOf(tint));
    }

    /**
     * Store a color to use whenever this Icon is drawn.
     *
     * @param tintList as in {@link Drawable#setTintList(ColorStateList)}, null to remove tint
     * @return this same object, for use in chained construction
     */
    public IconCompat setTintList(ColorStateList tintList) {
        mTintList = tintList;
        return this;
    }

    /**
     * Store a blending mode to use whenever this Icon is drawn.
     *
     * @param mode a blending mode, as in {@link Drawable#setTintMode(PorterDuff.Mode)}, may be null
     * @return this same object, for use in chained construction
     */
    public IconCompat setTintMode(PorterDuff.Mode mode) {
        mTintMode = mode;
        return this;
    }

    /**
     * Convert this compat object to {@link Icon} object.
     *
     * @return {@link Icon} object
     */
    @RequiresApi(23)
    public Icon toIcon() {
        Icon icon;
        switch (mType) {
            case TYPE_UNKOWN:
                // When type is unknown we are just wrapping an icon.
                return (Icon) mObj1;
            case TYPE_BITMAP:
                icon = Icon.createWithBitmap((Bitmap) mObj1);
                break;
            case TYPE_ADAPTIVE_BITMAP:
                if (Build.VERSION.SDK_INT >= 26) {
                    icon = Icon.createWithAdaptiveBitmap((Bitmap) mObj1);
                } else {
                    icon = Icon.createWithBitmap(
                            createLegacyIconFromAdaptiveIcon((Bitmap) mObj1, false));
                }
                break;
            case TYPE_RESOURCE:
                icon = Icon.createWithResource((String) mObj1, mInt1);
                break;
            case TYPE_DATA:
                icon = Icon.createWithData((byte[]) mObj1, mInt1, mInt2);
                break;
            case TYPE_URI:
                icon = Icon.createWithContentUri((String) mObj1);
                break;
            default:
                throw new IllegalArgumentException("Unknown type");
        }
        if (mTintList != null) {
            icon.setTintList(mTintList);
        }
        if (mTintMode != DEFAULT_TINT_MODE) {
            icon.setTintMode(mTintMode);
        }
        return icon;
    }



    /**
     * Returns a Drawable that can be used to draw the image inside this Icon, constructing it
     * if necessary.
     *
     * @param context {@link android.content.Context Context} in which to load the drawable; used
     *                to access {@link android.content.res.Resources Resources}, for example.
     * @return A fresh instance of a drawable for this image, yours to keep.
     */
    public Drawable loadDrawable(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            return toIcon().loadDrawable(context);
        }
        final Drawable result = loadDrawableInner(context);
        if (result != null && (mTintList != null || mTintMode != DEFAULT_TINT_MODE)) {
            result.mutate();
            DrawableCompat.setTintList(result, mTintList);
            DrawableCompat.setTintMode(result, mTintMode);
        }
        return result;
    }


    /**
     * Do the heavy lifting of loading the drawable, but stop short of applying any tint.
     */
    private Drawable loadDrawableInner(Context context) {
        switch (mType) {
            case TYPE_BITMAP:
                return new BitmapDrawable(context.getResources(), (Bitmap) mObj1);
            case TYPE_ADAPTIVE_BITMAP:
                return new BitmapDrawable(context.getResources(),
                        createLegacyIconFromAdaptiveIcon((Bitmap) mObj1, false));
            case TYPE_RESOURCE:
                Resources res;
                // figure out where to load resources from
                String resPackage = (String) mObj1;
                if (TextUtils.isEmpty(resPackage)) {
                    // if none is specified, try the given context
                    resPackage = context.getPackageName();
                }
                if ("android".equals(resPackage)) {
                    res = Resources.getSystem();
                } else {
                    final PackageManager pm = context.getPackageManager();
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(
                                resPackage, PackageManager.MATCH_UNINSTALLED_PACKAGES);
                        if (ai != null) {
                            res = pm.getResourcesForApplication(ai);
                        } else {
                            break;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, String.format("Unable to find pkg=%s for icon %s",
                                resPackage, this), e);
                        break;
                    }
                }
                try {
                    return ResourcesCompat.getDrawable(res, mInt1, context.getTheme());
                } catch (RuntimeException e) {
                    Log.e(TAG, String.format("Unable to load resource 0x%08x from pkg=%s",
                            mInt1,
                            mObj1),
                            e);
                }
                break;
            case TYPE_DATA:
                return new BitmapDrawable(context.getResources(),
                        BitmapFactory.decodeByteArray((byte[]) mObj1, mInt1, mInt2)
                );
            case TYPE_URI:
                final Uri uri = Uri.parse((String) mObj1);
                final String scheme = uri.getScheme();
                InputStream is = null;
                if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                        || ContentResolver.SCHEME_FILE.equals(scheme)) {
                    try {
                        is = context.getContentResolver().openInputStream(uri);
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to load image from URI: " + uri, e);
                    }
                } else {
                    try {
                        is = new FileInputStream(new File((String) mObj1));
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, "Unable to load image from path: " + uri, e);
                    }
                }
                if (is != null) {
                    return new BitmapDrawable(context.getResources(),
                            BitmapFactory.decodeStream(is));
                }
                break;
        }
        return null;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @SuppressWarnings("deprecation")
    public void addToShortcutIntent(@NonNull Intent outIntent, @Nullable Drawable badge,
            @NonNull Context c) {
        Bitmap icon;
        switch (mType) {
            case TYPE_BITMAP:
                icon = (Bitmap) mObj1;
                if (badge != null) {
                    // Do not modify the original icon when applying a badge
                    icon = icon.copy(icon.getConfig(), true);
                }
                break;
            case TYPE_ADAPTIVE_BITMAP:
                icon = createLegacyIconFromAdaptiveIcon((Bitmap) mObj1, true);
                break;
            case TYPE_RESOURCE:
                try {
                    Context context = c.createPackageContext((String) mObj1, 0);
                    if (badge == null) {
                        outIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                Intent.ShortcutIconResource.fromContext(context, mInt1));
                        return;
                    } else {
                        Drawable dr = ContextCompat.getDrawable(context, mInt1);
                        if (dr.getIntrinsicWidth() <= 0 || dr.getIntrinsicHeight() <= 0) {
                            int size = ((ActivityManager) context.getSystemService(
                                    Context.ACTIVITY_SERVICE)).getLauncherLargeIconSize();
                            icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        } else {
                            icon = Bitmap.createBitmap(dr.getIntrinsicWidth(),
                                    dr.getIntrinsicHeight(),
                                    Bitmap.Config.ARGB_8888);
                        }
                        dr.setBounds(0, 0, icon.getWidth(), icon.getHeight());
                        dr.draw(new Canvas(icon));
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    throw new IllegalArgumentException("Can't find package " + mObj1, e);
                }
                break;
            default:
                throw new IllegalArgumentException("Icon type not supported for intent shortcuts");
        }
        if (badge != null) {
            // Badge the icon
            int w = icon.getWidth();
            int h = icon.getHeight();
            badge.setBounds(w / 2, h / 2, w, h);
            badge.draw(new Canvas(icon));
        }
        outIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
    }

    /**
     * Adds this Icon to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        switch (mType) {
            case TYPE_BITMAP:
            case TYPE_ADAPTIVE_BITMAP:
                bundle.putParcelable(EXTRA_OBJ, (Bitmap) mObj1);
                break;
            case TYPE_UNKOWN:
                // When unknown just wrapping an Icon.
                bundle.putParcelable(EXTRA_OBJ, (Parcelable) mObj1);
                break;
            case TYPE_RESOURCE:
            case TYPE_URI:
                bundle.putString(EXTRA_OBJ, (String) mObj1);
                break;
            case TYPE_DATA:
                bundle.putByteArray(EXTRA_OBJ, (byte[]) mObj1);
                break;
            default:
                throw new IllegalArgumentException("Invalid icon");
        }
        bundle.putInt(EXTRA_TYPE, mType);
        bundle.putInt(EXTRA_INT1, mInt1);
        bundle.putInt(EXTRA_INT2, mInt2);
        if (mTintList != null) {
            bundle.putParcelable(EXTRA_TINT_LIST, mTintList);
        }
        if (mTintMode != DEFAULT_TINT_MODE) {
            bundle.putString(EXTRA_TINT_MODE, mTintMode.name());
        }
        return bundle;
    }

    @Override
    public String toString() {
        if (mType == TYPE_UNKOWN) {
            return String.valueOf(mObj1);
        }
        final StringBuilder sb = new StringBuilder("Icon(typ=").append(typeToString(mType));
        switch (mType) {
            case TYPE_BITMAP:
            case TYPE_ADAPTIVE_BITMAP:
                sb.append(" size=")
                        .append(((Bitmap) mObj1).getWidth())
                        .append("x")
                        .append(((Bitmap) mObj1).getHeight());
                break;
            case TYPE_RESOURCE:
                sb.append(" pkg=")
                        .append(getResPackage())
                        .append(" id=")
                        .append(String.format("0x%08x", getResId()));
                break;
            case TYPE_DATA:
                sb.append(" len=").append(mInt1);
                if (mInt2 != 0) {
                    sb.append(" off=").append(mInt2);
                }
                break;
            case TYPE_URI:
                sb.append(" uri=").append(mObj1);
                break;
        }
        if (mTintList != null) {
            sb.append(" tint=");
            sb.append(mTintList);
        }
        if (mTintMode != DEFAULT_TINT_MODE) {
            sb.append(" mode=").append(mTintMode);
        }
        sb.append(")");
        return sb.toString();
    }

    private static String typeToString(int x) {
        switch (x) {
            case TYPE_BITMAP: return "BITMAP";
            case TYPE_ADAPTIVE_BITMAP: return "BITMAP_MASKABLE";
            case TYPE_DATA: return "DATA";
            case TYPE_RESOURCE: return "RESOURCE";
            case TYPE_URI: return "URI";
            default: return "UNKNOWN";
        }
    }

    /**
     * Extracts an icon from a bundle that was added using {@link #toBundle()}.
     */
    public static @Nullable IconCompat createFromBundle(@NonNull Bundle bundle) {
        int type = bundle.getInt(EXTRA_TYPE);
        IconCompat icon = new IconCompat(type);
        icon.mInt1 = bundle.getInt(EXTRA_INT1);
        icon.mInt2 = bundle.getInt(EXTRA_INT2);
        if (bundle.containsKey(EXTRA_TINT_LIST)) {
            icon.mTintList = bundle.getParcelable(EXTRA_TINT_LIST);
        }
        if (bundle.containsKey(EXTRA_TINT_MODE)) {
            icon.mTintMode = PorterDuff.Mode.valueOf(
                    bundle.getString(EXTRA_TINT_MODE));
        }
        switch (type) {
            case TYPE_BITMAP:
            case TYPE_ADAPTIVE_BITMAP:
            case TYPE_UNKOWN:
                icon.mObj1 = bundle.getParcelable(EXTRA_OBJ);
                break;
            case TYPE_RESOURCE:
            case TYPE_URI:
                icon.mObj1 = bundle.getString(EXTRA_OBJ);
                break;
            case TYPE_DATA:
                icon.mObj1 = bundle.getByteArray(EXTRA_OBJ);
                break;
            default:
                Log.w(TAG, "Unknown type " + type);
                return null;
        }
        return icon;
    }

    /**
     * Creates an IconCompat from an Icon.
     */
    @RequiresApi(23)
    @Nullable
    public static IconCompat createFromIcon(@NonNull Icon icon) {
        IconCompat iconCompat = new IconCompat(TYPE_UNKOWN);
        iconCompat.mObj1 = icon;
        return iconCompat;
    }

    /**
     * Gets the type of the icon provided.
     * <p>
     * Note that new types may be added later, so callers should guard against other
     * types being returned. Returns {@link #TYPE_UNKOWN} when the type cannot be
     * determined.
     */
    @IconType
    @RequiresApi(23)
    public static int getType(@NonNull Icon icon) {
        if (BuildCompat.isAtLeastP()) {
            return icon.getType();
        }
        try {
            return (int) icon.getClass().getMethod("getType").invoke(icon);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to get icon type " + icon, e);
            return TYPE_UNKOWN;
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Unable to get icon type " + icon, e);
            return TYPE_UNKOWN;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Unable to get icon type " + icon, e);
            return TYPE_UNKOWN;
        }
    }

    /**
     * Gets the package used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_RESOURCE.
     * Note: This package may not be available if referenced in the future, and it is
     * up to the caller to ensure safety if this package is re-used and/or persisted.
     * Returns {@code null} when the value cannot be gotten.
     */
    @Nullable
    @RequiresApi(23)
    public static String getResPackage(@NonNull Icon icon) {
        if (BuildCompat.isAtLeastP()) {
            return icon.getResPackage();
        }
        try {
            return (String) icon.getClass().getMethod("getResPackage").invoke(icon);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to get icon package", e);
            return null;
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Unable to get icon package", e);
            return null;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Unable to get icon package", e);
            return null;
        }
    }

    /**
     * Gets the resource used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_RESOURCE.
     * Note: This resource may not be available if the application changes at all, and it is
     * up to the caller to ensure safety if this resource is re-used and/or persisted.
     * Returns {@code 0} if the id cannot be gotten.
     */
    @IdRes
    @RequiresApi(23)
    public static int getResId(@NonNull Icon icon) {
        if (BuildCompat.isAtLeastP()) {
            return icon.getResId();
        }
        try {
            return (int) icon.getClass().getMethod("getResId").invoke(icon);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to get icon resource", e);
            return 0;
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Unable to get icon resource", e);
            return 0;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Unable to get icon resource", e);
            return 0;
        }
    }

    /**
     * Gets the uri used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_URI.
     * Note: This uri may not be available in the future, and it is
     * up to the caller to ensure safety if this uri is re-used and/or persisted.
     * Returns {@code null} if the uri cannot be gotten.
     */
    @Nullable
    @RequiresApi(23)
    public Uri getUri(@NonNull Icon icon) {
        if (BuildCompat.isAtLeastP()) {
            return icon.getUri();
        }
        try {
            return (Uri) icon.getClass().getMethod("getUri").invoke(icon);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to get icon uri", e);
            return null;
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Unable to get icon uri", e);
            return null;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Unable to get icon uri", e);
            return null;
        }
    }

    /**
     * Converts a bitmap following the adaptive icon guide lines, into a bitmap following the
     * shortcut icon guide lines.
     * The returned bitmap will always have same width and height and clipped to a circle.
     *
     * @param addShadow set to {@code true} only for legacy shortcuts and {@code false} otherwise
     */
    @VisibleForTesting
    static Bitmap createLegacyIconFromAdaptiveIcon(Bitmap adaptiveIconBitmap, boolean addShadow) {
        int size = (int) (DEFAULT_VIEW_PORT_SCALE * Math.min(adaptiveIconBitmap.getWidth(),
                adaptiveIconBitmap.getHeight()));

        Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        float center = size * 0.5f;
        float radius = center * ICON_DIAMETER_FACTOR;

        if (addShadow) {
            // Draw key shadow
            float blur = BLUR_FACTOR * size;
            paint.setColor(Color.TRANSPARENT);
            paint.setShadowLayer(blur, 0, KEY_SHADOW_OFFSET_FACTOR * size, KEY_SHADOW_ALPHA << 24);
            canvas.drawCircle(center, center, radius, paint);

            // Draw ambient shadow
            paint.setShadowLayer(blur, 0, 0, AMBIENT_SHADOW_ALPHA << 24);
            canvas.drawCircle(center, center, radius, paint);
            paint.clearShadowLayer();
        }

        // Draw the clipped icon
        paint.setColor(Color.BLACK);
        BitmapShader shader = new BitmapShader(adaptiveIconBitmap, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
        Matrix shift = new Matrix();
        shift.setTranslate(-(adaptiveIconBitmap.getWidth() - size) / 2,
                -(adaptiveIconBitmap.getHeight() - size) / 2);
        shader.setLocalMatrix(shift);
        paint.setShader(shader);
        canvas.drawCircle(center, center, radius, paint);

        canvas.setBitmap(null);
        return icon;
    }
}
