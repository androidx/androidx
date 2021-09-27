/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.NonNull;

/**
 * Utility class for {@link Display} related information.
 */
public final class DisplayUtil {
    /**
     * Retrieves the display which has the max size among all displays.
     */
    @SuppressWarnings("deprecation") /* getRealSize */
    @NonNull
    public static Display getMaxSizeDisplay(@NonNull DisplayManager displayManager) {
        Display maxDisplay = null;
        int maxDisplaySize = -1;

        for (Display display : displayManager.getDisplays()) {
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            if (displaySize.x * displaySize.y > maxDisplaySize) {
                maxDisplaySize = displaySize.x * displaySize.y;
                maxDisplay = display;
            }
        }

        if (maxDisplay == null) {
            throw new IllegalArgumentException("No display can be found from the input display "
                    + "manager!");
        }

        return maxDisplay;
    }

    /**
     * Retrieves the {@link DisplayManager} instance.
     */
    @NonNull
    public static DisplayManager getDisplayManager(@NonNull Context context) {
        return (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    }

    private DisplayUtil() {}
}
