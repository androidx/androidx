/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.wear.protolayout.expression

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType
import java.util.Objects

/**
 * Represent a key that references a dynamic value source, such as state pushed by app/tile or
 * real-time data from the platform.
 *
 * @param T The data type of the dynamic values that this key is bound to.
 */
public abstract class DynamicDataKey<T : DynamicType>
/**
 * Create a [DynamicDataKey] with the specified key in the given namespace.
 *
 * @property namespace The namespace of the key for the dynamic data source.
 * @property key The key that references the dynamic data source.
 */
internal constructor(public val namespace: String, public val key: String) {

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is DynamicDataKey<*>) {
            return false
        }

        return key == other.key && namespace == other.namespace
    }

    override fun hashCode(): Int = Objects.hash(key, namespace)

    override fun toString(): String = "DynamicDataKey{namespace=$namespace, key=$key}"
}
