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

package androidx.room.compiler.processing

import com.squareup.javapoet.TypeName

internal abstract class InternalXAnnotationValue : XAnnotationValue {
    /**
     * The kind of an annotation value.
     *
     * Note that a kind, say [Kind.STRING], is used for both `String` and `String[]` types. To
     * distinguish between `String` and `String[]` you can check `valueType.isArray()`.
     */
    private enum class Kind {
        BOOLEAN, INT, SHORT, LONG, FLOAT, DOUBLE, BYTE, CHAR, STRING, ENUM, ANNOTATION, TYPE;
        companion object {
            fun of(type: XType): Kind {
                if (type.isArray()) {
                    return of(type.componentType)
                }
                return when {
                    type.typeName == TypeName.BOOLEAN -> BOOLEAN
                    type.typeName == TypeName.INT -> INT
                    type.typeName == TypeName.SHORT -> SHORT
                    type.typeName == TypeName.LONG -> LONG
                    type.typeName == TypeName.FLOAT -> FLOAT
                    type.typeName == TypeName.DOUBLE -> DOUBLE
                    type.typeName == TypeName.BYTE -> BYTE
                    type.typeName == TypeName.CHAR -> CHAR
                    type.typeName == InternalXAnnotationValue.STRING -> STRING
                    type.typeName.rawTypeName() == CLASS -> TYPE
                    type.typeName.rawTypeName() == KCLASS -> TYPE
                    type.typeElement?.isEnum() == true -> ENUM
                    type.typeElement?.isAnnotationClass() == true -> ANNOTATION
                    else -> error("Unexpected type: $type")
                }
            }
        }
    }

    /** The kind of the value. */
    private val kind: Kind by lazy {
        Kind.of(valueType)
    }

    final override fun hasListValue() = valueType.isArray()

    /** Returns true if the value is an [XType] */
    final override fun hasTypeValue() = kind == Kind.TYPE && !hasListValue()

    /** Returns true if the value is a list of [XType] */
    final override fun hasTypeListValue() = kind == Kind.TYPE && hasListValue()

    /** Returns true if the value is an [XAnnotation] */
    final override fun hasAnnotationValue() = kind == Kind.ANNOTATION && !hasListValue()

    /** Returns true if the value is a list of [XAnnotation] */
    final override fun hasAnnotationListValue() = kind == Kind.ANNOTATION && hasListValue()

    /** Returns true if the value is an [XEnumEntry] */
    final override fun hasEnumValue() = kind == Kind.ENUM && !hasListValue()

    /** Returns true if the value is a list of [XEnumEntry] */
    final override fun hasEnumListValue() = kind == Kind.ENUM && hasListValue()

    /** Returns true if the value is an [Boolean] */
    final override fun hasBooleanValue() = kind == Kind.BOOLEAN && !hasListValue()

    /** Returns true if the value is a list of [Boolean] */
    final override fun hasBooleanListValue() = kind == Kind.BOOLEAN && hasListValue()

    /** Returns true if the value is an [Boolean] */
    final override fun hasStringValue() = kind == Kind.STRING && !hasListValue()

    /** Returns true if the value is a list of [String] */
    final override fun hasStringListValue() = kind == Kind.STRING && hasListValue()

    /** Returns true if the value is an [Int] */
    final override fun hasIntValue() = kind == Kind.INT && !hasListValue()

    /** Returns true if the value is a list of [Int] */
    final override fun hasIntListValue() = kind == Kind.INT && hasListValue()

    /** Returns true if the value is an [Long] */
    final override fun hasLongValue() = kind == Kind.LONG && !hasListValue()

    /** Returns true if the value is a list of [Long] */
    final override fun hasLongListValue() = kind == Kind.LONG && hasListValue()

    /** Returns true if the value is an [Short] */
    final override fun hasShortValue() = kind == Kind.SHORT && !hasListValue()

    /** Returns true if the value is a list of [Short] */
    final override fun hasShortListValue() = kind == Kind.SHORT && hasListValue()

    /** Returns true if the value is an [Float] */
    final override fun hasFloatValue() = kind == Kind.FLOAT && !hasListValue()

    /** Returns true if the value is a list of [Float] */
    final override fun hasFloatListValue() = kind == Kind.FLOAT && hasListValue()

    /** Returns true if the value is an [Double] */
    final override fun hasDoubleValue() = kind == Kind.DOUBLE && !hasListValue()

    /** Returns true if the value is a list of [Double] */
    final override fun hasDoubleListValue() = kind == Kind.DOUBLE && hasListValue()

    /** Returns true if the value is an [Char] */
    final override fun hasCharValue() = kind == Kind.CHAR && !hasListValue()

    /** Returns true if the value is a list of [Char] */
    final override fun hasCharListValue() = kind == Kind.CHAR && hasListValue()

    /** Returns true if the value is an [Byte] */
    final override fun hasByteValue() = kind == Kind.BYTE && !hasListValue()

    /** Returns true if the value is a list of [Byte] */
    final override fun hasByteListValue() = kind == Kind.BYTE && hasListValue()

    private companion object {
        val STRING: TypeName = TypeName.get(String::class.java)
        val CLASS: TypeName = TypeName.get(Class::class.java)
        val KCLASS: TypeName = TypeName.get(kotlin.reflect.KClass::class.java)
    }
}
