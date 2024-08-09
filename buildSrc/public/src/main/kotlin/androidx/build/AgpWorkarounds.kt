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

package androidx.build

import com.android.build.api.variant.DeviceTest
import com.android.build.api.variant.HasDeviceTests

private const val deviceTestsMethod = "getDeviceTests"

/**
 * Temporary workaround to allow calling `deviceTests.forEach` while this API is being migrated from
 * `List<DeviceTest>` to `Map<String, DeviceTest>`.
 */
@Suppress("PrivateApi", "UnstableApiUsage")
fun HasDeviceTests.deviceTestsForEachCompat(forEachAction: (DeviceTest) -> Unit) {
    val method = javaClass.getDeclaredMethod(deviceTestsMethod)
    val returnValue = method.invoke(this)

    val iterable: Iterable<*> =
        if (returnValue is List<*>) {
            returnValue
        } else {
            if (returnValue is Map<*, *>) {
                returnValue.values.toList()
            } else {
                throw RuntimeException(
                    "Field $deviceTestsMethod returns neither a List<> nor a Map<>"
                )
            }
        }
    iterable.forEach { element -> element?.let { forEachAction(it as DeviceTest) } }
}
