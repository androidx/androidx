/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.common;

import android.os.Build;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.RestrictTo;

import java.util.Locale;

/** Shared utility class for ProtoLayout renderer. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Utils {
    /** Returns whether the current OS version is higher or equal to B. */
    @ChecksSdkIntAtLeast(api = 36, codename = "Baklava")
    public static boolean isAtLeastBaklava() {
        // API Baklava is 36 when released, but before releasing and API finalization, it's 35 with
        // a
        // name Baklava. We can't just check first character of codename, because alphabet is
        // repeating
        // from the start again.
        return Build.VERSION.SDK_INT >= 36
                || (
                /* isAtLeastV */ Build.VERSION.SDK_INT == 35
                        && "Baklava"
                                .toUpperCase(Locale.US)
                                .equals(Build.VERSION.CODENAME.toUpperCase(Locale.US)));
    }

    private Utils() {}
}
