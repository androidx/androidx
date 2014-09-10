/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;

/**
 * Helper for accessing features in {@link android.graphics.drawable.Drawable}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class DrawableCompat {
    /**
     * Interface for the full API.
     */
    interface DrawableImpl {
        void jumpToCurrentState(Drawable drawable);
        void setAutoMirrored(Drawable drawable, boolean mirrored);
        boolean isAutoMirrored(Drawable drawable);
        void setHotspot(Drawable drawable, float x, float y);
        void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom);
        void setTint(Drawable drawable, int tint);
        void setTintList(Drawable drawable, ColorStateList tint);
        void setTintMode(Drawable drawable, PorterDuff.Mode tintMode);
    }

    /**
     * Interface implementation that doesn't use anything about v4 APIs.
     */
    static class BaseDrawableImpl implements DrawableImpl {
        @Override
        public void jumpToCurrentState(Drawable drawable) {
        }

        @Override
        public void setAutoMirrored(Drawable drawable, boolean mirrored) {
        }

        @Override
        public boolean isAutoMirrored(Drawable drawable) {
            return false;
        }

        @Override
        public void setHotspot(Drawable drawable, float x, float y) {
        }

        @Override
        public void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom) {
        }

        @Override
        public void setTint(Drawable drawable, int tint) {
        }

        @Override
        public void setTintList(Drawable drawable, ColorStateList tint) {
        }

        @Override
        public void setTintMode(Drawable drawable, PorterDuff.Mode tintMode) {
        }
    }

    /**
     * Interface implementation for devices with at least v11 APIs.
     */
    static class HoneycombDrawableImpl extends BaseDrawableImpl {
        @Override
        public void jumpToCurrentState(Drawable drawable) {
            DrawableCompatHoneycomb.jumpToCurrentState(drawable);
        }
    }

    /**
     * Interface implementation for devices with at least KitKat APIs.
     */
    static class KitKatDrawableImpl extends HoneycombDrawableImpl {
        @Override
        public void setAutoMirrored(Drawable drawable, boolean mirrored) {
            DrawableCompatKitKat.setAutoMirrored(drawable, mirrored);
        }

        @Override
        public boolean isAutoMirrored(Drawable drawable) {
            return DrawableCompatKitKat.isAutoMirrored(drawable);
        }
    }

    /**
     * Interface implementation for devices with at least L APIs.
     */
    static class LDrawableImpl extends KitKatDrawableImpl {
        @Override
        public void setHotspot(Drawable drawable, float x, float y) {
            DrawableCompatL.setHotspot(drawable, x, y);
        }

        @Override
        public void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom) {
            DrawableCompatL.setHotspotBounds(drawable, left, top, right, bottom);
        }

        @Override
        public void setTint(Drawable drawable, int tint) {
            DrawableCompatL.setTint(drawable, tint);
        }

        @Override
        public void setTintList(Drawable drawable, ColorStateList tint) {
            DrawableCompatL.setTintList(drawable, tint);
        }

        @Override
        public void setTintMode(Drawable drawable, PorterDuff.Mode tintMode) {
            DrawableCompatL.setTintMode(drawable, tintMode);
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final DrawableImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 21) {
            IMPL = new LDrawableImpl();
        } else if (version >= 19) {
            IMPL = new KitKatDrawableImpl();
        } else if (version >= 11) {
            IMPL = new HoneycombDrawableImpl();
        } else {
            IMPL = new BaseDrawableImpl();
        }
    }

    /**
     * Call {@link Drawable#jumpToCurrentState() Drawable.jumpToCurrentState()}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#HONEYCOMB}
     * device this method does nothing.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static void jumpToCurrentState(Drawable drawable) {
        IMPL.jumpToCurrentState(drawable);
    }

    /**
     * Set whether this Drawable is automatically mirrored when its layout
     * direction is RTL (right-to left). See
     * {@link android.util.LayoutDirection}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#KITKAT} device
     * this method does nothing.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param mirrored Set to true if the Drawable should be mirrored, false if
     *            not.
     */
    public static void setAutoMirrored(Drawable drawable, boolean mirrored) {
        IMPL.setAutoMirrored(drawable, mirrored);
    }

    /**
     * Tells if this Drawable will be automatically mirrored when its layout
     * direction is RTL right-to-left. See {@link android.util.LayoutDirection}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#KITKAT} device
     * this method returns false.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @return boolean Returns true if this Drawable will be automatically
     *         mirrored.
     */
    public static boolean isAutoMirrored(Drawable drawable) {
        return IMPL.isAutoMirrored(drawable);
    }

    /**
     * Specifies the hotspot's location within the drawable.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param x The X coordinate of the center of the hotspot
     * @param y The Y coordinate of the center of the hotspot
     */
    public static void setHotspot(Drawable drawable, float x, float y) {
        IMPL.setHotspot(drawable, x, y);
    }

    /**
     * Sets the bounds to which the hotspot is constrained, if they should be
     * different from the drawable bounds.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static void setHotspotBounds(Drawable drawable, int left, int top,
            int right, int bottom) {
        IMPL.setHotspotBounds(drawable, left, top, right, bottom);
    }

    /**
     * Specifies a tint for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint Color to use for tinting this drawable
     */
    public static void setTint(Drawable drawable, int tint) {
        IMPL.setTint(drawable, tint);
    }

    /**
     * Specifies a tint for {@code drawable} as a color state list.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint Color state list to use for tinting this drawable, or null to
     *            clear the tint
     */
    public static void setTintList(Drawable drawable, ColorStateList tint) {
        IMPL.setTintList(drawable, tint);
    }

    /**
     * Specifies a tint blending mode for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tintMode Color state list to use for tinting this drawable, or null to
     *            clear the tint
     * @param tintMode A Porter-Duff blending mode
     */
    public static void setTintMode(Drawable drawable, PorterDuff.Mode tintMode) {
        IMPL.setTintMode(drawable, tintMode);
    }
}
