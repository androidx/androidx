/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.theme;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.RequiresCarApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RequiresCarApi(9)
/* Represents the theme style configuration for the car app. */
public final class CarAppTheme {
    private CarAppTheme() {} // Non-instantiable

    /** Respect system/OEM customization (colors, shapes, typography). */
    public static final int SYSTEM_THEME = 0;

    /** Request standard/neutral theme experience, bypassing OEM overrides. */
    public static final int APP_THEME = 1;

    @IntDef({SYSTEM_THEME, APP_THEME})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface Theme {}
}
