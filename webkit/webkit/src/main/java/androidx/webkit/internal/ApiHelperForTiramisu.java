/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.webkit.internal;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

/**
 * Utility class to use new APIs that were added in Tiramisu (API level 33).
 * These need to exist in a separate class so that Android framework can successfully verify
 * classes without encountering the new APIs.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class ApiHelperForTiramisu {
    private ApiHelperForTiramisu() {
    }

    static PackageManager.ComponentInfoFlags of(long value) {
        return PackageManager.ComponentInfoFlags.of(value);
    }

    static ServiceInfo getServiceInfo(PackageManager packageManager, ComponentName component,
            PackageManager.ComponentInfoFlags flags)
            throws PackageManager.NameNotFoundException {
        return packageManager.getServiceInfo(component, flags);
    }
}
