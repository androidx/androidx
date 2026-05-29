/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.subspace.semantics

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Scope provided by [semantics] blocks, letting you set key/value pairs for use in testing,
 * accessibility, etc.
 */
public interface SubspaceSemanticsPropertyReceiver {
    /**
     * Sets the semantics property defined by [key] to [value].
     *
     * @param key The property key to set.
     * @param value The value to assign to the property.
     */
    public operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}

/** Test tag attached to this semantics node. */
public var SubspaceSemanticsPropertyReceiver.testTag: String
    get() = throw UnsupportedOperationException("You cannot read semantics properties")
    set(value) {
        this[SemanticsProperties.TestTag] = value
    }

/** Developer-set content description of the accessibility node. */
public var SubspaceSemanticsPropertyReceiver.contentDescription: String
    get() = throw UnsupportedOperationException("You cannot read semantics properties")
    set(value) {
        this[SemanticsProperties.ContentDescription] = listOf(value)
    }

internal class SubspaceSemanticsPropertyReceiverImpl(
    private val delegate: SemanticsPropertyReceiver
) : SubspaceSemanticsPropertyReceiver {
    override operator fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
        delegate[key] = value
    }
}
