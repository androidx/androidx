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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

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
import android.graphics.drawable.AdaptiveIconDrawable;
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
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

/**
 * Helper for accessing features in {@link android.graphics.drawable.Icon}.
 */
@VersionedParcelize(allowSerialization = true, ignoreParcelables = true, isCustom = true,
        jetifyAs = "android.support.v4.graphics.drawable.IconCompat")
public class IconCompat extends CustomVersionedParcelable {

    private static final String TAG = "IconCompat";

    /**
     * Value returned when the type of an {@link Icon} cannot be determined.
     */
    public static final int TYPE_UNKNOWN  = -1;
    /**
     * An icon that was created using {@link #createWithBitmap(Bitmap)}.
     */
    public static final int TYPE_BITMAP   = Icon.TYPE_BITMAP;
    /**
     * An icon that was created using {@link #createWithResource}.
     */
    public static final int TYPE_RESOURCE = Icon.TYPE_RESOURCE;
    /**
     * An icon that was created using {@link #createWithData(byte[], int, int)}.
     */
    public static final int TYPE_DATA     = Icon.TYPE_DATA;
    /**
     * An icon that was created using {@link #createWithContentUri}.
     */
    public static final int TYPE_URI      = Icon.TYPE_URI;
    /**
     * An icon that was created using {@link #createWithAdaptiveBitmap}.
     */
    public static final int TYPE_ADAPTIVE_BITMAP = Icon.TYPE_ADAPTIVE_BITMAP;

    /**
     * An icon that was created using {@link #createWithAdaptiveBitmapContentUri}.
     */
    public static final int TYPE_URI_ADAPTIVE_BITMAP = Icon.TYPE_URI_ADAPTIVE_BITMAP;

    /**
     */
    @RestrictTo(LIBRARY)
    @IntDef({TYPE_UNKNOWN, TYPE_BITMAP, TYPE_RESOURCE, TYPE_DATA, TYPE_URI, TYPE_ADAPTIVE_BITMAP,
            TYPE_URI_ADAPTIVE_BITMAP})
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

    @VisibleForTesting
    static final String EXTRA_TYPE = "type";
    @VisibleForTesting
    static final String EXTRA_OBJ = "obj";
    @VisibleForTesting
    static final String EXTRA_INT1 = "int1";
    @VisibleForTesting
    static final String EXTRA_INT2 = "int2";
    @VisibleForTesting
    static final String EXTRA_TINT_LIST = "tint_list";
    @VisibleForTesting
    static final String EXTRA_TINT_MODE = "tint_mode";
    @VisibleForTesting
    static final String EXTRA_STRING1 = "string1";

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @ParcelField(value = 1,
            defaultValue = "androidx.core.graphics.drawable.IconCompat.TYPE_UNKNOWN")
    public int mType = TYPE_UNKNOWN;

    // To avoid adding unnecessary overhead, we have a few basic objects that get repurposed
    // based on the value of mType.

    // TYPE_BITMAP: Bitmap
    // TYPE_ADAPTIVE_BITMAP: Bitmap
    // TYPE_RESOURCE: String
    // TYPE_URI: String
    // TYPE_DATA: DataBytes
    @NonParcelField
    Object          mObj1;

    /**
     */
    @RestrictTo(LIBRARY)
    @ParcelField(value = 2, defaultValue = "null")
    public byte @Nullable []          mData = null;
    /**
     */
    @RestrictTo(LIBRARY)
    @ParcelField(value = 3, defaultValue = "null")
    public @Nullable Parcelable      mParcelable = null;

    // TYPE_RESOURCE: resId
    // TYPE_DATA: data offset
    /**
     */
    @RestrictTo(LIBRARY)
    @ParcelField(value = 4, defaultValue = "0")
    public int             mInt1 = 0;

    // TYPE_DATA: data length
    /**
     */
    @RestrictTo(LIBRARY)
    @ParcelField(value = 5, defaultValue = "0")
    public int             mInt2 = 0;

