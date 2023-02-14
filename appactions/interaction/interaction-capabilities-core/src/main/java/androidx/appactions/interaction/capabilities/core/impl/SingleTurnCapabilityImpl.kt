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

import androidx.annotation.NonNull
import androidx.appactions.interaction.capabilities.core.ActionCapability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.SessionBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.TaskInfo

import androidx.annotation.RestrictTo

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SingleTurnCapabilityImpl<
    PropertyT,
    ArgumentT,
    OutputT,
    > constructor(
    override val id: String?,
    val actionSpec: ActionSpec<PropertyT, ArgumentT, OutputT>,
    val property: PropertyT,
    val sessionBuilder: SessionBuilder<BaseSession<ArgumentT, OutputT>>,
) : ActionCapability {
    override val supportsMultiTurnTask = false

    @NonNull
    override fun getAppAction(): AppAction {
        val appActionBuilder = actionSpec.convertPropertyToProto(property).toBuilder()
            .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(false))
        id?.let(appActionBuilder::setIdentifier)
        return appActionBuilder.build()
    }

    @NonNull
    override fun createSession(hostProperties: HostProperties): ActionCapabilitySession {
        return SingleTurnCapabilitySession(
            actionSpec,
            sessionBuilder.createSession(hostProperties),
        )
    }
}
