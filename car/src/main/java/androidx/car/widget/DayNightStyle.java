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

package androidx.car.widget;

import android.support.annotation.IntDef;

/**
 * Specifies how the system UI should respond to day/night mode events.
 *
 * <p>By default, the Android Auto system UI assumes the app content background is light during the
 * day and dark during the night. The system UI updates the foreground color (such as status bar
 * icon colors) to be dark during day mode and light during night mode. By setting the
 * DayNightStyle, the app can specify how the system should respond to a day/night mode event. For
 * example, if the app has a dark content background for both day and night time, the app can tell
 * the system to use {@link #FORCE_NIGHT} style so the foreground color is locked to light color for
 * both cases.
 *
 * <p>Note: Not all system UI elements can be customized with a DayNightStyle.
 */
@IntDef({
        DayNightStyle.AUTO,
        DayNightStyle.AUTO_INVERSE,
        DayNightStyle.FORCE_NIGHT,
        DayNightStyle.FORCE_DAY,
})
public @interface DayNightStyle {
    /**
     * Sets the foreground color to be automatically changed based on day/night mode, assuming the
     * app content background is light during the day and dark during the night.
     *
     * <p>This is the default behavior.
     */
    int AUTO = 0;

    /**
     * Sets the foreground color to be automatically changed based on day/night mode, assuming the
     * app content background is dark during the day and light during the night.
     */
    int AUTO_INVERSE = 1;

    /**
     * Sets the foreground color to be locked to the night version, which assumes the app content
     * background is always dark during both day and night.
     */
    int FORCE_NIGHT = 2;

    /**
     * Sets the foreground color to be locked to the day version, which assumes the app content
     * background is always light during both day and night.
     */
    int FORCE_DAY = 3;
}
