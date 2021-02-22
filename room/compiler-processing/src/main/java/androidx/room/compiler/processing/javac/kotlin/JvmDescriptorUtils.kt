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

package androidx.room.compiler.processing.javac.kotlin

import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.NestingKind
import javax.lang.model.element.QualifiedNameable
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.IntersectionType
import javax.lang.model.type.NoType
import javax.lang.model.type.NullType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.UnionType
import javax.lang.model.type.WildcardType
import javax.lang.model.util.AbstractTypeVisitor8

/**
 * Returns the method descriptor of this [ExecutableElement].
 *
 * For reference, see the [JVM specification, section 4.3.2](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.2)
 */
internal fun VariableElement.descriptor() = "$simpleName:${asType().descriptor()}"

/**
 * Returns the method descriptor of this [ExecutableElement].
 *
 * For reference, see the [JVM specification, section 4.3.3](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3)
 */
internal fun ExecutableElement.descriptor() =
    "$simpleName${MoreTypes.asExecutable(asType()).descriptor()}"

/**
 * Returns the name of this [TypeElement] in its "internal form".
 *
 * For reference, see the [JVM specification, section 4.2](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.2).
 */
internal val Element.internalName: String
    get() = when (this) {
        is TypeElement ->
            when (nestingKind) {
                NestingKind.TOP_LEVEL ->
                    qualifiedName.toString().replace('.', '/')
                NestingKind.MEMBER ->
                    enclosingElement.internalName + "$" + simpleName
                NestingKind.LOCAL, NestingKind.ANONYMOUS ->
                    error("Unsupported nesting $nestingKind")
                else ->
                    error("Unsupported, nestingKind == null")
            }
        is QualifiedNameable -> qualifiedName.toString().replace('.', '/')
        else -> simpleName.toString()
    }

@Suppress("unused")
internal val NoType.descriptor: String
    get() = "V"

internal val DeclaredType.descriptor: String
    get() = "L" + asElement().internalName + ";"

internal val PrimitiveType.descriptor: String
    get() = when (this.kind) {
        TypeKind.BYTE -> "B"
        TypeKind.CHAR -> "C"
        TypeKind.DOUBLE -> "D"
        TypeKind.FLOAT -> "F"
        TypeKind.INT -> "I"
        TypeKind.LONG -> "J"
        TypeKind.SHORT -> "S"
        TypeKind.BOOLEAN -> "Z"
        else -> error("Unknown primitive type $this")
    }

// see https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.2-200
internal fun String.typeNameFromJvmSignature(): TypeName {
    check(isNotEmpty())
    return when (this[0]) {
        'B' -> TypeName.BYTE
        'C' -> TypeName.CHAR
        'D' -> TypeName.DOUBLE
        'F' -> TypeName.FLOAT
        'I' -> TypeName.INT
        'J' -> TypeName.LONG
        'S' -> TypeName.SHORT
        'Z' -> TypeName.BOOLEAN
        'L' -> {
            val end = lastIndexOf(";")
            check(end > 0) {
                "invalid input $this"
            }
            val simpleNamesSeparator = lastIndexOf('/')
            val simpleNamesStart = if (simpleNamesSeparator < 0) {
                1 // first char is 'L'
            } else {
                simpleNamesSeparator + 1
            }
            val packageName = if (simpleNamesSeparator < 0) {
                // no package name
                ""
            } else {
                substring(1, simpleNamesSeparator).replace('/', '.')
            }
            val firstSimpleNameSeparator = indexOf('$', startIndex = simpleNamesStart)
            return if (firstSimpleNameSeparator < 0) {
                // not nested
                ClassName.get(packageName, substring(simpleNamesStart, end))
            } else {
                // nested class
                val firstSimpleName = substring(simpleNamesStart, firstSimpleNameSeparator)
                val restOfSimpleNames = substring(firstSimpleNameSeparator + 1, end)
                    .split('$')
                    .toTypedArray()
                ClassName.get(packageName, firstSimpleName, *restOfSimpleNames)
            }
        }
        '[' -> ArrayTypeName.of(substring(1).typeNameFromJvmSignature())
        else -> error("unexpected jvm signature $this")
    }
}

internal fun TypeMirror.descriptor(): String = accept(JvmDescriptorTypeVisitor, Unit)

@Suppress("unused")
internal fun WildcardType.descriptor(): String = ""

// The erasure of a type variable is the erasure of its leftmost bound. - JVM Spec Sec 4.6
internal fun TypeVariable.descriptor(): String = this.upperBound.descriptor()

// For a type variable with multiple bounds: "the erasure of a type variable is determined by
// the first type in its bound" - JVM Spec Sec 4.4
internal fun IntersectionType.descriptor(): String =
    this.bounds[0].descriptor()

internal fun ArrayType.descriptor(): String =
    "[" + componentType.descriptor()

internal fun ExecutableType.descriptor(): String {
    val parameterDescriptors =
        parameterTypes.joinToString(separator = "") { it.descriptor() }
    val returnDescriptor = returnType.descriptor()
    return "($parameterDescriptors)$returnDescriptor"
}

/**
 * When applied over a type, it returns either:
 * + a "field descriptor", for example: `Ljava/lang/Object;`
 * + a "method descriptor", for example: `(Ljava/lang/Object;)Z`
 *
 * The easiest way to use this is through [TypeMirror.descriptor]
 *
 * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
 */
@Suppress("DEPRECATION")
internal object JvmDescriptorTypeVisitor : AbstractTypeVisitor8<String, Unit>() {

    override fun visitNoType(t: NoType, u: Unit): String = t.descriptor

    override fun visitDeclared(t: DeclaredType, u: Unit): String = t.descriptor

    override fun visitPrimitive(t: PrimitiveType, u: Unit): String = t.descriptor

    override fun visitArray(t: ArrayType, u: Unit): String = t.descriptor()

    override fun visitWildcard(t: WildcardType, u: Unit): String = t.descriptor()

    override fun visitExecutable(t: ExecutableType, u: Unit): String = t.descriptor()

    override fun visitTypeVariable(t: TypeVariable, u: Unit): String = t.descriptor()

    override fun visitNull(t: NullType, u: Unit): String = visitUnknown(t, u)

    override fun visitError(t: ErrorType, u: Unit): String = visitUnknown(t, u)

    override fun visitIntersection(t: IntersectionType, u: Unit) = t.descriptor()

    override fun visitUnion(t: UnionType, u: Unit) = visitUnknown(t, u)

    override fun visitUnknown(t: TypeMirror, u: Unit): String = error("Unsupported type $t")
}