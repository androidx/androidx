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

package androidx.compose.material3

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun mediaQueryInfo(): MediaQueryInfo {
    val packageManager = LocalContext.current.packageManager
    return MediaQueryInfo(
        isLaptop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && packageManager.isLaptop(),
        isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK),
        isAuto = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE),
    )
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
private fun PackageManager.isLaptop() = hasSystemFeature(PackageManager.FEATURE_PC)
