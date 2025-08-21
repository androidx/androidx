/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.frontend.state

import androidx.compose.remote.core.Platform
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.player.view.state.RemoteDomains
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember

/**
 * Abstract base class for all remote long representations. This class extends [RemoteState<Long>].
 *
 * @property hasConstantValue A boolean indicating whether this [RemoteLong] will always evaluate to
 *   the same [value]. This is a conservative check; some expressions that are effectively constant
 *   might still return `false` due to the cost of tracking their dependencies.
 */
abstract class RemoteLong internal constructor(override val hasConstantValue: Boolean) :
    RemoteState<Long> {

    abstract val id: Int

    companion object {
        /**
         * Creates a [RemoteLong] instance from a constant [Long] value. This value will be added as
         * a constant to the remote document.
         *
         * @param v The constant [Long] value.
         * @return A [MutableRemoteLong] representing the constant value.
         */
        operator fun invoke(v: Long): RemoteLong {
            return MutableRemoteLong(mutableLongStateOf(v), true) { creationState ->
                creationState.document.addLong(v)
            }
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
        fun createNamedRemoteLong(name: String, initialValue: Long): RemoteLong {
            return MutableRemoteLong(mutableLongStateOf(initialValue), false) { creationState ->
                creationState.document.addNamedLong(name, initialValue)
            }
        }
    }
}

/**
 * A mutable implementation of [RemoteLong] that holds its value in a [MutableLongState].
 *
 * @property content The underlying [MutableLongState] that stores the actual long value.
 * @property hasConstantValue A boolean indicating whether this [MutableRemoteLong] is expected to
 *   remain constant. For mutable states, this is typically `false`.
 * @property idProvider A lambda that provides the unique ID for this mutable long within the
 *   [RemoteComposeCreationState]. This ID is used to identify the long in the remote document.
 */
class MutableRemoteLong(
    private val content: MutableLongState,
    hasConstantValue: Boolean,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteLong(hasConstantValue), MutableRemoteState<Long> {

    /**
     * Constructor for [MutableRemoteLong] that allows specifying an optional initial ID. If no ID
     * is provided, a new float variable ID is reserved.
     *
     * @param content The [MutableLongState] to hold the value.
     * @param id An optional explicit ID for this mutable long. If `null`, a new ID is reserved.
     */
    constructor(
        content: MutableLongState,
        id: Int? = null,
    ) : this(
        content,
        false,
        { creationState -> id ?: Utils.idFromNan(creationState.document.reserveFloatVariable()) },
    )

    override fun writeToDocument(creationState: RemoteComposeCreationState) =
        idProvider(creationState)

    @Deprecated("Use getIdForCreationState directly")
    override val id: Int
        get() {
            FallbackCreationState.state.platform.log(
                Platform.LogCategory.TODO,
                "Use RemoteLong.getIdForCreationState directly",
            )
            return getIdForCreationState(FallbackCreationState.state)
        }

    override var value: Long
        get() {
            return content.longValue
        }
        set(newValue) {
            content.longValue = newValue
        }

    override operator fun component1(): Long = value

    override operator fun component2(): (Long) -> Unit = { newValue ->
        content.longValue = newValue
    }

    override fun toString(): String {
        return "MutableRemoteLong@${this.hashCode()} =" + content.longValue
    }
}

/**
 * A Composable function to remember and provide a mutable remote long value.
 *
 * @param name The unique name for this remote long, used for identification in the remote document.
 * @param domain The domain of the remote long (defaults to [RemoteDomains.USER]). This helps
 *   organize named values.
 * @param value A lambda that provides the initial [Long] value for this remote long.
 * @return A [MutableRemoteLong] instance that will be remembered across recompositions.
 */
@Composable
fun rememberRemoteLongValue(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    value: () -> Long,
): MutableRemoteLong {
    return remember(name) {
        val initial = value()
        MutableRemoteLong(mutableLongStateOf(initial), false) { creationState ->
            val id = creationState.document.addNamedLong(name, initial)
            creationState.document.setStringName(id.toInt(), "$domain:$name")
            id
        }
    }
}
