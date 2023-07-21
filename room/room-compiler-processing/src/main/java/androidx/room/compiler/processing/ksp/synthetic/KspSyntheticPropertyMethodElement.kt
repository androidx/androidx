/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.javac.kotlin.JvmAbi
import androidx.room.compiler.processing.ksp.KSTypeVarianceResolverScope
import androidx.room.compiler.processing.ksp.KspAnnotated
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_GETTER
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_SETTER
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_SET_PARAM
import androidx.room.compiler.processing.ksp.KspFieldElement
import androidx.room.compiler.processing.ksp.KspHasModifiers
import androidx.room.compiler.processing.ksp.KspMemberContainer
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspType
import androidx.room.compiler.processing.ksp.findEnclosingMemberContainer
import androidx.room.compiler.processing.ksp.jvmDescriptor
import androidx.room.compiler.processing.ksp.overrides
import androidx.room.compiler.processing.util.sanitizeAsJavaParameterName
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter

/**
 * Kotlin properties don't have getters/setters in KSP. As Room expects Java code, we synthesize
 * them.
 *
 * @see KspSyntheticPropertyMethodElement.Getter
 * @see KspSyntheticPropertyMethodElement.Setter
 * @see KspSyntheticPropertyMethodType
 */
internal sealed class KspSyntheticPropertyMethodElement(
    val env: KspProcessingEnv,
    val field: KspFieldElement,
    open val accessor: KSPropertyAccessor
) : XMethodElement,
    XEquality,
    XHasModifiers by KspHasModifiers.createForSyntheticAccessor(
        field.declaration,
        accessor
    ) {

    @OptIn(KspExperimental::class)
    override val jvmName: String by lazy {
        env.resolver.getJvmName(accessor) ?: error("Cannot find the name for accessor $accessor")
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(field, accessor)
    }

    // NOTE: modifiers of the property are not necessarily my modifiers.
    //  that being said, it only matters if it is private in which case KAPT does not generate the
    //  synthetic hence we don't either.
    final override fun isJavaDefault() = false

    final override fun hasKotlinDefaultImpl() = false

    final override fun isSuspendFunction() = false

    final override fun isExtensionFunction() = false

    final override val enclosingElement: KspMemberContainer
        get() = this.field.enclosingElement

    final override val closestMemberContainer: XMemberContainer by lazy {
        enclosingElement.closestMemberContainer
    }

    final override fun isVarArgs() = false

    final override val executableType: XMethodType by lazy {
        KspSyntheticPropertyMethodType.create(
            env = env,
            element = this,
            container = field.enclosingElement.type
        )
    }

    override val docComment: String?
        get() = null

    override fun validate(): Boolean {
        return true
    }

    @OptIn(KspExperimental::class)
    override val thrownTypes: List<XType> by lazy {
        env.resolver.getJvmCheckedException(accessor).map {
            env.wrap(
                ksType = it,
                allowPrimitives = false
            )
        }.toList()
    }

    final override fun asMemberOf(other: XType): XMethodType {
        check(other is KspType)
        return KspSyntheticPropertyMethodType.create(
            env = env,
            element = this,
            container = other
        )
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return jvmName
    }

    final override fun overrides(other: XMethodElement, owner: XTypeElement): Boolean {
        return env.resolver.overrides(this, other)
    }

    override fun isKotlinPropertyMethod() = true

    internal class Getter(
        env: KspProcessingEnv,
        field: KspFieldElement,
        override val accessor: KSPropertyGetter
    ) : KspSyntheticPropertyMethodElement(
        env = env,
        field = field,
        accessor = accessor
    ),
        XAnnotated by KspAnnotated.create(
            env = env,
            delegate = accessor,
            filter = NO_USE_SITE_OR_GETTER
        ) {

        override val name: String by lazy {
            JvmAbi.computeGetterName(field.declaration.simpleName.asString())
        }

        override val jvmDescriptor: String
            get() = this.jvmDescriptor()

        override val returnType: XType by lazy {
            field.type.copyWithScope(
                KSTypeVarianceResolverScope.PropertyGetterMethodReturnType(
                    getterMethod = this,
                    asMemberOf = enclosingElement.type,
                )
            )
        }

        override val typeParameters: List<XTypeParameterElement>
            get() = emptyList()

        override val parameters: List<XExecutableParameterElement>
            get() = emptyList()

        override fun kindName(): String {
            return "synthetic property getter"
        }
    }

    internal class Setter(
        env: KspProcessingEnv,
        field: KspFieldElement,
        override val accessor: KSPropertySetter
    ) : KspSyntheticPropertyMethodElement(
        env = env,
        field = field,
        accessor = accessor
    ),
        XAnnotated by KspAnnotated.create(
            env = env,
            delegate = field.declaration.setter,
            filter = NO_USE_SITE_OR_SETTER
        ) {

        override val name by lazy {
            JvmAbi.computeSetterName(field.declaration.simpleName.asString())
        }

        override val jvmDescriptor: String
            get() = this.jvmDescriptor()

        override val returnType: XType by lazy {
            env.voidType
        }

        override val typeParameters: List<XTypeParameterElement>
            get() = emptyList()

        override val parameters: List<XExecutableParameterElement> by lazy {
            listOf(
                SyntheticExecutableParameterElement(
                    env = env,
                    enclosingElement = this
                )
            )
        }

        override fun kindName(): String {
            return "synthetic property getter"
        }

        private class SyntheticExecutableParameterElement(
            private val env: KspProcessingEnv,
            override val enclosingElement: Setter
        ) : XExecutableParameterElement,
            XAnnotated by KspAnnotated.create(
                env = env,
                delegate = enclosingElement.field.declaration.setter?.parameter,
                filter = NO_USE_SITE_OR_SET_PARAM
            ) {
            override fun isContinuationParam() = false

            override fun isReceiverParam() = false

            override fun isKotlinPropertyParam() = true

            override val name: String by lazy {
                val originalName = enclosingElement.accessor.parameter.name?.asString()
                originalName.sanitizeAsJavaParameterName(0)
            }

            override val type: KspType by lazy {
                enclosingElement.field.type.copyWithScope(
                    KSTypeVarianceResolverScope.PropertySetterParameterType(
                        setterMethod = enclosingElement,
                        asMemberOf = enclosingElement.enclosingElement.type,
                    )
                )
            }

            override val fallbackLocationText: String
                get() = "$name in ${enclosingElement.fallbackLocationText}"

            override val hasDefaultValue: Boolean
                get() = false

            override val closestMemberContainer: XMemberContainer by lazy {
                enclosingElement.closestMemberContainer
            }

            override fun asMemberOf(other: XType): KspType {
                if (closestMemberContainer.type?.isSameType(other) != false) {
                    return type
                }
                check(other is KspType)
                return enclosingElement.field.asMemberOf(other)
                    .copyWithScope(
                        KSTypeVarianceResolverScope.PropertySetterParameterType(
                            setterMethod = enclosingElement,
                            asMemberOf = other,
                        )
                    )
            }

            override val docComment: String?
                get() = null

            override fun kindName(): String {
                return "method parameter"
            }

            override fun validate(): Boolean {
                return true
            }
        }
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            accessor: KSPropertyAccessor
        ): KspSyntheticPropertyMethodElement {
            val enclosingType = accessor.receiver.findEnclosingMemberContainer(env)

            checkNotNull(enclosingType) {
                "XProcessing does not currently support annotations on top level " +
                    "properties with KSP. Cannot process $accessor."
            }

            val field = KspFieldElement(
                env = env,
                declaration = accessor.receiver,
            )
            return create(
                env = env,
                field = field,
                accessor = accessor
            )
        }

        fun create(
            env: KspProcessingEnv,
            field: KspFieldElement,
            accessor: KSPropertyAccessor
        ): KspSyntheticPropertyMethodElement {
            return when (accessor) {
                is KSPropertyGetter -> {
                    Getter(
                        env = env,
                        field = field,
                        accessor = accessor
                    )
                }
                is KSPropertySetter -> {
                    Setter(
                        env = env,
                        field = field,
                        accessor = accessor
                    )
                }
                else -> error("Unsupported property accessor $accessor")
            }
        }
    }
}
