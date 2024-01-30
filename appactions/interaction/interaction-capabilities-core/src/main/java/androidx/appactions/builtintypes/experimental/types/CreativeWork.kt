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
import androidx.appactions.builtintypes.experimental.properties.Text

interface CreativeWork : Thing {
    val text: Text?

    override fun toBuilder(): Thing.Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Thing.Builder<*> = CreativeWorkBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        fun setText(text: Text?): Self
        fun setText(text: String): Self

        override fun build(): CreativeWork
    }
}

private class CreativeWorkBuilderImpl : CreativeWork.Builder<CreativeWorkBuilderImpl> {
    private var identifier: String? = null
    private var name: Name? = null
    private var text: Text? = null

    override fun build() = CreateWorkImpl(text, identifier, name)

    override fun setText(text: Text?): CreativeWorkBuilderImpl = apply { this.text = text }

    override fun setText(text: String): CreativeWorkBuilderImpl = apply { this.text = Text(text) }

    override fun setIdentifier(text: String?): CreativeWorkBuilderImpl = apply {
        identifier = text
    }

    override fun setName(text: String): CreativeWorkBuilderImpl = apply { name = Name(text) }
    override fun setName(name: Name?): CreativeWorkBuilderImpl = apply {
        this.name = name
    }

    override fun clearName(): CreativeWorkBuilderImpl = apply { name = null }
}

private class CreateWorkImpl(
    override val text: Text?,
    override val identifier: String?,
    override val name: Name?
) : CreativeWork {
    override fun toBuilder(): CreativeWork.Builder<*> =
        CreativeWorkBuilderImpl()
            .setIdentifier(identifier)
            .setName(name)
            .setText(text)
}
