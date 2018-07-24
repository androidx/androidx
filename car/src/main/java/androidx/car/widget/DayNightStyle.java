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
 * @deprecated Set the day/night behavior via themes instead.
 */
@IntDef({
        DayNightStyle.AUTO,
        DayNightStyle.AUTO_INVERSE,
        DayNightStyle.ALWAYS_LIGHT,
        DayNightStyle.ALWAYS_DARK
})
@Retention(SOURCE)
@Deprecated
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
}
