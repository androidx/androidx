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

interface Person : Thing {
    val email: String?
    val telephone: String?

    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = PersonBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Builder<Self>> {
        fun setEmail(email: String?): Self
        fun setTelephone(telephone: String?): Self

        override fun build(): Person
    }
}

private class PersonBuilderImpl : Person.Builder<PersonBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null
    private var email: String? = null
    private var telephone: String? = null

    override fun setEmail(email: String?): PersonBuilderImpl = apply { this.email = email }

    override fun setTelephone(telephone: String?): PersonBuilderImpl =
        apply { this.telephone = telephone }

    override fun build() = PersonImpl(identifier, name, email, telephone)

    override fun setIdentifier(text: String?): PersonBuilderImpl = apply { identifier = text }

    override fun setName(text: String): PersonBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): PersonBuilderImpl = apply { this.name = name }

    override fun clearName(): PersonBuilderImpl = apply { name = null }
}

private class PersonImpl(
    override val identifier: String?,
    override val name: Name?,
    override val email: String?,
    override val telephone: String?
) : Person {
    override fun toBuilder(): Person.Builder<*> =
        PersonBuilderImpl()
            .setIdentifier(identifier)
            .setName(name)
            .setEmail(email)
            .setTelephone(telephone)
}