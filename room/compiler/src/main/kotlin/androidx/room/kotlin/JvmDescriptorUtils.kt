/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.kotlin

import com.google.auto.common.MoreElements
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.NoType
import javax.lang.model.type.NullType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.WildcardType
import javax.lang.model.util.AbstractTypeVisitor6
import javax.lang.model.util.Types

/**
 * Returns the method descriptor of this [ExecutableElement].
 *
 * For reference, see the [JVM specification, section 4.3.3](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3)
 */
fun ExecutableElement.descriptor(typeUtils: Types) =
    "$simpleName${asType().descriptor(typeUtils)}"

/**
 * Returns the name of this [TypeElement] in its "internal form".
 *
 * For reference, see the [JVM specification, section 4.2](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.2).
 */
internal val TypeElement.internalName: String
    get() = when (nestingKind) {
        NestingKind.TOP_LEVEL ->
            qualifiedName.toString().replace('.', '/')
        NestingKind.MEMBER ->
            MoreElements.asType(enclosingElement).internalName + "$" + simpleName
        NestingKind.LOCAL, NestingKind.ANONYMOUS ->
            error("Unsupported nesting $nestingKind")
    }

internal val NoType.descriptor: String
    get() = "V"

internal val DeclaredType.descriptor: String
    get() = "L" + MoreElements.asType(asElement()).internalName + ";"

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

fun TypeMirror.descriptor(typeUtils: Types): String =
    accept(JvmDescriptorTypeVisitor, typeUtils)

internal fun WildcardType.descriptor(typeUtils: Types): String =
    typeUtils.erasure(this).descriptor(typeUtils)

internal fun TypeVariable.descriptor(typeUtils: Types): String =
    typeUtils.erasure(this).descriptor(typeUtils)

internal fun ArrayType.descriptor(typeUtils: Types): String =
    "[" + componentType.descriptor(typeUtils)

internal fun ExecutableType.descriptor(typeUtils: Types): String {
    val parameterDescriptors =
        parameterTypes.joinToString(separator = "") { it.descriptor(typeUtils) }
    val returnDescriptor = returnType.descriptor(typeUtils)
    return "($parameterDescriptors)$returnDescriptor"
}

/**
 * When applied over a type, it returns either:
 * + a "field descriptor", for example: `Ljava/lang/Object;`
 * + a "method descriptor", for example: `(Ljava/lang/Object;)Z`
 *
 * The easiest way to use this is through [TypeMirror.descriptor][JvmDescriptorUtils.descriptor] in [JvmDescriptorUtils].
 *
 * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
 */
internal object JvmDescriptorTypeVisitor : AbstractTypeVisitor6<String, Types>() {
    override fun visitNoType(t: NoType, typeUtils: Types): String = t.descriptor

    override fun visitDeclared(t: DeclaredType, typeUtils: Types): String = t.descriptor

    override fun visitPrimitive(t: PrimitiveType, typeUtils: Types): String = t.descriptor

    override fun visitArray(t: ArrayType, typeUtils: Types): String = t.descriptor(typeUtils)

    override fun visitWildcard(t: WildcardType, typeUtils: Types): String = t.descriptor(typeUtils)

    override fun visitExecutable(t: ExecutableType, typeUtils: Types): String =
        t.descriptor(typeUtils)

    override fun visitTypeVariable(t: TypeVariable, typeUtils: Types): String =
        t.descriptor(typeUtils)

    override fun visitNull(t: NullType, typeUtils: Types): String =
        visitUnknown(t, typeUtils)

    override fun visitError(t: ErrorType, typeUtils: Types): String =
        visitUnknown(t, typeUtils)

    override fun visitUnknown(t: TypeMirror, typeUtils: Types): String =
        error("Unsupported type $t")
}