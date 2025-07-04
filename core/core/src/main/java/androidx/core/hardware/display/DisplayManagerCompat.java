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

package androidx.core.hardware.display;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Helper for accessing features in {@link android.hardware.display.DisplayManager}.
 */
public final class DisplayManagerCompat {

    /**
     * An internal category to get all the displays. This was added in SC_V2 and should only be
     * used internally. This is not a stable API so it should not be make public.
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static final String DISPLAY_CATEGORY_ALL =
            "android.hardware.display.category.ALL_INCLUDING_DISABLED";

    /**
     * An internal copy of the type from the platform. This was added in SDK 17 and should only be
     * used internally. This is not a stable API so it should not be made public.
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static final int DISPLAY_TYPE_INTERNAL = 1;

    /**
     * Display category: Presentation displays.
     * <p>
     * This category can be used to identify secondary displays that are suitable for
     * use as presentation displays.
     * </p>
     *
     * @see android.app.Presentation for information about presenting content
     * on secondary displays.
     * @see #getDisplays(String)
     */
    public static final String DISPLAY_CATEGORY_PRESENTATION =
            "android.hardware.display.category.PRESENTATION";

    /**
     * Display category: Built in displays.
     * <p>
     *     This category can be used to identify displays that are built in to the device.
     * </p>
     * @see #getDisplays(String)
     */
    @ExperimentalDisplayApi
    public static final String DISPLAY_CATEGORY_BUILT_IN_DISPLAYS =
            "android.hardware.display.category.BUILT_IN_DISPLAYS";

    private final Context mContext;

    private DisplayManagerCompat(Context context) {
        mContext = context;
    }

    /**
     * Gets an instance of the display manager given the context.
     */
    public static @NonNull DisplayManagerCompat getInstance(@NonNull Context context) {
        return new DisplayManagerCompat(context);
    }

    /**
     * Gets information about a logical display.
     *
     * The display metrics may be adjusted to provide compatibility
     * for legacy applications.
     *
     * @param displayId The logical display id.
     * @return The display object, or null if there is no valid display with the given id.
     */
    public @Nullable Display getDisplay(int displayId) {
        DisplayManager displayManager =
                (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        return displayManager.getDisplay(displayId);
    }

    /**
     * Gets all currently valid logical displays.
     *
     * @return An array containing all displays.
     */
    public Display @NonNull [] getDisplays() {
        return ((DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE)).getDisplays();
    }

    /**
     * Gets all currently valid logical displays of the specified category.
     * <p>
     * When there are multiple displays in a category the returned displays are sorted
     * of preference.  For example, if the requested category is
     * {@link #DISPLAY_CATEGORY_PRESENTATION} and there are multiple presentation displays
     * then the displays are sorted so that the first display in the returned array
     * is the most preferred presentation display.  The application may simply
     * use the first display or allow the user to choose.
     * </p>
     *
     * @param category The requested display category or null to return all displays.
     * @return An array containing all displays sorted by order of preference.
     *
     * @see #DISPLAY_CATEGORY_PRESENTATION
     */
    public Display @NonNull [] getDisplays(@Nullable String category) {
        DisplayManager displayManager = (DisplayManager) mContext
                .getSystemService(Context.DISPLAY_SERVICE);
        if (DISPLAY_CATEGORY_BUILT_IN_DISPLAYS.equals(category)) {
            return computeBuiltInDisplays(displayManager);
        } else {
            return ((DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE))
                    .getDisplays(category);
        }
    }

    /**
     * Returns an array of built in displays, a built in display is one that is physically part
     * of the device.
     */
    private static Display[] computeBuiltInDisplays(DisplayManager displayManager) {
        final Display[] allDisplays;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            allDisplays = displayManager
                    .getDisplays(DISPLAY_CATEGORY_ALL);

        } else {
            allDisplays = displayManager.getDisplays();
        }
        final int numberOfBuiltInDisplays =
                numberOfDisplaysByType(DISPLAY_TYPE_INTERNAL, allDisplays);
        final Display[] builtInDisplays = new Display[numberOfBuiltInDisplays];

        int builtInDisplayIndex = 0;
        for (int i = 0; i < allDisplays.length; i++) {
            Display display = allDisplays[i];
            if (DISPLAY_TYPE_INTERNAL == getTypeCompat(display)) {
                builtInDisplays[builtInDisplayIndex] = display;
                builtInDisplayIndex = builtInDisplayIndex + 1;
            }
        }
        return builtInDisplays;
    }

    /**
     * Returns the number of displays that have the matching type.
     */
    private static int numberOfDisplaysByType(int type, Display[] displays) {
        int count = 0;
        for (int i = 0; i < displays.length; i++) {
            Display display = displays[i];
            if (type == getTypeCompat(display)) {
                count = count + 1;
            }
        }
        return count;
    }

    /**
     * An internal method to get the type of the display using reflection. This is used to support
     * backporting of getting a display of a specific type. The preferred way to expose displays is
     * to have a category and have developers get them using the category.
     */
    @SuppressLint("BanUncheckedReflection")
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static int getTypeCompat(@NonNull Display display) {
        try {
            return (Integer) Objects.requireNonNull(
                    Display.class.getMethod("getType").invoke(display)
            );
        } catch (NoSuchMethodException noSuchMethodException) {
            return 0;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
