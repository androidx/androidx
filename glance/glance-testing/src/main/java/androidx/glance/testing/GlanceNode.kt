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

package androidx.glance.testing

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

/**
 * A wrapper for Glance composable node under test.
 *
 * @param T A representation of Glance composable node (e.g. MappedNode) on which assertions can be
 *          performed
 * @param value an object of the representation of the Glance composable node
 */
abstract class GlanceNode<T> @RestrictTo(Scope.LIBRARY_GROUP) constructor(val value: T) {
    /**
     * Returns children of current glance node.
     */
    abstract fun children(): List<GlanceNode<T>>

    /**
     * Returns the Glance node as string that can be presented in error messages helping developer
     * debug the assertion error.
     */
    abstract fun toDebugString(): String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlanceNode<*>) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        val result = value.hashCode()
        return 31 * result
    }

    override fun toString(): String {
        return ("GlanceNode{value='$value}'")
    }
}
