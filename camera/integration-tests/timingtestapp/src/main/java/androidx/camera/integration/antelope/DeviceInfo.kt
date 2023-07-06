/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

/**
 * Convenience class to access device information
 */
class DeviceInfo() {
    /** Detailed string with device and OS information */
    val device: String = android.os.Build.MANUFACTURER + " " +
        android.os.Build.BRAND + " " +
        android.os.Build.DEVICE + " " +
        android.os.Build.MODEL + " " +
        android.os.Build.PRODUCT + " " +
        "(" + android.os.Build.VERSION.RELEASE + android.os.Build.VERSION.INCREMENTAL + ") " +
        "\nSDK: " + android.os.Build.VERSION.SDK_INT

    /** Short string with device information */
    val deviceShort: String = android.os.Build.DEVICE
}
