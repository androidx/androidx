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
package androidx.health.connect.client.records.metadata

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * A physical device (such as phone, watch, scale, or chest strap) which captured associated health
 * data point.
 *
 * Device needs to be populated by users of the API. Metadata fields not provided by clients will
 * remain absent.
 *
 * @property manufacturer an optional client supplied manufacturer of the device
 * @property model an optional client supplied model of the device
 * @property type an optional client supplied type of the device
 */
public class Device(
    public val manufacturer: String? = null,
    public val model: String? = null,
    @property:DeviceType public val type: Int = TYPE_UNKNOWN
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Device

        if (manufacturer != other.manufacturer) return false
        if (model != other.model) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manufacturer?.hashCode() ?: 0
        result = 31 * result + (model?.hashCode() ?: 0)
        result = 31 * result + type
        return result
    }

    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_WATCH = 1
        const val TYPE_PHONE = 2
        const val TYPE_SCALE = 3
        const val TYPE_RING = 4
        const val TYPE_HEAD_MOUNTED = 5
        const val TYPE_FITNESS_BAND = 6
        const val TYPE_CHEST_STRAP = 7
        const val TYPE_SMART_DISPLAY = 8
    }

    /**
     * List of supported device types on Health Platform.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                TYPE_UNKNOWN,
                TYPE_WATCH,
                TYPE_PHONE,
                TYPE_SCALE,
                TYPE_RING,
                TYPE_HEAD_MOUNTED,
                TYPE_FITNESS_BAND,
                TYPE_CHEST_STRAP,
                TYPE_SMART_DISPLAY,
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class DeviceType
}
