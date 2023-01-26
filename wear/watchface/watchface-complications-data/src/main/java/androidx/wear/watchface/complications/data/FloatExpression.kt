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

package androidx.wear.watchface.complications.data

import androidx.annotation.RestrictTo

/**
 * Placeholder for FloatExpression implementation by tiles.
 * @hide
 */
// TODO(b/260065006): Replace this with the real implementation.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class FloatExpression {
    abstract fun asByteArray(): ByteArray

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // Not checking for exact same class because it's not implemented yet.
        if (other !is FloatExpression) return false
        return asByteArray().contentEquals(other.asByteArray())
    }

    override fun hashCode() = asByteArray().contentHashCode()

    override fun toString() = "FloatExpressionPlaceholder${asByteArray().contentToString()}"
}

/**
 * Placeholder parser for [FloatExpression] from [ByteArray].
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ByteArray.toFloatExpression() = object : FloatExpression() {
    override fun asByteArray() = this@toFloatExpression
}