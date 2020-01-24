/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.semantics

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class SemanticsPropertyKey<T>(
    /**
     * The name of the property.  Should be the same as the constant from which it is accessed.
     */
    val name: String
) :
    ReadWriteProperty<SemanticsPropertyReceiver, T> {
    /**
     * Subclasses that wish to implement merging should override this to output the merged value
     *
     * This implementation always throws IllegalStateException.  It should be overridden for
     * properties that can be merged.
     */
    open fun merge(existingValue: T, newValue: T): T {
        throw IllegalStateException(
            "merge function called on unmergeable property $name. " +
                    "Existing value: $existingValue, new value: $newValue. " +
                    "You may need to add a semantic boundary."
        )
    }

    /**
     * Throws [UnsupportedOperationException].  Should not be called.
     */
    // noinspection DeprecatedCallableAddReplaceWith
    // TODO(KT-32770): Re-deprecate this
    // @Deprecated(
    //     message = "You cannot retrieve a semantics property directly - " +
    //             "use one of the SemanticsConfiguration.getOr* methods instead",
    //     level = DeprecationLevel.ERROR
    // )
    // TODO(KT-6519): Remove this getter entirely
    final override fun getValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>): T {
        throw UnsupportedOperationException(
            "You cannot retrieve a semantics property directly - " +
                    "use one of the SemanticsConfiguration.getOr* methods instead"
        )
    }

    final override fun setValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
        value: T
    ) {
        thisRef[this] = value
    }

    override fun toString(): String {
        return "SemanticsPropertyKey: $name"
    }
}

// This needs to be in core because it needs to be accessible from platform
data class AccessibilityAction<T : Function<Unit>>(val label: String?, val action: T) {
    // TODO(b/145951226): Workaround for a bytecode issue, remove this
    override fun hashCode(): Int {
        var result = label?.hashCode() ?: 0
        // (action as Any) is the workaround
        result = 31 * result + (action as Any).hashCode()
        return result
    }
}

data class AccessibilityRangeInfo(
    val current: Float,
    val range: ClosedFloatingPointRange<Float>
)

interface SemanticsPropertyReceiver {
    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}
