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

package android.support.v4.graphics.drawable;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

/**
 * Helper for accessing features in {@link android.graphics.drawable.Icon}.
 */
public class IconCompat {

    // Ratio of expected size to actual icon size
    private static final float ADAPTIVE_ICON_INSET_FACTOR = 1 / 4f;
    private static final float DEFAULT_VIEW_PORT_SCALE = 1 / (1 + 2 * ADAPTIVE_ICON_INSET_FACTOR);
    private static final float ICON_DIAMETER_FACTOR = 176f / 192;
    private static final float BLUR_FACTOR = 0.5f / 48;
    private static final float KEY_SHADOW_OFFSET_FACTOR = 1f / 48;

    private static final int KEY_SHADOW_ALPHA = 61;
    private static final int AMBIENT_SHADOW_ALPHA = 30;

    private static final int TYPE_BITMAP   = 1;
    private static final int TYPE_RESOURCE = 2;
    private static final int TYPE_DATA     = 3;
    private static final int TYPE_URI      = 4;
    private static final int TYPE_ADAPTIVE_BITMAP = 5;

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
        rep.mObj1 = context;
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
     * Convert this compat object to {@link Icon} object.
     *
     * @return {@link Icon} object
     */
    @RequiresApi(23)
    public Icon toIcon() {
        switch (mType) {
            case TYPE_BITMAP:
                return Icon.createWithBitmap((Bitmap) mObj1);
            case TYPE_ADAPTIVE_BITMAP:
                if (Build.VERSION.SDK_INT >= 26) {
                    return Icon.createWithAdaptiveBitmap((Bitmap) mObj1);
                } else {
                    return Icon.createWithBitmap(createLegacyIconFromAdaptiveIcon((Bitmap) mObj1));
                }
            case TYPE_RESOURCE:
                return Icon.createWithResource((Context) mObj1, mInt1);
            case TYPE_DATA:
                return Icon.createWithData((byte[]) mObj1, mInt1, mInt2);
            case TYPE_URI:
                return Icon.createWithContentUri((String) mObj1);
            default:
                throw new IllegalArgumentException("Unknown type");
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void addToShortcutIntent(Intent outIntent) {
        switch (mType) {
            case TYPE_BITMAP:
                outIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, (Bitmap) mObj1);
                break;
            case TYPE_ADAPTIVE_BITMAP:
                outIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON,
                        createLegacyIconFromAdaptiveIcon((Bitmap) mObj1));
                break;
            case TYPE_RESOURCE:
                outIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext((Context) mObj1, mInt1));
                break;
            default:
                throw new IllegalArgumentException("Icon type not supported for intent shortcuts");
        }
    }

    /**
     * Converts a bitmap following the adaptive icon guide lines, into a bitmap following the
     * shortcut icon guide lines.
     * The returned bitmap will always have same width and height and clipped to a circle.
     */
    @VisibleForTesting
    static Bitmap createLegacyIconFromAdaptiveIcon(Bitmap adaptiveIconBitmap) {
        int size = (int) (DEFAULT_VIEW_PORT_SCALE * Math.min(adaptiveIconBitmap.getWidth(),
                adaptiveIconBitmap.getHeight()));

        Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        float center = size * 0.5f;
        float radius = center * ICON_DIAMETER_FACTOR;

        // Draw key shadow
        float blur = BLUR_FACTOR * size;
        paint.setColor(Color.TRANSPARENT);
        paint.setShadowLayer(blur, 0, KEY_SHADOW_OFFSET_FACTOR * size, KEY_SHADOW_ALPHA << 24);
        canvas.drawCircle(center, center, radius, paint);

        // Draw ambient shadow
        paint.setShadowLayer(blur, 0, 0, AMBIENT_SHADOW_ALPHA << 24);
        canvas.drawCircle(center, center, radius, paint);
        paint.clearShadowLayer();

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
