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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.tryBox
import androidx.room.compiler.processing.tryUnbox
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import kotlin.reflect.KClass

/**
 * XType implementation for KSP type.
 *
 * It might be initialized with a [KSTypeReference] or [KSType] depending on the call point.
 *
 * We don't necessarily have a [KSTypeReference] (e.g. if we are getting it from an element).
 * Similarly, we may not be able to get a [KSType] (e.g. if it resolves to error).
 */
internal abstract class KspType(
    val env: KspProcessingEnv,
    val ksType: KSType
) : XType, XEquality {
    override val rawType by lazy {
        KspRawType(this)
    }

    override val nullability by lazy {
        when (ksType.nullability) {
            Nullability.NULLABLE -> XNullability.NULLABLE
            Nullability.NOT_NULL -> XNullability.NONNULL
            else -> XNullability.UNKNOWN
        }
    }

    override val typeElement by lazy {
        // for primitive types, we could technically return null from here as they are not backed
        // by a type element in javac but in Kotlin we have types for them, hence returning them
        // is better.
        val declaration = ksType.declaration as? KSClassDeclaration
        declaration?.let {
            env.wrapClassDeclaration(it)
        }
    }

    override val typeArguments: List<XType> by lazy {
        ksType.arguments.mapIndexed { index, arg ->
            env.wrap(ksType.declaration.typeParameters[index], arg)
        }
    }

    override fun isAssignableFrom(other: XType): Boolean {
        check(other is KspType)
        return ksType.isAssignableFrom(other.ksType)
    }

    override fun isError(): Boolean {
        return ksType.isError
    }

    override fun defaultValue(): String {
        // NOTE: this does not match the java implementation though it is probably more correct for
        // kotlin.
        if (ksType.nullability == Nullability.NULLABLE) {
            return "null"
        }
        val builtIns = env.resolver.builtIns
        return when (ksType) {
            builtIns.booleanType -> "false"
            builtIns.byteType, builtIns.shortType, builtIns.intType, builtIns.longType, builtIns
                .charType -> "0"
            builtIns.floatType -> "0f"
            builtIns.doubleType -> "0.0"
            else -> "null"
        }
    }

    override fun isNone(): Boolean {
        // even void is converted to Unit so we don't have none type in KSP
        // see: KspTypeTest.noneType
        return false
    }

    override fun isTypeOf(other: KClass<*>): Boolean {
        // closest to what MoreTypes#isTypeOf does.
        // accept both boxed and unboxed because KClass.java for primitives wrappers will always
        // give the primitive (e.g. kotlin.Int::class.java is int)
        return rawType.typeName.tryBox().toString() == other.java.canonicalName ||
            rawType.typeName.tryUnbox().toString() == other.java.canonicalName
    }

    override fun isSameType(other: XType): Boolean {
        check(other is KspType)
        if (nullability == XNullability.UNKNOWN || other.nullability == XNullability.UNKNOWN) {
            // if one the nullabilities is unknown, it is coming from java source code or .class.
            // for those cases, use java platform type equality (via typename)
            return typeName == other.typeName
        }
        // NOTE: this is inconsistent with java where nullability is ignored.
        // it is intentional but might be reversed if it happens to break use cases.
        return ksType == other.ksType
    }

    override fun extendsBound(): XType? {
        // when we detect that there should be an extends bounds, KspProcessingEnv creates
        // [KspTypeArgumentType].
        return null
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(ksType)
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return ksType.toString()
    }

    abstract override fun boxed(): KspType

    /**
     * Create a copy of this type with the given nullability.
     * This method is not called if the nullability of the type is already equal to the given
     * nullability.
     */
    protected abstract fun copyWithNullability(nullability: XNullability): KspType

    final override fun makeNullable(): KspType {
        if (nullability == XNullability.NULLABLE) {
            return this
        }
        return copyWithNullability(XNullability.NULLABLE)
    }

    final override fun makeNonNullable(): KspType {
        if (nullability == XNullability.NONNULL) {
            return this
        }
        return copyWithNullability(XNullability.NONNULL)
    }
}
