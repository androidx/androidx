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

package androidx.appactions.interaction.capabilities.core.impl.converters

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.protobuf.Value

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UnionTypeSpec<T : Any> internal constructor(
    private val bindings: List<MemberBinding<T, *>>,
) : TypeSpec<T> {
    internal class MemberBinding<T, M>(
        private val memberGetter: (T) -> M?,
        private val ctor: (M) -> T,
        private val typeSpec: TypeSpec<M>,
    ) {
        @Throws(StructConversionException::class)
        fun tryDeserialize(value: Value): T {
            return ctor(typeSpec.fromValue(value))
        }

        fun serialize(obj: T): Value {
            return typeSpec.toValue(memberGetter(obj)!!)
        }

        fun getIdentifier(obj: T): String? {
            return typeSpec.getIdentifier(memberGetter(obj)!!)
        }

        fun isMemberSet(obj: T): Boolean {
            return memberGetter(obj) != null
        }
    }

    private fun getApplicableBinding(obj: T): MemberBinding<T, *> {
        val applicableBindings = bindings.filter { it.isMemberSet(obj) }
        return when (applicableBindings.size) {
            0 -> throw IllegalStateException("$obj is invalid, all union members are null.")
            1 -> applicableBindings[0]
            else -> throw IllegalStateException(
                "$obj is invalid, multiple union members are non-null."
            )
        }
    }

    override fun getIdentifier(obj: T): String? {
        return getApplicableBinding(obj).getIdentifier(obj)
    }

    override fun toValue(obj: T): Value {
        return getApplicableBinding(obj).serialize(obj)
    }

    @Throws(StructConversionException::class)
    override fun fromValue(value: Value): T {
        for (binding in bindings) {
            try {
                return binding.tryDeserialize(value)
            } catch (e: StructConversionException) {
                continue
            }
        }
        throw StructConversionException("all member TypeSpecs failed to deserialize input Value.")
    }

    class Builder<T : Any> {
        private val bindings = mutableListOf<MemberBinding<T, *>>()

        /** During deserialization, bindings will be tried in the order they are bound. */
        fun <M> bindMemberType(
            memberGetter: (T) -> M?,
            ctor: (M) -> T,
            typeSpec: TypeSpec<M>,
        ) = apply {
            bindings.add(
                MemberBinding(
                    memberGetter,
                    ctor,
                    typeSpec,
                ),
            )
        }

        fun build() = UnionTypeSpec(bindings.toList())
    }
}
