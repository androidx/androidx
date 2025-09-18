/*
 * Copyright 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.ui.util.fastMap

/**
 * Represents an array of floats.
 *
 * @property input The collection of floats to store in the document
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteFloatArray(input: List<RemoteFloat>) : RemoteState<List<RemoteFloat>> {
    private val floatArray = input

    override val hasConstantValue: Boolean
        get() = true

    override val value: List<RemoteFloat>
        get() = floatArray

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val asFloat = floatArray.fastMap { it.toFloat() }.toFloatArray()
        return Utils.idFromNan(creationState.document.addFloatArray(asFloat))
    }

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteFloat] index. Performs a
     * dereference operation on a remote float array.
     */
    public operator fun get(v: RemoteFloat): RemoteFloat {
        return RemoteFloatExpression(hasConstantValue) { creationState ->
            floatArrayOf(
                *arrayForCreationState(creationState),
                *v.arrayForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    /**
     * Array access operator for [RemoteFloatArray] with an [Int] index. Performs a dereference
     * operation on a remote float array.
     */
    public operator fun get(v: Int): RemoteFloat {
        return RemoteFloatExpression(hasConstantValue) { creationState ->
            floatArrayOf(
                *arrayForCreationState(creationState),
                v.toFloat(),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteInt] index. Performs a dereference
     * operation on a remote float array.
     */
    public operator fun get(v: RemoteInt): RemoteFloat {
        return RemoteFloatExpression(hasConstantValue && v.hasConstantValue) { creationState ->
            floatArrayOf(
                *arrayForCreationState(creationState),
                v.getFloatIdForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    private fun arrayForCreationState(creationState: RemoteComposeCreationState): FloatArray {
        val cachedArray = creationState.floatArrayCache.get(this)
        if (cachedArray != null) {
            return cachedArray
        }
        val array = floatArrayOf(getFloatIdForCreationState(creationState))
        creationState.floatArrayCache.put(this, array)
        return array
    }
}
