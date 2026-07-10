/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp.vo

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.RoomWarnings

@Entity
open class School(
    @PrimaryKey var id: Int = 0,
    var name: String? = null,
    @Embedded(prefix = "address_") var address: Address? = null,
    @Suppress(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
    @Embedded(prefix = "manager_")
    var manager: User? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is School) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (manager != other.manager) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (manager?.hashCode() ?: 0)
        return result
    }
}
