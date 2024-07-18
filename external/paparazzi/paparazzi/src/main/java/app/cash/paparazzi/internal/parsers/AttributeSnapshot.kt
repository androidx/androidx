/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi.internal.parsers

/**
 * Derived from https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-master-dev:android/src/com/android/tools/idea/rendering/parsers/AttributeSnapshot.java
 *
 * A snapshot of an attribute value pulled from an XML resource.
 * Used in conjunction with [TagSnapshot].
 */
open class AttributeSnapshot(
  open val namespace: String,
  open val prefix: String?,
  open val name: String,
  open val value: String
) {
  override fun toString() = "$name: $value"

  // since data classes can't be subclassed
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AttributeSnapshot

    if (namespace != other.namespace) return false
    if (prefix != other.prefix) return false
    if (name != other.name) return false
    if (value != other.value) return false

    return true
  }

  override fun hashCode(): Int {
    var result = namespace.hashCode()
    result = 31 * result + prefix.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + value.hashCode()
    return result
  }
}
