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
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
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
internal fun VariableElement.descriptor(env: ProcessingEnvironment) =
    "$simpleName:${asType().descriptor(env)}"

/**
 * Returns the method descriptor of this [ExecutableElement].
 *
 * For reference, see the [JVM specification, section 4.3.3](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3)
 */
internal fun ExecutableElement.descriptor(env: ProcessingEnvironment) =
    "$simpleName${asType().descriptor(env)}"

private fun TypeMirror.descriptor(env: ProcessingEnvironment) =
    JvmDescriptorTypeVisitor.visit(this, env)

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
private object JvmDescriptorTypeVisitor : AbstractTypeVisitor8<String, ProcessingEnvironment>() {

    override fun visitNoType(t: NoType, env: ProcessingEnvironment): String = "V"

    override fun visitDeclared(t: DeclaredType, env: ProcessingEnvironment): String =
        "L${t.asElement().internalName(env)};"

    override fun visitPrimitive(t: PrimitiveType, env: ProcessingEnvironment): String {
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

    override fun visitArray(t: ArrayType, env: ProcessingEnvironment): String =
        "[" + visit(t.componentType, env)

    override fun visitWildcard(t: WildcardType, env: ProcessingEnvironment): String =
        visitUnknown(t, env)

    override fun visitExecutable(t: ExecutableType, env: ProcessingEnvironment): String {
        val parameterDescriptors = t.parameterTypes.joinToString("") { visit(it, env) }
        val returnDescriptor = visit(t.returnType, env)
        return "($parameterDescriptors)$returnDescriptor"
    }

    override fun visitTypeVariable(t: TypeVariable, env: ProcessingEnvironment): String =
        visit(t.upperBound, env)

    override fun visitNull(t: NullType, env: ProcessingEnvironment): String = visitUnknown(t, env)

    override fun visitError(t: ErrorType, env: ProcessingEnvironment): String =
        visitDeclared(t, env)

    // For a type variable with multiple bounds: "the erasure of a type variable is determined
    // by the first type in its bound" - JLS Sec 4.4
    // See https://docs.oracle.com/javase/specs/jls/se16/html/jls-4.html#jls-4.4
    override fun visitIntersection(t: IntersectionType, env: ProcessingEnvironment): String =
        visit(t.bounds[0], env)

    override fun visitUnion(t: UnionType, env: ProcessingEnvironment): String = visitUnknown(t, env)

    override fun visitUnknown(t: TypeMirror, env: ProcessingEnvironment): String =
        error("Unsupported type $t")

    /**
     * Returns the name of this [TypeElement] in its "internal form".
     *
     * For reference, see the [JVM specification, section 4.2](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.2).
     */
    private fun Element.internalName(env: ProcessingEnvironment): String = when (this) {
        is TypeElement -> env.elementUtils.getBinaryName(this).toString().replace('.', '/')
        is ExecutableElement -> enclosingElement.internalName(env)
        is QualifiedNameable -> qualifiedName.toString().replace('.', '/')
        else -> simpleName.toString()
    }
}
