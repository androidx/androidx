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
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.TaskInfo
import kotlinx.coroutines.sync.Mutex

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SingleTurnCapabilityImpl<
    PropertyT,
    ArgumentsT,
    OutputT,
    > constructor(
    id: String,
    val actionSpec: ActionSpec<PropertyT, ArgumentsT, OutputT>,
    val property: PropertyT,
    val executionCallback: ExecutionCallback<ArgumentsT, OutputT>,
) : Capability(id) {
    private val mutex = Mutex()

    override val appAction: AppAction get() =
        actionSpec.convertPropertyToProto(property).toBuilder()
            .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(false))
            .setIdentifier(id)
            .build()

    override fun createSession(
        sessionId: String,
        hostProperties: HostProperties,
    ): CapabilitySession {
        return SingleTurnCapabilitySession(
            sessionId,
            actionSpec,
            executionCallback,
            mutex,
        )
    }
}
