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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;

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
        DayNightStyle.ALWAYS_LIGHT,
        DayNightStyle.ALWAYS_DARK,
        DayNightStyle.FORCE_NIGHT,
        DayNightStyle.FORCE_DAY,
})
@Retention(SOURCE)
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

    /** Sets the color to be locked to a light variant during day and night. */
    int ALWAYS_LIGHT = 2;

    /** Sets the color to be locked ot a dark variant during day and night. */
    int ALWAYS_DARK = 3;

    /**
     * Sets the foreground color to be locked to the night version, which assumes the app content
     * background is always dark during both day and night.
     *
     * @deprecated Use {@link #ALWAYS_LIGHT} instead.
     */
    @Deprecated
    int FORCE_NIGHT = 4;

    /**
     * Sets the foreground color to be locked to the day version, which assumes the app content
     * background is always light during both day and night.
     *
     * @deprecated Use {@link #ALWAYS_DARK} instead.
     */
    @Deprecated
    int FORCE_DAY = 5;

}
