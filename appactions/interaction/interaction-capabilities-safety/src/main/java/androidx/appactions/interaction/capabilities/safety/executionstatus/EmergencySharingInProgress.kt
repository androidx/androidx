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

package androidx.appactions.interaction.capabilities.safety.executionstatus

import androidx.appactions.builtintypes.experimental.properties.Name
import androidx.appactions.builtintypes.experimental.types.Thing

interface EmergencySharingInProgress : Thing {
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = EmergencySharingInProgressBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        override fun build(): EmergencySharingInProgress
    }
}

private class EmergencySharingInProgressBuilderImpl :
    EmergencySharingInProgress.Builder<EmergencySharingInProgressBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null

    override fun build() = EmergencySharingInProgressImpl(identifier, name)

    override fun setIdentifier(text: String?): EmergencySharingInProgressBuilderImpl =
        apply { identifier = text }

    override fun setName(text: String): EmergencySharingInProgressBuilderImpl =
        apply { name = Name(text) }

    override fun setName(name: Name?): EmergencySharingInProgressBuilderImpl =
        apply { this.name = name }

    override fun clearName(): EmergencySharingInProgressBuilderImpl = apply { name = null }
}

private class EmergencySharingInProgressImpl(
    override val identifier: String?,
    override val name: Name?
) :
    EmergencySharingInProgress {
    override fun toBuilder(): EmergencySharingInProgress.Builder<*> =
        EmergencySharingInProgressBuilderImpl().setIdentifier(identifier).setName(name)

    override fun toString(): String = "EmergencySharingInProgress"
}