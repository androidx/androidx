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

package androidx.xr.compose.platform

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Logger for Subspace Compose. This is a simple wrapper around Log.v that is only enabled if the
 * androidx.xr.compose.platform.Logger metadata flag is set in the AndroidManifest.xml.
 */
public object Logger {
    private const val METADATA_KEY = "androidx.xr.compose.platform.Logger"
    private var isDebug: Boolean = false

    /**
     * Initializes the logger. This method expects
     * android:name="androidx.xr.compose.platform.Logger" to be set in a metadata tag in the
     * AndroidManifest.xml. Typically the value of this key is set to false but can be set to true
     * to enable debug logging. For example:
     *
     * <meta-data android:name="androidx.xr.compose.platform.Logger" android:value="true" />
     *
     * @param context The application context for the current application.
     */
    public fun init(context: Context) {
        isDebug =
            context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData
                .getBoolean(METADATA_KEY, false)
    }

    /**
     * Logs a message to the logcat if debugging was enabled in the init method.
     *
     * @param tag The tag to use for the log message.
     * @param messageGenerator This generates the message to log.
     */
    public fun log(tag: String, messageGenerator: () -> String) {
        if (isDebug) {
            Log.v(tag, messageGenerator())
        }
    }
}
