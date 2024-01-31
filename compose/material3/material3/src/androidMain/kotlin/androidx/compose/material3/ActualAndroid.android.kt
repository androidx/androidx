/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.util.Locale

/**
 * Returns the default [CalendarLocale].
 */
@Composable
@ReadOnlyComposable
internal actual fun defaultLocale(): CalendarLocale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Locale24.defaultLocale()
    } else {
        ConfigurationCompat.getLocales(LocalConfiguration.current).get(0) ?: Locale.getDefault()
    }
}

@RequiresApi(24)
private class Locale24 {
    companion object {
        @Composable
        @ReadOnlyComposable
        fun defaultLocale(): CalendarLocale {
            return LocalConfiguration.current.locales[0]
        }
    }
}
