/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XRawType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.XTypeVariableType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.javapoet.TypeName
import kotlin.reflect.KClass

/**
 * A KSP [XType] representing the type var type declared in a function.
 *
 * This class is different than [KspTypeVariableType] due to KSP having no type var model and only
 * type var declaration (i.e. [KSTypeParameter]).
 */
internal class KspMethodTypeVariableType(
    env: KspProcessingEnv,
    val ksTypeVariable: KSTypeParameter,
) : KspAnnotated(env), XTypeVariableType, XEquality {

    override val typeName: TypeName by lazy {
        xTypeName.java
    }

    override fun asTypeName() = xTypeName

    private val xTypeName: XTypeName by lazy {
        XTypeName(
            ksTypeVariable.asJTypeName(env.resolver),
            ksTypeVariable.asKTypeName(env.resolver),
            nullability
        )
    }

    override val upperBounds: List<XType> = ksTypeVariable.bounds.map(env::wrap).toList()

    override fun annotations(): Sequence<KSAnnotation> {
        return ksTypeVariable.annotations
    }

    override val rawType: XRawType by lazy {
        object : XRawType {
            override val typeName: TypeName
                get() = this@KspMethodTypeVariableType.typeName

            override fun asTypeName(): XTypeName =
                this@KspMethodTypeVariableType.asTypeName()

            override fun isAssignableFrom(other: XRawType): Boolean {
                return this.typeName == other.typeName
            }
        }
    }

    override val nullability: XNullability
        get() = XNullability.UNKNOWN

    override val superTypes: List<XType> by lazy {
        val anyType = env.requireType(XTypeName.ANY_OBJECT).makeNullable()
        if (upperBounds.size == 1 && upperBounds.single() == anyType) {
            upperBounds
        } else {
            listOf(anyType) + upperBounds
        }
    }

    override val typeElement: XTypeElement?
        get() = null

    override val typeArguments: List<XType>
        get() = emptyList()

    override fun isAssignableFrom(other: XType): Boolean {
        val typeVar = when (other) {
            is KspTypeVariableType -> other.ksTypeVariable
            is KspMethodTypeVariableType -> other.ksTypeVariable
            else -> null
        }
        return ksTypeVariable == typeVar
    }

    override fun isError(): Boolean {
        return false
    }

    override fun defaultValue(): String {
        return "null"
    }

    override fun boxed(): KspMethodTypeVariableType {
        return this
    }

    override fun isNone(): Boolean {
        return false
    }

    override fun isTypeOf(other: KClass<*>): Boolean {
        return false
    }

    override fun isSameType(other: XType): Boolean {
        val typeVar = when (other) {
            is KspTypeVariableType -> other.ksTypeVariable
            is KspMethodTypeVariableType -> other.ksTypeVariable
            else -> null
        }
        return ksTypeVariable == typeVar
    }

    override fun extendsBound(): XType? {
        return null
    }

    override fun makeNullable(): XType {
        return this
    }

    override fun makeNonNullable(): XType {
        return this
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(ksTypeVariable)
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return ksTypeVariable.toString()
    }
}
