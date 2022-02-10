/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.metadata

/**
 * The physical device (such as phone, watch, scale, or chest strap) that captured associated health
 * record.
 *
 * [Device] needs to be populated by users of the API. Fields not provided by clients will remain
 * absent. Two devices with corresponding unknown fields will compare as equal, but may represent
 * different devices.
 */
public class Device(
    public val identifier: String? = null,
    public val manufacturer: String? = null,
    public val model: String? = null,
    public val type: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Device) return false

        if (identifier != other.identifier) return false
        if (manufacturer != other.manufacturer) return false
        if (model != other.model) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identifier?.hashCode() ?: 0
        result = 31 * result + (manufacturer?.hashCode() ?: 0)
        result = 31 * result + (model?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }
}
