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

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper for accessing features in {@link android.hardware.display.DisplayManager}.
 */
@SuppressWarnings("unused")
public final class DisplayManagerCompat {

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

    private final Context mContext;

    private DisplayManagerCompat(Context context) {
        mContext = context;
    }

    /**
     * Gets an instance of the display manager given the context.
     */
    @NonNull
    public static DisplayManagerCompat getInstance(@NonNull Context context) {
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
    @Nullable
    @SuppressWarnings("deprecation")
    public Display getDisplay(int displayId) {
        DisplayManager displayManager =
                (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        return displayManager.getDisplay(displayId);
    }

    /**
     * Gets all currently valid logical displays.
     *
     * @return An array containing all displays.
     */
    @SuppressWarnings("deprecation")
    @NonNull
    public Display[] getDisplays() {
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
    @NonNull
    @SuppressWarnings("deprecation")
    public Display[] getDisplays(@Nullable String category) {
        return ((DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE)).getDisplays();
    }
}
