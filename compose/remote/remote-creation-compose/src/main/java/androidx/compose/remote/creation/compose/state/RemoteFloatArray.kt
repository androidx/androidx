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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.ui.util.fastMap

/**
 * Represents an array of floats.
 *
 * @property input The collection of floats to store in the document
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteFloatArray(public override val constantValue: List<RemoteFloat>?) :
    RemoteState<List<RemoteFloat>> {

    override val value: List<RemoteFloat>
        get() = constantValue!!

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val asFloat = constantValue!!.fastMap { it.toFloat() }.toFloatArray()
        return Utils.idFromNan(creationState.document.addFloatArray(asFloat))
    }

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteFloat] index. Performs a
     * dereference operation on a remote float array.
     */
    public operator fun get(v: RemoteFloat): RemoteFloat {
        v.constantValue?.let {
            return constantValue!![it.toInt()]
        }
        return RemoteFloatExpression(constantValue = null) { creationState ->
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
    public operator fun get(v: Int): RemoteFloat = constantValue!![v]

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteInt] index. Performs a dereference
     * operation on a remote float array.
     */
    public operator fun get(v: RemoteInt): RemoteFloat {
        v.constantValue?.let {
            return constantValue!![it]
        }
        return RemoteFloatExpression(constantValue = null) { creationState ->
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
