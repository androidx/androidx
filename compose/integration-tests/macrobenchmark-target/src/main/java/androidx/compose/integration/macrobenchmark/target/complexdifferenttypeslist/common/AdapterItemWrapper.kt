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

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.common

class AdapterItemWrapper(
    val type: BaseAdapterItemType,
    val data: Any? = null,
    var id: Any? = null,
    val isSticky: Boolean = false,
) {

    override fun hashCode(): Int {
        return if (id == null) {
            31 * type.hashCode() + (data?.hashCode() ?: 0)
        } else {
            31 * type.hashCode() + id.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdapterItemWrapper

        if (type != other.type) return false
        if (id != other.id) return false
        if (data != other.data) return false
        if (isSticky != other.isSticky) return false

        return true
    }

    /** Returns a copy of [AdapterItemWrapper]. */
    fun copy(
        type: BaseAdapterItemType = this.type,
        data: Any? = this.data,
        id: Any? = this.id,
        isSticky: Boolean = this.isSticky,
    ): AdapterItemWrapper =
        AdapterItemWrapper(type = type, data = data, id = id, isSticky = isSticky)
}
