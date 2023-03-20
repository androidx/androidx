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
import androidx.appactions.interaction.protobuf.Struct

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UnionTypeSpec<T : Any> internal constructor(
    private val bindings: List<MemberBinding<T, *>>,
) : TypeSpec<T> {
    internal class MemberBinding<T, M>(
        val memberGetter: (T) -> M?,
        val ctor: (M) -> T,
        val typeSpec: TypeSpec<M>,
    ) {
        fun tryDeserialize(struct: Struct): T {
            return ctor(typeSpec.fromStruct(struct))
        }

        fun trySerialize(obj: T): Struct? {
            return memberGetter(obj)?.let { typeSpec.toStruct(it) }
        }
    }

    override fun fromStruct(struct: Struct): T {
        for (binding in bindings) {
            try {
                return binding.tryDeserialize(struct)
            } catch (e: StructConversionException) {
                continue
            }
        }
        throw StructConversionException("failed to deserialize union type")
    }

    override fun toStruct(obj: T): Struct {
        for (binding in bindings) {
            binding.trySerialize(obj)?.let {
                return it
            }
        }
        throw StructConversionException("failed to serialize union type")
    }

    class Builder<T : Any> {
        private val bindings = mutableListOf<MemberBinding<T, *>>()

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

        fun build() = UnionTypeSpec<T>(bindings.toList())
    }
}
