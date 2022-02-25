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

package androidx.health.platform.client.impl.data

import androidx.annotation.RestrictTo
import com.google.protobuf.MessageLite

/** Base class for data objects backed by protos. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class ProtoData<T : MessageLite> {
    /** Proto representation of this object. */
    abstract val proto: T

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        // It's possible that two objects from different classes have the same serialized form, however
        // they shouldn't be considered equal unless they're of the same class.
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        val that = other as ProtoData<*>
        return proto == that.proto
    }

    override fun hashCode(): Int = proto.hashCode()
}