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
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.runtime.Composable

/**
 * Abstract base class for all remote long representations. This class extends [RemoteState<Long>].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RemoteLong : BaseRemoteState<Long>() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [RemoteLong] instance from a constant [Long] value. This value will be added as
         * a constant to the remote document.
         *
         * @param v The constant [Long] value.
         * @return A [MutableRemoteLong] representing the constant value.
         */
        public operator fun invoke(v: Long): RemoteLong {
            return MutableRemoteLong(v) { creationState -> creationState.document.addLong(v) }
        }

        /**
         * Creates a named [RemoteLong] with an initial value. Named remote longs can be set via
         * AndroidRemoteContext.setNamedLong.
         *
         * @param name The unique name for this remote long.
         * @param initialValue The initial [Long] value for the named remote long.
         * @return A [RemoteLong] representing the named long.
         */
        @JvmStatic
        public fun createNamedRemoteLong(name: String, initialValue: Long): RemoteLong {
            return MutableRemoteLong(constantValueOrNull = null) { creationState ->
                creationState.document.addNamedLong(name, initialValue)
            }
        }
    }
}

/**
 * A mutable implementation of [RemoteLong].
 *
 * @param constantValue A boolean indicating whether this [MutableRemoteLong] is expected to remain
 *   constant. For mutable states, this is typically `false`.
 * @param idProvider A lambda that provides the unique ID for this mutable long within the
 *   [RemoteComposeCreationState]. This ID is used to identify the long in the remote document.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MutableRemoteLong(
    public override val constantValueOrNull: Long?,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteLong(), MutableRemoteState<Long> {

    /**
     * Constructor for [MutableRemoteLong] that allows specifying an optional initial ID. If no ID
     * is provided, a new float variable ID is reserved.
     *
     * @param id An optional explicit ID for this mutable long. If `null`, a new ID is reserved.
     */
    public constructor(id: Int) : this(constantValueOrNull = null, { creationState -> id })

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)

    public override fun toString(): String {
        return "MutableRemoteLong@${this.hashCode()} =" + constantValueOrNull
    }
}

/**
 * A Composable function to remember and provide a mutable remote long value.
 *
 * @param name The unique name for this remote long, used for identification in the remote document.
 * @param domain The domain of the remote long (defaults to [RemoteState.Domain.User]). This helps
 *   organize named values.
 * @param value A lambda that provides the initial [Long] value for this remote long.
 * @return A [MutableRemoteLong] instance that will be remembered across recompositions.
 */
@Composable
public fun rememberRemoteLongValue(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> Long,
): MutableRemoteLong {
    return rememberNamedState(name, domain) {
        val initial = value()
        MutableRemoteLong(constantValueOrNull = null) { creationState ->
            val id = creationState.document.addNamedLong(name, initial)
            creationState.document.setStringName(id, "$domain:$name")
            id
        }
    }
}
