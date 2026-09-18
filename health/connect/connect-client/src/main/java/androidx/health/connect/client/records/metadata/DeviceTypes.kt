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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.records.metadata

import androidx.annotation.RestrictTo

/** List of supported device types on Health Platform. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object DeviceTypes {
    public const val UNKNOWN: String = "UNKNOWN"
    public const val WATCH: String = "WATCH"
    public const val PHONE: String = "PHONE"
    public const val SCALE: String = "SCALE"
    public const val RING: String = "RING"
    public const val HEAD_MOUNTED: String = "HEAD_MOUNTED"
    public const val FITNESS_BAND: String = "FITNESS_BAND"
    public const val CHEST_STRAP: String = "CHEST_STRAP"
    public const val SMART_DISPLAY: String = "SMART_DISPLAY"
}
