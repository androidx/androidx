/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.view;

import static android.os.Build.VERSION.SDK_INT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.PointerIcon;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Helper for accessing features in {@link PointerIcon} in a backwards compatible
 * fashion.
 */
public final class PointerIconCompat {
    /** Synonym for {@link PointerIcon#TYPE_NULL} */
    public static final int TYPE_NULL = 0;

    /** Synonym for {@link PointerIcon#TYPE_ARROW} */
    public static final int TYPE_ARROW = 1000;

    /** Synonym for {@link PointerIcon#TYPE_CONTEXT_MENU} */
    public static final int TYPE_CONTEXT_MENU = 1001;

    /** Synonym for {@link PointerIcon#TYPE_HAND} */
    public static final int TYPE_HAND = 1002;

    /** Synonym for {@link PointerIcon#TYPE_HELP} */
    public static final int TYPE_HELP = 1003;

    /** Synonym for {@link PointerIcon#TYPE_WAIT} */
    public static final int TYPE_WAIT = 1004;

    /** Synonym for {@link PointerIcon#TYPE_CELL} */
    public static final int TYPE_CELL = 1006;

    /** Synonym for {@link PointerIcon#TYPE_CROSSHAIR} */
    public static final int TYPE_CROSSHAIR = 1007;

    /** Synonym for {@link PointerIcon#TYPE_TEXT} */
    public static final int TYPE_TEXT = 1008;

    /** Synonym for {@link PointerIcon#TYPE_VERTICAL_TEXT} */
    public static final int TYPE_VERTICAL_TEXT = 1009;

    /** Synonym for {@link PointerIcon#TYPE_ALIAS} */
    public static final int TYPE_ALIAS = 1010;

    /** Synonym for {@link PointerIcon#TYPE_COPY} */
    public static final int TYPE_COPY = 1011;

    /** Synonym for {@link PointerIcon#TYPE_NO_DROP} */
    public static final int TYPE_NO_DROP = 1012;

    /** Synonym for {@link PointerIcon#TYPE_ALL_SCROLL} */
    public static final int TYPE_ALL_SCROLL = 1013;

    /** Synonym for {@link PointerIcon#TYPE_HORIZONTAL_DOUBLE_ARROW} */
    public static final int TYPE_HORIZONTAL_DOUBLE_ARROW = 1014;

    /** Synonym for {@link PointerIcon#TYPE_VERTICAL_DOUBLE_ARROW} */
    public static final int TYPE_VERTICAL_DOUBLE_ARROW = 1015;

    /** Synonym for {@link PointerIcon#TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW} */
    public static final int TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW = 1016;

    /** Synonym for {@link PointerIcon#TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW} */
    public static final int TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW = 1017;

    /** Synonym for {@link PointerIcon#TYPE_ZOOM_IN} */
    public static final int TYPE_ZOOM_IN = 1018;

    /** Synonym for {@link PointerIcon#TYPE_ZOOM_OUT} */
    public static final int TYPE_ZOOM_OUT = 1019;

    /** Synonym for {@link PointerIcon#TYPE_GRAB} */
    public static final int TYPE_GRAB = 1020;

    /** Synonym for {@link PointerIcon#TYPE_GRABBING} */
    public static final int TYPE_GRABBING = 1021;

    /** Synonym for {@link PointerIcon#TYPE_DEFAULT} */
    public static final int TYPE_DEFAULT = TYPE_ARROW;

    private final PointerIcon mPointerIcon;

    private PointerIconCompat(PointerIcon pointerIcon) {
        mPointerIcon = pointerIcon;
    }

    /**
     * @hide
     */
    @Nullable
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public Object getPointerIcon() {
        return mPointerIcon;
    }

    /**
     * Gets a system pointer icon for the given style.
     * If style is not recognized, returns the default pointer icon.
     *
     * @param context The context.
     * @param style The pointer icon style.
     * @return The pointer icon.
     */
    @NonNull
    public static PointerIconCompat getSystemIcon(@NonNull Context context, int style) {
        if (SDK_INT >= 24) {
            return new PointerIconCompat(Api24Impl.getSystemIcon(context, style));
        } else {
            return new PointerIconCompat(null);
        }
    }

    /**
     * Creates a custom pointer from the given bitmap and hotspot information.
     *
     * @param bitmap The bitmap for the icon.
     * @param hotSpotX The X offset of the pointer icon hotspot in the bitmap.
     *        Must be within the [0, bitmap.getWidth()) range.
     * @param hotSpotY The Y offset of the pointer icon hotspot in the bitmap.
     *        Must be within the [0, bitmap.getHeight()) range.
     * @return A pointer icon for this bitmap.
     *
     * @throws IllegalArgumentException if bitmap is null, or if the x/y hotspot
     *         parameters are invalid.
     */
    @NonNull
    public static PointerIconCompat create(@NonNull Bitmap bitmap, float hotSpotX, float hotSpotY) {
        if (SDK_INT >= 24) {
            return new PointerIconCompat(Api24Impl.create(bitmap, hotSpotX, hotSpotY));
        } else {
            return new PointerIconCompat(null);
        }
    }

    /**
     * Loads a custom pointer icon from an XML resource.
     * <p>
     * The XML resource should have the following form:
     * <code>
     * &lt;?xml version="1.0" encoding="utf-8"?&gt;
     * &lt;pointer-icon xmlns:android="http://schemas.android.com/apk/res/android"
     *   android:bitmap="@drawable/my_pointer_bitmap"
     *   android:hotSpotX="24"
     *   android:hotSpotY="24" /&gt;
     * </code>
     * </p>
     *
     * @param resources The resources object.
     * @param resourceId The resource id.
     * @return The pointer icon.
     *
     * @throws Resources.NotFoundException if the resource was not found or the drawable
     * linked in the resource was not found.
     */
    @NonNull
    public static PointerIconCompat load(@NonNull Resources resources, int resourceId) {
        if (SDK_INT >= 24) {
            return new PointerIconCompat(Api24Impl.load(resources, resourceId));
        } else {
            return new PointerIconCompat(null);
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static PointerIcon getSystemIcon(Context context, int type) {
            return PointerIcon.getSystemIcon(context, type);
        }

        @DoNotInline
        static PointerIcon create(Bitmap bitmap, float hotSpotX, float hotSpotY) {
            return PointerIcon.create(bitmap, hotSpotX, hotSpotY);
        }

        @DoNotInline
        static PointerIcon load(Resources resources, int resourceId) {
            return PointerIcon.load(resources, resourceId);
        }
    }
}
