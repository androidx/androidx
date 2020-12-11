/*
 * Copyright 2019 The Android Open Source Project
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

import static android.content.Context.UI_MODE_SERVICE;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.text.TextUtils;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.lang.reflect.Method;

/**
 * A class for retrieving accurate display modes for a display.
 * <p>
 * On many Android TV devices, Display.Mode may not report the accurate width and height because
 * these devices do not have powerful enough graphics pipelines to run framework code at the same
 * resolutions supported by their video pipelines. For these devices, there is no way for an app
 * to determine, for example, whether or not the current display mode is 4k, or that the display
 * supports switching to other 4k modes. This class offers a workaround for this problem.
 */
public final class DisplayCompat {
    private static final int DISPLAY_SIZE_4K_WIDTH = 3840;
    private static final int DISPLAY_SIZE_4K_HEIGHT = 2160;

    private DisplayCompat() {
        // This class is non-instantiable.
    }

    /**
     * Gets the current display mode of the given display, where the size can be relied on to
     * determine support for 4k on Android TV devices.
     */
    @NonNull
    public static ModeCompat getMode(@NonNull Context context, @NonNull Display display) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Api23Impl.getMode(context, display);
        }
        // Prior to display modes, the best we can do is return the display size as the display
        // mode.
        return new ModeCompat(getDisplaySize(context, display));
    }

    @NonNull
    private static Point getDisplaySize(@NonNull Context context, @NonNull Display display) {
        // If a workaround for the display size is present, use it.
        Point displaySize = getCurrentDisplaySizeFromWorkarounds(context, display);
        if (displaySize != null) {
            return displaySize;
        }

        displaySize = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Api17Impl.getRealSize(display, displaySize);
        } else {
            display.getSize(displaySize);
        }
        return displaySize;
    }

    /**
     * Gets the supported modes of the given display where any mode with the same size as the
     * current mode can be relied on to determine support for 4k on Android TV devices.
     */
    @NonNull
    @SuppressLint("ArrayReturn")
    public static ModeCompat[] getSupportedModes(
                @NonNull Context context, @NonNull Display display) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Api23Impl.getSupportedModes(context, display);
        }
        // Prior to display modes, the best we can do is return the current mode - the
        // current display size wrapped in a ModeCompat object.
        return new ModeCompat[] { getMode(context, display) };
    }

    /**
     * Parses a string which represents the display-size which contains 'x' as a delimiter
     * between two integers representing the display's width and height and returns the
     * display size as a Point object.
     *
     * @param displaySize a string
     * @return a Point object containing the size in x and y direction in pixels
     * @throws NumberFormatException in case the integers cannot be parsed
     */
    private static Point parseDisplaySize(@NonNull String displaySize)
            throws NumberFormatException {
        String[] displaySizeParts = displaySize.trim().split("x", -1);
        if (displaySizeParts.length == 2) {
            int width = Integer.parseInt(displaySizeParts[0]);
            int height = Integer.parseInt(displaySizeParts[1]);
            if (width > 0 && height > 0) {
                return new Point(width, height);
            }
        }
        throw new NumberFormatException();
    }

    /**
     * Reads a system property and returns its string value.
     *
     * @param name the name of the system property
     * @return the result string or null if an exception occurred
     */
    @Nullable
    private static String getSystemProperty(String name) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getMethod("get", String.class);
            return (String) getMethod.invoke(systemProperties, name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns whether the app is running on a TV device
     */
    private static boolean isTv(@NonNull Context context) {
        // See https://developer.android.com/training/tv/start/hardware.html#runtime-check.
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        return uiModeManager != null
                && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    /**
     * Helper function to determine the physical display size from the system properties only. On
     * Android TVs it is common for the UI to be configured for a lower resolution than SurfaceViews
     * can output. Before API 26 the Display object does not provide a way to identify this case,
     * and up to and including API 28 many devices still do not correctly set their hardware
     * composer output size.
     *
     * @return the physical display size, in pixels or null if the information is not available
     */
    @Nullable
    private static Point parsePhysicalDisplaySizeFromSystemProperties(@NonNull String property,
            @NonNull Display display) {
        // System properties are only relevant for the default display.
        if (display.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return null;
        }

        // Check the system property for display size.
        String displaySize = getSystemProperty(property);
        if (TextUtils.isEmpty(displaySize)) {
            return null;
        }

        try {
            return parseDisplaySize(displaySize);
        } catch (NumberFormatException e) {
            // Ignore invalid display sizes.
            return null;
        }
    }

    /**
     * Gets the current physical size of the given display in pixels from a variety of vendor
     * workarounds.
     */
    static Point getCurrentDisplaySizeFromWorkarounds(
            @NonNull Context context,
            @NonNull Display display) {
        // From API 28 treble may prevent the system from writing sys.display-size so we check
        // vendor.display-size instead.
        Point displaySize = Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                ? parsePhysicalDisplaySizeFromSystemProperties("sys.display-size", display)
                : parsePhysicalDisplaySizeFromSystemProperties("vendor.display-size", display);
        if (displaySize != null) {
            return displaySize;
        } else if (isSonyBravia4kTv(context)) {
            // Sony Android TVs advertise support for 4k output via a system feature.
            // The TV may or may not be currently in the 4k display mode. Instead, we can only
            // assume that if the current display mode is the highest display mode, then we are
            // in a 4k mode.
            return isCurrentModeTheLargestMode(display)
                    ? new Point(DISPLAY_SIZE_4K_WIDTH, DISPLAY_SIZE_4K_HEIGHT)
                    : null;
        }
        return null;
    }

    /**
     * Is the connected display is a 4k capable Sony TV?
     */
    private static boolean isSonyBravia4kTv(@NonNull Context context) {
        return isTv(context)
                && "Sony".equals(Build.MANUFACTURER)
                && Build.MODEL.startsWith("BRAVIA")
                && context.getPackageManager().hasSystemFeature(
                        "com.sony.dtv.hardware.panel.qfhd");
    }

    /**
     * Does the current display mode have the largest physical size of all supported modes?
     */
    static boolean isCurrentModeTheLargestMode(@NonNull Display display) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Api23Impl.isCurrentModeTheLargestMode(display);
        } else {
            // Prior to modes, the current mode is always the largest display mode.
            return true;
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    static class Api23Impl {
        private Api23Impl() {}

        @NonNull
        static ModeCompat getMode(@NonNull Context context, @NonNull Display display) {
            Display.Mode currentMode = display.getMode();
            Point workaroundSize = getCurrentDisplaySizeFromWorkarounds(context, display);
            // If the current mode has the wrong physical size, then correct it with the
            // workaround.
            return workaroundSize == null || physicalSizeEquals(currentMode, workaroundSize)
                    ? new ModeCompat(currentMode, /* isNative= */ true)
                    : new ModeCompat(currentMode, workaroundSize);
        }

        @NonNull
        @SuppressLint("ArrayReturn")
        public static ModeCompat[] getSupportedModes(
                    @NonNull Context context, @NonNull Display display) {
            Display.Mode[] supportedModes = display.getSupportedModes();
            ModeCompat[] supportedModesCompat = new ModeCompat[supportedModes.length];

            Display.Mode currentMode = display.getMode();
            Point workaroundSize = getCurrentDisplaySizeFromWorkarounds(context, display);
            // The workaround size not matching the current mode indicates that the Android TV
            // reports mode sizes inaccurately.
            if (workaroundSize == null || physicalSizeEquals(currentMode, workaroundSize)) {
                // This Android TV device reports display mode sizes accurately.
                for (int i = 0; i < supportedModes.length; ++i) {
                    boolean isNative = physicalSizeEquals(supportedModes[i], currentMode);
                    supportedModesCompat[i] = new ModeCompat(supportedModes[i], isNative);
                }
            } else {
                // This Android TV device does NOT report display mode sizes accurately.
                for (int i = 0; i < supportedModes.length; ++i) {
                    // A mode with the same size as the current mode should use the workaround size.
                    supportedModesCompat[i] = physicalSizeEquals(supportedModes[i], currentMode)
                            ? new ModeCompat(supportedModes[i], workaroundSize)
                            : new ModeCompat(supportedModes[i], /* isNative= */ false);
                }
            }
            return supportedModesCompat;
        }

        static boolean isCurrentModeTheLargestMode(@NonNull Display display) {
            Display.Mode currentMode = display.getMode();
            Display.Mode[] supportedModes = display.getSupportedModes();
            for (int i = 0; i < supportedModes.length; ++i) {
                if (currentMode.getPhysicalHeight() < supportedModes[i].getPhysicalHeight()
                        || currentMode.getPhysicalWidth() < supportedModes[i].getPhysicalWidth()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns true if mode.getPhysicalWidth and mode.getPhysicalHeight are equal to the given
         * size.
         */
        static boolean physicalSizeEquals(Display.Mode mode, Point size) {
            return (mode.getPhysicalWidth() == size.x && mode.getPhysicalHeight() == size.y)
                    || (mode.getPhysicalWidth() == size.y && mode.getPhysicalHeight() == size.x);
        }

        /**
         * Returns true if mode.getPhysicalWidth and mode.getPhysicalHeight are equal to the size
         * of another mode.
         */
        static boolean physicalSizeEquals(Display.Mode mode, Display.Mode otherMode) {
            return mode.getPhysicalWidth() == otherMode.getPhysicalWidth()
                    && mode.getPhysicalHeight() == otherMode.getPhysicalHeight();
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    static class Api17Impl {
        private Api17Impl() {}

        static void getRealSize(Display display, Point displaySize) {
            display.getRealSize(displaySize);
        }
    }

    /**
     * Compat class which provides access to the underlying display mode, if there is one, and
     * a more reliable display mode size.
     */
    public static final class ModeCompat {
        private final Display.Mode mMode;
        private final Point mPhysicalSize;
        private final boolean mIsNative;

        /**
         * Create a ModeCompat object that does not wrap any Display.Mode object, but only
         * contains the display mode size.
         *
         * @param physicalSize the physical size of the display mode
         */
        ModeCompat(@NonNull Point physicalSize) {
            Preconditions.checkNotNull(physicalSize, "physicalSize == null");
            mPhysicalSize = physicalSize;
            mMode = null;
            mIsNative = true;
        }

        /**
         * Create a ModeCompat object that wraps a Display.Mode that has an accurate physical size.
         *
         * @param mode the wrapped Display.Mode object
         */
        @RequiresApi(Build.VERSION_CODES.M)
        ModeCompat(@NonNull Display.Mode mode, boolean isNative) {
            Preconditions.checkNotNull(mode, "mode == null, can't wrap a null reference");
            // This simplifies the getPhysicalWidth() / getPhysicalHeight functions below
            mPhysicalSize = new Point(mode.getPhysicalWidth(), mode.getPhysicalHeight());
            mMode = mode;
            mIsNative = isNative;
        }

        /**
         * Create a ModeCompat object that wraps a Display.Mode, but with a more accurate
         * display mode size.
         *
         * @param mode the wrapped Display.Mode object
         * @param physicalSize the true physical size of the display mode
         *
         */
        @RequiresApi(Build.VERSION_CODES.M)
        ModeCompat(@NonNull Display.Mode mode, @NonNull Point physicalSize) {
            Preconditions.checkNotNull(mode, "mode == null, can't wrap a null reference");
            Preconditions.checkNotNull(physicalSize, "physicalSize == null");
            mPhysicalSize = physicalSize;
            mMode = mode;
            mIsNative = true;
        }

        /**
         * Returns the physical width of the given display when configured in this mode.
         */
        public int getPhysicalWidth() {
            return mPhysicalSize.x;
        }

        /**
         * Returns the physical height of the given display when configured in this mode.
         */
        public int getPhysicalHeight() {
            return mPhysicalSize.y;
        }

        /**
         * This field indicates whether a mode has the same resolution as the current display mode.
         * <p>
         * This field does *not* indicate the native resolution of the display.
         *
         * @return true if this mode is the same resolution as the current display mode.
         * @deprecated Use {@link DisplayCompat#getMode} to retrieve the resolution of the current
         *             display mode.
         */
        @Deprecated
        public boolean isNative() {
            return mIsNative;
        }

        /**
         * Returns the wrapped object Display.Mode, which may be null if no mode is available.
         */
        @RequiresApi(Build.VERSION_CODES.M)
        @Nullable
        public Display.Mode toMode() {
            return mMode;
        }
    }
}

