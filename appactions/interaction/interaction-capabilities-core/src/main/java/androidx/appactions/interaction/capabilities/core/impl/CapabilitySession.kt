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

package androidx.appactions.interaction.capabilities.core.impl

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.proto.AppActionsContext.AppDialogState

/**
 * Internal interface for a session, contains developer's Session instance
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CapabilitySession {
    /** The id of this ActionCapabilitySession instance. */
    val sessionId: String

    /**
     * Executes the action and returns the result of execution.
     *
     * @param argumentsWrapper The arguments send from assistant to the activity.
     * @param callback The callback to receive app action result.
     */
    fun execute(
        argumentsWrapper: ArgumentsWrapper,
        callback: CallbackInternal
    )

    /**
     * Notify this session to stop processing and perform cleanup.
     */
    fun destroy()

    /**
     * Support for manual input. This method should be invoked by AppInteraction SDKs
     * (background/foreground), so the developers have a way to report state updates back to
     * Assistant.
     */
    fun setTouchEventCallback(callback: TouchEventCallback)

    /**
     * The current state of the multi-turn session including slot values and their statuses.
     */
    val state: AppDialogState?

    /** The current status of the CapabilitySession. */
    val isActive: Boolean

    /**
     * The developer-provided external object (either a BaseExecutionSession instance or an
     * CapabilityExecutor instance).
     */
    val uiHandle: Any
}
