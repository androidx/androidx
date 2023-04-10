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
internal fun ExecutableElement.descriptor() = "$simpleName${asType().descriptor()}"

private fun TypeMirror.descriptor() = JvmDescriptorTypeVisitor.visit(this)

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

/**
 * When applied over a type, it returns either:
 * + a "field descriptor", for example: `Ljava/lang/Object;`
 * + a "method descriptor", for example: `(Ljava/lang/Object;)Z`
 *
 * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
 */
private object JvmDescriptorTypeVisitor : AbstractTypeVisitor8<String, Any?>() {

    override fun visitNoType(t: NoType, u: Any?): String = "V"

    override fun visitDeclared(t: DeclaredType, u: Any?): String = "L${t.asElement().internalName};"

    override fun visitPrimitive(t: PrimitiveType, u: Any?): String {
        return when (t.kind) {
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
    }

    override fun visitArray(t: ArrayType, u: Any?): String = "[" + visit(t.componentType)

    override fun visitWildcard(t: WildcardType, u: Any?): String = visitUnknown(t, u)

    override fun visitExecutable(t: ExecutableType, u: Any?): String {
        val parameterDescriptors = t.parameterTypes.joinToString("") { visit(it) }
        val returnDescriptor = visit(t.returnType)
        return "($parameterDescriptors)$returnDescriptor"
    }

    override fun visitTypeVariable(t: TypeVariable, u: Any?): String = visit(t.upperBound)

    override fun visitNull(t: NullType, u: Any?): String = visitUnknown(t, u)

    override fun visitError(t: ErrorType, u: Any?): String = visitDeclared(t, u)

    // For a type variable with multiple bounds: "the erasure of a type variable is determined
    // by the first type in its bound" - JLS Sec 4.4
    // See https://docs.oracle.com/javase/specs/jls/se16/html/jls-4.html#jls-4.4
    override fun visitIntersection(t: IntersectionType, u: Any?): String = visit(t.bounds[0])

    override fun visitUnion(t: UnionType, u: Any?): String = visitUnknown(t, u)

    override fun visitUnknown(t: TypeMirror, u: Any?): String = error("Unsupported type $t")

    /**
     * Returns the name of this [TypeElement] in its "internal form".
     *
     * For reference, see the [JVM specification, section 4.2](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.2).
     */
    private val Element.internalName: String
        get() = when (this) {
            is TypeElement ->
                when (nestingKind) {
                    NestingKind.TOP_LEVEL ->
                        qualifiedName.toString().replace('.', '/')
                    NestingKind.MEMBER, NestingKind.LOCAL ->
                        enclosingElement.internalName + "$" + simpleName
                    NestingKind.ANONYMOUS ->
                        error("Unsupported nesting $nestingKind")
                    else ->
                        error("Unsupported, nestingKind == null")
                }
            is ExecutableElement -> enclosingElement.internalName
            is QualifiedNameable -> qualifiedName.toString().replace('.', '/')
            else -> simpleName.toString()
        }
}