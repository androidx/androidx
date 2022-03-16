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

package androidx.wear.tiles.material;

import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * Contains the default color values used by all Tiles Components.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class Colors {
    private Colors() {}

    @ColorInt public static final int PRIMARY = 0xFFAECBFA;
    @ColorInt public static final int ON_PRIMARY = 0xFF202124;
    @ColorInt public static final int SURFACE = 0xFF202124;
    @ColorInt public static final int ON_SURFACE = 0xFFFFFFFF;
}
