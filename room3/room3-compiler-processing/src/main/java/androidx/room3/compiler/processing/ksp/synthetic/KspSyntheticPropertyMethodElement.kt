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

package androidx.room3.compiler.processing.ksp.synthetic

import androidx.room3.compiler.processing.XAnnotated
import androidx.room3.compiler.processing.XEquality
import androidx.room3.compiler.processing.XExecutableParameterElement
import androidx.room3.compiler.processing.XHasModifiers
import androidx.room3.compiler.processing.XMemberContainer
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XMethodType
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.XTypeParameterElement
import androidx.room3.compiler.processing.javac.kotlin.JvmAbi
import androidx.room3.compiler.processing.ksp.KSTypeVarianceResolverScope
import androidx.room3.compiler.processing.ksp.KspAnnotated
import androidx.room3.compiler.processing.ksp.KspAnnotated.UseSiteFilter.NO_USE_SITE_OR_GETTER
import androidx.room3.compiler.processing.ksp.KspAnnotated.UseSiteFilter.NO_USE_SITE_OR_SETTER
import androidx.room3.compiler.processing.ksp.KspAnnotated.UseSiteFilter.NO_USE_SITE_OR_SET_PARAM
import androidx.room3.compiler.processing.ksp.KspFieldElement
import androidx.room3.compiler.processing.ksp.KspHasModifiers
import androidx.room3.compiler.processing.ksp.KspMemberContainer
import androidx.room3.compiler.processing.ksp.KspProcessingEnv
import androidx.room3.compiler.processing.ksp.KspType
import androidx.room3.compiler.processing.ksp.hasJvmStaticAnnotation
import androidx.room3.compiler.processing.ksp.isEnclosedInCompanionObject
import androidx.room3.compiler.processing.ksp.jvmDescriptor
import androidx.room3.compiler.processing.ksp.overrides
import androidx.room3.compiler.processing.ksp.requireEnclosingMemberContainer
import androidx.room3.compiler.processing.util.sanitizeAsJavaParameterName
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.Origin

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
    val isSyntheticStatic: Boolean,
    open val accessor: KSPropertyAccessor,
) :
    XMethodElement,
    XEquality,
    XHasModifiers by KspHasModifiers.create(accessor, isSyntheticStatic) {

    override val propertyName = field.name

    @OptIn(KspExperimental::class)
    override val jvmName: String by lazy {
        env.resolver.getJvmName(accessor) ?: error("Cannot find the name for accessor $accessor")
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(field, accessor, isSyntheticStatic)
    }

    // NOTE: modifiers of the property are not necessarily my modifiers.
    //  that being said, it only matters if it is private in which case KAPT does not generate the
    //  synthetic hence we don't either.
    final override fun isJavaDefault() = false

    final override fun hasKotlinDefaultImpl() = false

    final override fun isSuspendFunction() = false

    final override fun isExtensionFunction() = false

    final override val enclosingElement: KspMemberContainer
        get() =
            if (isSyntheticStatic) {
                actualEnclosingElement.declaration!!.requireEnclosingMemberContainer(env)
            } else {
                actualEnclosingElement
            }

    private val actualEnclosingElement: KspMemberContainer
        get() = this.field.enclosingElement

    final override val closestMemberContainer: XMemberContainer by lazy {
        enclosingElement.closestMemberContainer
    }

    /**
     * Returns the synthetic static accessor method for the companion object's enclosing class.
     *
     * If the property for this accessor method is declared in a Kotlin companion object, then this
     * will return the synthetic static accessor method (i.e. getter or setter) owned by the
     * companion object's enclosing class (to match KAPT). Otherwise, this will return null.
     */
    val syntheticStaticAccessor: KspSyntheticPropertyMethodElement? by lazy {
        // If this is already a synthetic static method don't create another one.
        if (
            !isSyntheticStatic &&
                field.declaration.isEnclosedInCompanionObject() &&
                (field.declaration.hasJvmStaticAnnotation() || accessor.hasJvmStaticAnnotation())
        ) {
            createInternal(env, field, accessor, isSyntheticStatic = true)
        } else {
            null
        }
    }

    final override fun isVarArgs() = false

    final override val executableType: XMethodType by lazy {
        KspSyntheticPropertyMethodType.create(
            env = env,
            element = this,
            container = field.enclosingElement.type,
        )
    }

    override val docComment: String?
        get() = null

    override fun validate(): Boolean {
        return true
    }

    @OptIn(KspExperimental::class)
    override val thrownTypes: List<XType> by lazy {
        env.resolver
            .getJvmCheckedException(accessor)
            .map {
                env.wrap(
                    // Thrown exception types are never nullable
                    ksType = it.makeNotNullable(),
                    allowPrimitives = false,
                )
            }
            .toList()
    }

    final override fun asMemberOf(other: XType): XMethodType {
        check(other is KspType)
        return KspSyntheticPropertyMethodType.create(env = env, element = this, container = other)
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
        override val accessor: KSPropertyGetter,
        isSyntheticStatic: Boolean,
    ) :
        KspSyntheticPropertyMethodElement(
            env = env,
            field = field,
            accessor = accessor,
            isSyntheticStatic = isSyntheticStatic,
        ),
        XAnnotated by KspAnnotated.create(
            env = env,
            delegate = accessor,
            filter = NO_USE_SITE_OR_GETTER,
        ) {

        override fun isKotlinPropertySetter() = false

        override fun isKotlinPropertyGetter() = true

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
        override val accessor: KSPropertySetter,
        isSyntheticStatic: Boolean,
    ) :
        KspSyntheticPropertyMethodElement(
            env = env,
            field = field,
            accessor = accessor,
            isSyntheticStatic = isSyntheticStatic,
        ),
        XAnnotated by KspAnnotated.create(
            env = env,
            delegate = field.declaration.setter,
            filter = NO_USE_SITE_OR_SETTER,
        ) {

        override fun isKotlinPropertySetter() = true

        override fun isKotlinPropertyGetter() = false

        override val name by lazy {
            JvmAbi.computeSetterName(field.declaration.simpleName.asString())
        }

        override val jvmDescriptor: String
            get() = this.jvmDescriptor()

        override val returnType: XType by lazy { env.voidType }

        override val typeParameters: List<XTypeParameterElement>
            get() = emptyList()

        override val parameters: List<XExecutableParameterElement> by lazy {
            listOf(SyntheticExecutableParameterElement(env = env, enclosingElement = this))
        }

        override fun kindName(): String {
            return "synthetic property getter"
        }

        internal class SyntheticExecutableParameterElement(
            internal val env: KspProcessingEnv,
            override val enclosingElement: Setter,
        ) :
            XExecutableParameterElement,
            XAnnotated by KspAnnotated.create(
                env = env,
                delegate = enclosingElement.field.declaration.setter?.parameter,
                filter = NO_USE_SITE_OR_SET_PARAM,
            ) {
            override fun isContinuationParam() = false

            override fun isReceiverParam() = false

            override fun isKotlinPropertyParam() = true

            override fun isVarArgs() = false

            override val name: String by lazy {
                val param = enclosingElement.accessor.parameter
                param.name?.asString().let {
                    if (
                        it == "<set-?>" ||
                            // In KSP2 synthetic setters' parameter name is `value`.
                            param.origin == Origin.SYNTHETIC
                    ) {
                        "p0"
                    } else {
                        it
                    }
                } ?: "_no_param_name"
            }

            override val jvmName: String by lazy { name.sanitizeAsJavaParameterName(0) }

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
                return enclosingElement.field
                    .asMemberOf(other)
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
        fun create(env: KspProcessingEnv, field: KspFieldElement, accessor: KSPropertyAccessor) =
            createInternal(env = env, field = field, accessor = accessor, isSyntheticStatic = false)

        private fun createInternal(
            env: KspProcessingEnv,
            field: KspFieldElement,
            accessor: KSPropertyAccessor,
            isSyntheticStatic: Boolean,
        ): KspSyntheticPropertyMethodElement {
            return when (accessor) {
                is KSPropertyGetter -> {
                    Getter(
                        env = env,
                        field = field,
                        accessor = accessor,
                        isSyntheticStatic = isSyntheticStatic,
                    )
                }
                is KSPropertySetter -> {
                    Setter(
                        env = env,
                        field = field,
                        accessor = accessor,
                        isSyntheticStatic = isSyntheticStatic,
                    )
                }
                else -> error("Unsupported property accessor $accessor")
            }
        }
    }
}
