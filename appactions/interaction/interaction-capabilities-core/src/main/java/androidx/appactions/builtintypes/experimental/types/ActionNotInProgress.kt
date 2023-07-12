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

package androidx.appactions.builtintypes.experimental.types

import androidx.appactions.builtintypes.experimental.properties.Name

interface ActionNotInProgress : Thing {
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = ActionNotInProgressBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        override fun build(): ActionNotInProgress
    }
}

private class ActionNotInProgressBuilderImpl :
    ActionNotInProgress.Builder<ActionNotInProgressBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null

    override fun build() = ActionNotInProgressImpl(identifier, name)

    override fun setIdentifier(text: String?): ActionNotInProgressBuilderImpl =
        apply { identifier = text }

    override fun setName(text: String): ActionNotInProgressBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): ActionNotInProgressBuilderImpl = apply { this.name = name }

    override fun clearName(): ActionNotInProgressBuilderImpl = apply { name = null }
}

private class ActionNotInProgressImpl(override val identifier: String?, override val name: Name?) :
    ActionNotInProgress {
    override fun toBuilder(): ActionNotInProgress.Builder<*> =
        ActionNotInProgressBuilderImpl().setIdentifier(identifier).setName(name)

    override fun toString(): String = "ActionNotInProgress"
}