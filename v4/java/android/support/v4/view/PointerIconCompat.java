/**
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.os.BuildCompat;

/**
 * Helper for accessing features in {@link android.view.PointerIcon} introduced after API
 * level 4 in a backwards compatible fashion.
 */
public final class PointerIconCompat {
    /** Synonym for {@link android.view.PointerIcon#TYPE_NULL} */
    public static final int TYPE_NULL = 0;

    /** Synonym for {@link android.view.PointerIcon#TYPE_ARROW} */
    public static final int TYPE_ARROW = 1000;

    /** Synonym for {@link android.view.PointerIcon#TYPE_CONTEXT_MENU} */
    public static final int TYPE_CONTEXT_MENU = 1001;

    /** Synonym for {@link android.view.PointerIcon#TYPE_HAND} */
    public static final int TYPE_HAND = 1002;

    /** Synonym for {@link android.view.PointerIcon#TYPE_HELP} */
    public static final int TYPE_HELP = 1003;

    /** Synonym for {@link android.view.PointerIcon#TYPE_WAIT} */
    public static final int TYPE_WAIT = 1004;

    /** Synonym for {@link android.view.PointerIcon#TYPE_CELL} */
    public static final int TYPE_CELL = 1006;

    /** Synonym for {@link android.view.PointerIcon#TYPE_CROSSHAIR} */
    public static final int TYPE_CROSSHAIR = 1007;

    /** Synonym for {@link android.view.PointerIcon#TYPE_TEXT} */
    public static final int TYPE_TEXT = 1008;

    /** Synonym for {@link android.view.PointerIcon#TYPE_VERTICAL_TEXT} */
    public static final int TYPE_VERTICAL_TEXT = 1009;

    /** Synonym for {@link android.view.PointerIcon#TYPE_ALIAS} */
    public static final int TYPE_ALIAS = 1010;

    /** Synonym for {@link android.view.PointerIcon#TYPE_COPY} */
    public static final int TYPE_COPY = 1011;

    /** Synonym for {@link android.view.PointerIcon#TYPE_NO_DROP} */
    public static final int TYPE_NO_DROP = 1012;

    /** Synonym for {@link android.view.PointerIcon#TYPE_ALL_SCROLL} */
    public static final int TYPE_ALL_SCROLL = 1013;

    /** Synonym for {@link android.view.PointerIcon#TYPE_HORIZONTAL_DOUBLE_ARROW} */
    public static final int TYPE_HORIZONTAL_DOUBLE_ARROW = 1014;

    /** Synonym for {@link android.view.PointerIcon#TYPE_VERTICAL_DOUBLE_ARROW} */
    public static final int TYPE_VERTICAL_DOUBLE_ARROW = 1015;

    /** Synonym for {@link android.view.PointerIcon#TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW} */
    public static final int TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW = 1016;

    /** Synonym for {@link android.view.PointerIcon#TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW} */
    public static final int TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW = 1017;

    /** Synonym for {@link android.view.PointerIcon#TYPE_ZOOM_IN} */
    public static final int TYPE_ZOOM_IN = 1018;

    /** Synonym for {@link android.view.PointerIcon#TYPE_ZOOM_OUT} */
    public static final int TYPE_ZOOM_OUT = 1019;

    /** Synonym for {@link android.view.PointerIcon#TYPE_GRAB} */
    public static final int TYPE_GRAB = 1020;

    /** Synonym for {@link android.view.PointerIcon#TYPE_GRABBING} */
    public static final int TYPE_GRABBING = 1021;

    /** Synonym for {@link android.view.PointerIcon#TYPE_DEFAULT} */
    public static final int TYPE_DEFAULT = TYPE_ARROW;


    private Object mPointerIcon;

    private PointerIconCompat(Object pointerIcon) {
        mPointerIcon = pointerIcon;
    }

    /**
     * @hide
     */
    public Object getPointerIcon() {
        return mPointerIcon;
    }

    interface PointerIconCompatImpl {
        Object getSystemIcon(Context context, int style);
        Object create(Bitmap bitmap, float hotSpotX, float hotSpotY);
        Object load(Resources resources, int resourceId);
    }

    static class BasePointerIconCompatImpl implements PointerIconCompatImpl {
        @Override
        public Object getSystemIcon(Context context, int style) {
            return null;
        }

        @Override
        public Object create(Bitmap bitmap, float hotSpotX, float hotSpotY) {
            return null;
        }

        @Override
        public Object load(Resources resources, int resourceId) {
            return null;
        }
    }

    static class Api24PointerIconCompatImpl extends BasePointerIconCompatImpl {
        @Override
        public Object getSystemIcon(Context context, int style) {
            return PointerIconCompatApi24.getSystemIcon(context, style);
        }

        @Override
        public Object create(Bitmap bitmap, float hotSpotX, float hotSpotY) {
            return PointerIconCompatApi24.create(bitmap, hotSpotX, hotSpotY);
        }

        @Override
        public Object load(Resources resources, int resourceId) {
            return PointerIconCompatApi24.load(resources, resourceId);
        }
    }

    static final PointerIconCompatImpl IMPL;
    static {
        if (BuildCompat.isAtLeastN()) {
            IMPL = new Api24PointerIconCompatImpl();
        } else {
            IMPL = new BasePointerIconCompatImpl();
        }
    }

    /**
     * Gets a system pointer icon for the given style.
     * If style is not recognized, returns the default pointer icon.
     *
     * @param context The context.
     * @param style The pointer icon style.
     * @return The pointer icon.
     *
     * @throws IllegalArgumentException if context is null.
     */
    public static PointerIconCompat getSystemIcon(Context context, int style) {
        return new PointerIconCompat(IMPL.getSystemIcon(context, style));
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
    public static PointerIconCompat create(Bitmap bitmap, float hotSpotX, float hotSpotY) {
        return new PointerIconCompat(IMPL.create(bitmap, hotSpotX, hotSpotY));
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
     * @throws IllegalArgumentException if resources is null.
     * @throws Resources.NotFoundException if the resource was not found or the drawable
     * linked in the resource was not found.
     */
    public static PointerIconCompat load(Resources resources, int resourceId) {
        return new PointerIconCompat(IMPL.load(resources, resourceId));
    }
}