    /**
     */
    @RestrictTo(LIBRARY)
    @ParcelField(value = 6, defaultValue = "null")
    public @Nullable ColorStateList  mTintList = null;

    static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.SRC_IN; // SRC_IN
    @NonParcelField
    PorterDuff.Mode mTintMode = DEFAULT_TINT_MODE;
    /**
     */
    @RestrictTo(LIBRARY)
    @ParcelField(value = 7, defaultValue = "null")
    public @Nullable String mTintModeStr = null;

    /**
     */
    @RestrictTo(LIBRARY)
    @ParcelField(value = 8, defaultValue = "null")
    public @Nullable String mString1;

    /**
     * Create an Icon pointing to a drawable resource.
     * @param context The context for the application whose resources should be used to resolve the
     *                given resource ID.
     * @param resId ID of the drawable resource
     * @see android.graphics.drawable.Icon#createWithResource(Context, int)
     */
    public static @NonNull IconCompat createWithResource(@NonNull Context context,
            @DrawableRes int resId) {
        ObjectsCompat.requireNonNull(context);
        return createWithResource(context.getResources(), context.getPackageName(), resId);
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static @NonNull IconCompat createWithResource(@Nullable Resources r,
            @NonNull String pkg,
            @DrawableRes int resId) {
        ObjectsCompat.requireNonNull(pkg);
        if (resId == 0) {
            throw new IllegalArgumentException("Drawable resource ID must not be 0");
        }
        final IconCompat rep = new IconCompat(TYPE_RESOURCE);
        rep.mInt1 = resId;
        if (r != null) {
            try {
                rep.mObj1 = r.getResourceName(resId);
            } catch (Resources.NotFoundException e) {
                throw new IllegalArgumentException("Icon resource cannot be found");
            }
        } else {
            rep.mObj1 = pkg;
        }
        rep.mString1 = pkg;
        return rep;
    }

    /**
     * Create an Icon pointing to a bitmap in memory.
     * @param bits A valid {@link android.graphics.Bitmap} object
     * @see android.graphics.drawable.Icon#createWithBitmap(Bitmap)
     */
    public static @NonNull IconCompat createWithBitmap(@NonNull Bitmap bits) {
        ObjectsCompat.requireNonNull(bits);
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
    public static @NonNull IconCompat createWithAdaptiveBitmap(@NonNull Bitmap bits) {
        ObjectsCompat.requireNonNull(bits);
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
    public static @NonNull IconCompat createWithData(byte @NonNull [] data, int offset,
            int length) {
        ObjectsCompat.requireNonNull(data);
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
    public static @NonNull IconCompat createWithContentUri(@NonNull String uri) {
        ObjectsCompat.requireNonNull(uri);
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
    public static @NonNull IconCompat createWithContentUri(@NonNull Uri uri) {
        ObjectsCompat.requireNonNull(uri);
        return createWithContentUri(uri.toString());
    }

    /**
     * Create an Icon pointing to an image file specified by URI. Image file should follow the icon
     * design guideline defined by {@link AdaptiveIconDrawable}.
     *
     * @param uri A uri referring to local content:// or file:// image data.
     * @see android.graphics.drawable.Icon#createWithAdaptiveBitmapContentUri(String)
     */
    public static @NonNull IconCompat createWithAdaptiveBitmapContentUri(@NonNull String uri) {
        ObjectsCompat.requireNonNull(uri);
        final IconCompat rep = new IconCompat(TYPE_URI_ADAPTIVE_BITMAP);
        rep.mObj1 = uri;
        return rep;
    }

    /**
     * Create an Icon pointing to an image file specified by URI. Image file should follow the icon
     * design guideline defined by {@link AdaptiveIconDrawable}.
     *
     * @param uri A uri referring to local content:// or file:// image data.
     * @see android.graphics.drawable.Icon#createWithAdaptiveBitmapContentUri(String)
     */
    public static @NonNull IconCompat createWithAdaptiveBitmapContentUri(@NonNull Uri uri) {
        ObjectsCompat.requireNonNull(uri);
        return createWithAdaptiveBitmapContentUri(uri.toString());
    }

    /**
     * Used for VersionedParcelable.
     */
    @RestrictTo(LIBRARY)
    public IconCompat() {
    }

    IconCompat(int mType) {
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
        if (mType == TYPE_UNKNOWN) {
            return Api23Impl.getType(mObj1);
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
    public @NonNull String getResPackage() {
        if (mType == TYPE_UNKNOWN) {
            return Api23Impl.getResPackage(mObj1);
        }
        if (mType != TYPE_RESOURCE) {
            throw new IllegalStateException("called getResPackage() on " + this);
        }
        // Before aosp/1307777, we don't put the package name to mString1. Try to get the
        // package name from the full resource name string. Note that this is not always the same
        // as "the package used to create this icon" and this was what aosp/1307777 tried to fix.
        if (mString1 == null || TextUtils.isEmpty(mString1)) {
            return ((String) mObj1).split(":", -1)[0];
        } else {
            // The name of the getResPackage() API is a bit confusing. It actually returns
            // the app package name rather than the package name in the resource table.
            return mString1;
        }
    }

    /**
     * Gets the drawable resource id used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_RESOURCE.
     * Note: This resource may not be available if the application changes at all, and it is
     * up to the caller to ensure safety if this resource is re-used and/or persisted.
     */
    @DrawableRes
    public int getResId() {
        if (mType == TYPE_UNKNOWN) {
            return Api23Impl.getResId(mObj1);
        }
        if (mType != TYPE_RESOURCE) {
            throw new IllegalStateException("called getResId() on " + this);
        }
        return mInt1;
    }

    /**
     * Gets the bitmap used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_BITMAP.
     * Note: This bitmap may not be available in the future, and it is
     * up to the caller to ensure safety if this bitmap is re-used and/or persisted.
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @Nullable Bitmap getBitmap() {
        if (mType == TYPE_UNKNOWN) {
            if (mObj1 instanceof Bitmap) {
                return (Bitmap) mObj1;
            }
            return null;
        }
        if (mType == TYPE_BITMAP) {
            return (Bitmap) mObj1;
        } else if (mType == TYPE_ADAPTIVE_BITMAP) {
            return createLegacyIconFromAdaptiveIcon((Bitmap) mObj1, true);
        } else {
            throw new IllegalStateException("called getBitmap() on " + this);
        }
    }

    /**
     * Gets the uri used to create this icon.
     * <p>
     * Only valid for icons of type TYPE_URI.
     * Note: This uri may not be available in the future, and it is
     * up to the caller to ensure safety if this uri is re-used and/or persisted.
     */
    public @NonNull Uri getUri() {
        if (mType == TYPE_UNKNOWN) {
            return Api23Impl.getUri(mObj1);
        }
        if (mType != TYPE_URI && mType != TYPE_URI_ADAPTIVE_BITMAP) {
            throw new IllegalStateException("called getUri() on " + this);
        }
        return Uri.parse((String) mObj1);
    }

    /**
     * Store a color to use whenever this Icon is drawn.
     *
     * @param tint a color, as in {@link Drawable#setTint(int)}
     * @return this same object, for use in chained construction
     */
    public @NonNull IconCompat setTint(@ColorInt int tint) {
        return setTintList(ColorStateList.valueOf(tint));
    }

    /**
     * Store a color to use whenever this Icon is drawn.
     *
     * @param tintList as in {@link Drawable#setTintList(ColorStateList)}, null to remove tint
     * @return this same object, for use in chained construction
     */
    public @NonNull IconCompat setTintList(@Nullable ColorStateList tintList) {
        mTintList = tintList;
        return this;
    }

    /**
     * Store a blending mode to use whenever this Icon is drawn.
     *
     * @param mode a blending mode, as in {@link Drawable#setTintMode(PorterDuff.Mode)}, may be null
     * @return this same object, for use in chained construction
     */
    public @NonNull IconCompat setTintMode(PorterDuff.@Nullable Mode mode) {
        mTintMode = mode;
        return this;
    }

    /**
     * @deprecated Use {@link #toIcon(Context)} to generate the {@link Icon} object.
     */
    @Deprecated
    public @NonNull Icon toIcon() {
        return toIcon(null);
    }

    /**
     * Convert this compat object to {@link Icon} object.
     *
     * @return {@link Icon} object
     */
    public @NonNull Icon toIcon(@Nullable Context context) {
        return Api23Impl.toIcon(this, context);
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void checkResource(@NonNull Context context) {
        if (mType == TYPE_RESOURCE && mObj1 != null) {
            String fullResName = (String) mObj1;
            if (!fullResName.contains(":")) {
                return;
            }
            // Do some splitting to parse out each of the components.
            String resName = fullResName.split(":", -1)[1];
            String resType = resName.split("/", -1)[0];
            resName = resName.split("/", -1)[1];
            String resPackage = fullResName.split(":", -1)[0];
            if ("0_resource_name_obfuscated".equals(resName)) {
                // All obfuscated resources have the same name, so not going to look up the
                // resource identifier from the resource name.
                Log.i(TAG, "Found obfuscated resource, not trying to update resource id for it");
                return;
            }
            String appPackage = getResPackage();
            Resources res = getResources(context, appPackage);
            int id = res.getIdentifier(resName, resType, resPackage);
            if (mInt1 != id) {
                Log.i(TAG, "Id has changed for " + appPackage + " " + fullResName);
                mInt1 = id;
            }
        }
    }

    /**
     * Returns a Drawable that can be used to draw the image inside this Icon, constructing it
     * if necessary.
     *
     * @param context {@link android.content.Context Context} in which to load the drawable; used
     *                to access {@link android.content.res.Resources Resources}, for example.
     * @return A fresh instance of a drawable for this image, yours to keep.
     */
    public @Nullable Drawable loadDrawable(@NonNull Context context) {
        checkResource(context);
        return toIcon(context).loadDrawable(context);
    }

    /**
     * Create an input stream for bitmap by resolving corresponding content uri.
     *
     */
    @RestrictTo(LIBRARY_GROUP)
    public @Nullable InputStream getUriInputStream(@NonNull Context context) {
        final Uri uri = getUri();
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                || ContentResolver.SCHEME_FILE.equals(scheme)) {
            try {
                return context.getContentResolver().openInputStream(uri);
            } catch (Exception e) {
                Log.w(TAG, "Unable to load image from URI: " + uri, e);
            }
        } else {
            try {
                return new FileInputStream(new File((String) mObj1));
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Unable to load image from path: " + uri, e);
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    static Resources getResources(Context context, String resPackage) {
        if ("android".equals(resPackage)) {
            return Resources.getSystem();
        } else {
            final PackageManager pm = context.getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo(
                        resPackage, PackageManager.MATCH_UNINSTALLED_PACKAGES);
                if (ai != null) {
                    return pm.getResourcesForApplication(ai);
                } else {
                    return null;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, String.format("Unable to find pkg=%s for icon",
                        resPackage), e);
                return null;
            }
        }
    }


    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @SuppressWarnings("deprecation")
    public void addToShortcutIntent(@NonNull Intent outIntent, @Nullable Drawable badge,
            @NonNull Context c) {
        checkResource(c);
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
                    Context context = c.createPackageContext(getResPackage(), 0);
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
    public @NonNull Bundle toBundle() {
        Bundle bundle = new Bundle();
        switch (mType) {
            case TYPE_BITMAP:
            case TYPE_ADAPTIVE_BITMAP:
                bundle.putParcelable(EXTRA_OBJ, (Bitmap) mObj1);
                break;
            case TYPE_UNKNOWN:
                // When unknown just wrapping an Icon.
                bundle.putParcelable(EXTRA_OBJ, (Parcelable) mObj1);
                break;
            case TYPE_RESOURCE:
            case TYPE_URI:
            case TYPE_URI_ADAPTIVE_BITMAP:
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
        bundle.putString(EXTRA_STRING1, mString1);
        if (mTintList != null) {
            bundle.putParcelable(EXTRA_TINT_LIST, mTintList);
        }
        if (mTintMode != DEFAULT_TINT_MODE) {
            bundle.putString(EXTRA_TINT_MODE, mTintMode.name());
        }
        return bundle;
    }

    @Override
    public @NonNull String toString() {
        if (mType == TYPE_UNKNOWN) {
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
                        .append(mString1)
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
            case TYPE_URI_ADAPTIVE_BITMAP:
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

    @Override
    public void onPreParceling(boolean isStream) {
        mTintModeStr = mTintMode.name();
        switch (mType) {
            case TYPE_UNKNOWN:
                if (isStream) {
                    // We can't determine how to serialize this icon, so throw so the caller knows.
                    throw new IllegalArgumentException("Can't serialize Icon created with "
                            + "IconCompat#createFromIcon");
                } else {
                    mParcelable = (Parcelable) mObj1;
                }
                break;
            case TYPE_ADAPTIVE_BITMAP:
            case TYPE_BITMAP:
                if (isStream) {
                    Bitmap bitmap = (Bitmap) mObj1;
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, data);
                    mData = data.toByteArray();
                } else {
                    mParcelable = (Parcelable) mObj1;
                }
                break;
            case TYPE_URI:
            case TYPE_URI_ADAPTIVE_BITMAP:
                mData = mObj1.toString().getBytes(Charset.forName("UTF-16"));
                break;
            case TYPE_RESOURCE:
                mData = ((String) mObj1).getBytes(Charset.forName("UTF-16"));
                break;
            case TYPE_DATA:
                mData = (byte[]) mObj1;
                break;
        }
    }

    @Override
    public void onPostParceling() {
        mTintMode = PorterDuff.Mode.valueOf(mTintModeStr);
        switch (mType) {
            case TYPE_UNKNOWN:
                if (mParcelable != null) {
                    mObj1 = mParcelable;
                } else {
                    throw new IllegalArgumentException("Invalid icon");
                }
                break;
            case TYPE_ADAPTIVE_BITMAP:
            case TYPE_BITMAP:
                if (mParcelable != null) {
                    mObj1 = mParcelable;
                } else {
                    // This is data now.
                    mObj1 = mData;
                    mType = TYPE_DATA;
                    mInt1 = 0;
                    mInt2 = mData.length;
                }
                break;
            case TYPE_URI:
            case TYPE_URI_ADAPTIVE_BITMAP:
            case TYPE_RESOURCE:
                mObj1 = new String(mData, Charset.forName("UTF-16"));
                // Slice, which may contain a IconCompat object, supports serialization to file.
                // In the old format, we don't store the app package name separately. To keep
                // the backward-compatibility, we have no choice but read the package name from the
                // full resource name string.
                if (mType == TYPE_RESOURCE) {
                    if (mString1 == null) {
                        mString1 = ((String) mObj1).split(":", -1)[0];
                    }
                }
                break;
            case TYPE_DATA:
                mObj1 = mData;
                break;
        }
    }

    private static String typeToString(int x) {
        switch (x) {
            case TYPE_BITMAP: return "BITMAP";
            case TYPE_ADAPTIVE_BITMAP: return "BITMAP_MASKABLE";
            case TYPE_DATA: return "DATA";
            case TYPE_RESOURCE: return "RESOURCE";
            case TYPE_URI: return "URI";
            case TYPE_URI_ADAPTIVE_BITMAP: return "URI_MASKABLE";
            default: return "UNKNOWN";
        }
    }

    /**
     * Extracts an icon from a bundle that was added using {@link #toBundle()}.
     */
    @SuppressWarnings("deprecation")
    public static @Nullable IconCompat createFromBundle(@NonNull Bundle bundle) {
        int type = bundle.getInt(EXTRA_TYPE);
        IconCompat icon = new IconCompat(type);
        icon.mInt1 = bundle.getInt(EXTRA_INT1);
        icon.mInt2 = bundle.getInt(EXTRA_INT2);
        icon.mString1 = bundle.getString(EXTRA_STRING1);
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
            case TYPE_UNKNOWN:
                icon.mObj1 = bundle.getParcelable(EXTRA_OBJ);
                break;
            case TYPE_RESOURCE:
            case TYPE_URI:
            case TYPE_URI_ADAPTIVE_BITMAP:
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
    public static @NonNull IconCompat createFromIcon(@NonNull Context context,
            @NonNull Icon icon) {
        Preconditions.checkNotNull(icon);
        return Api23Impl.createFromIcon(context, icon);
    }

    /**
     * Creates an IconCompat from an Icon.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static @NonNull IconCompat createFromIcon(@NonNull Icon icon) {
        return Api23Impl.createFromIconInner(icon);
    }

    /**
     * Creates an IconCompat from an Icon, or returns null if the given Icon is created from
     * resource 0.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static @Nullable IconCompat createFromIconOrNullIfZeroResId(@NonNull Icon icon) {
        if (Api23Impl.getType(icon) == TYPE_RESOURCE && Api23Impl.getResId(icon) == 0) {
            return null;
        }
        return Api23Impl.createFromIconInner(icon);
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
        shift.setTranslate(-(adaptiveIconBitmap.getWidth() - size) / 2.0f,
                -(adaptiveIconBitmap.getHeight() - size) / 2.0f);
        shader.setLocalMatrix(shift);
        paint.setShader(shader);
        canvas.drawCircle(center, center, radius, paint);

        canvas.setBitmap(null);
        return icon;
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static String getResPackage(Object icon) {
            return ((Icon) icon).getResPackage();
        }

        static int getType(Object icon) {
            return ((Icon) icon).getType();
        }

        static int getResId(Object icon) {
            return ((Icon) icon).getResId();
        }

        static Uri getUri(Object icon) {
            return ((Icon) icon).getUri();
        }
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        static Icon createWithAdaptiveBitmap(Bitmap bits) {
            return Icon.createWithAdaptiveBitmap(bits);
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() {
            // This class is not instantiable.
        }

        static Icon createWithAdaptiveBitmapContentUri(Uri uri) {
            return Icon.createWithAdaptiveBitmapContentUri(uri);
        }

    }

    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        static @NonNull IconCompat createFromIcon(@NonNull Context context, @NonNull Icon icon) {
            switch (getType(icon)) {
                case TYPE_RESOURCE:
                    String resPackage = getResPackage(icon);
                    try {
                        return createWithResource(getResources(context, resPackage), resPackage,
                                getResId(icon));
                    } catch (Resources.NotFoundException e) {
                        throw new IllegalArgumentException("Icon resource cannot be found");
                    }
                case TYPE_URI:
                    return createWithContentUri(getUri(icon));
                case TYPE_URI_ADAPTIVE_BITMAP:
                    return createWithAdaptiveBitmapContentUri(getUri(icon));
            }
            IconCompat iconCompat = new IconCompat(TYPE_UNKNOWN);
            iconCompat.mObj1 = icon;
            return iconCompat;
        }

        /**
         * Gets the type of the icon provided.
         * <p>
         * Note that new types may be added later, so callers should guard against other
         * types being returned. Returns {@link #TYPE_UNKNOWN} when the type cannot be
         * determined.
         */
        @IconType
        static int getType(@NonNull Object icon) {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.getType(icon);
            } else {
                try {
                    return (int) icon.getClass().getMethod("getType").invoke(icon);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Unable to get icon type " + icon, e);
                    return TYPE_UNKNOWN;
                } catch (InvocationTargetException e) {
                    Log.e(TAG, "Unable to get icon type " + icon, e);
                    return TYPE_UNKNOWN;
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "Unable to get icon type " + icon, e);
                    return TYPE_UNKNOWN;
                }
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
        static @Nullable String getResPackage(@NonNull Object icon) {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.getResPackage(icon);
            } else {
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
        }

        /**
         * Used internally to avoid casting to Icon class in code accessible to SDK < 23.
         */
        static IconCompat createFromIconInner(@NonNull Object icon) {
            Preconditions.checkNotNull(icon);
            switch (getType(icon)) {
                case TYPE_RESOURCE:
                    return createWithResource(null, getResPackage(icon), getResId(icon));
                case TYPE_URI:
                    return createWithContentUri(getUri(icon));
                case TYPE_URI_ADAPTIVE_BITMAP:
                    return createWithAdaptiveBitmapContentUri(getUri(icon));
            }
            IconCompat iconCompat = new IconCompat(TYPE_UNKNOWN);
            iconCompat.mObj1 = icon;
            return iconCompat;
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
        @DrawableRes
        static int getResId(@NonNull Object icon) {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.getResId(icon);
            } else {
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
        }

        /**
         * Gets the uri used to create this icon.
         * <p>
         * Only valid for icons of type TYPE_URI.
         * Note: This uri may not be available in the future, and it is
         * up to the caller to ensure safety if this uri is re-used and/or persisted.
         * Returns {@code null} if the uri cannot be gotten.
         */
        static @Nullable Uri getUri(@NonNull Object icon) {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.getUri(icon);
            } else {
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
        }

        static Icon toIcon(IconCompat iconCompat, Context context) {
            Icon icon;
            switch (iconCompat.mType) {
                case TYPE_UNKNOWN:
                    // When type is unknown we are just wrapping an icon.
                    return (Icon) iconCompat.mObj1;
                case TYPE_BITMAP:
                    icon = Icon.createWithBitmap((Bitmap) iconCompat.mObj1);
                    break;
                case TYPE_ADAPTIVE_BITMAP:
                    if (Build.VERSION.SDK_INT >= 26) {
                        icon = Api26Impl.createWithAdaptiveBitmap((Bitmap) iconCompat.mObj1);
                    } else {
                        icon = Icon.createWithBitmap(
                                createLegacyIconFromAdaptiveIcon((Bitmap) iconCompat.mObj1, false));
                    }
                    break;
                case TYPE_RESOURCE:
                    icon = Icon.createWithResource(iconCompat.getResPackage(), iconCompat.mInt1);
                    break;
                case TYPE_DATA:
                    icon = Icon.createWithData((byte[]) iconCompat.mObj1, iconCompat.mInt1,
                            iconCompat.mInt2);
                    break;
                case TYPE_URI:
                    icon = Icon.createWithContentUri((String) iconCompat.mObj1);
                    break;
                case TYPE_URI_ADAPTIVE_BITMAP:
                    if (Build.VERSION.SDK_INT >= 30) {
                        icon = Api30Impl.createWithAdaptiveBitmapContentUri(iconCompat.getUri());
                        break;
                    }
                    if (context == null) {
                        throw new IllegalArgumentException(
                                "Context is required to resolve the file uri of the icon: "
                                        + iconCompat.getUri());
                    }
                    InputStream is = iconCompat.getUriInputStream(context);
                    if (is == null) {
                        throw new IllegalStateException(
                                "Cannot load adaptive icon from uri: " + iconCompat.getUri());
                    }
                    if (Build.VERSION.SDK_INT >= 26) {
                        icon = Api26Impl.createWithAdaptiveBitmap(BitmapFactory.decodeStream(is));
                    } else {
                        icon = Icon.createWithBitmap(createLegacyIconFromAdaptiveIcon(
                                BitmapFactory.decodeStream(is), false));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown type");
            }
            if (iconCompat.mTintList != null) {
                icon.setTintList(iconCompat.mTintList);
            }
            if (iconCompat.mTintMode != DEFAULT_TINT_MODE) {
                icon.setTintMode(iconCompat.mTintMode);
            }
            return icon;
        }
    }
}
