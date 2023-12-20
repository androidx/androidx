/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import com.google.auto.common.AnnotationMirrors
import java.lang.reflect.Proxy
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor8

internal interface JavacClassGetter {
    fun getAsType(methodName: String): XType?
    fun getAsTypeList(methodName: String): List<XType>
    fun <T : Annotation> getAsAnnotationBox(methodName: String): XAnnotationBox<T>
    fun <T : Annotation> getAsAnnotationBoxArray(methodName: String): Array<XAnnotationBox<T>>
}

/**
 * Class that helps to read values from annotations. Simple types as string, int, lists can
 * be read from [value]. If you need to read classes or another annotations from annotation use
 * [getAsType], [getAsAnnotationBox] and [getAsAnnotationBoxArray] correspondingly.
 */
internal class JavacAnnotationBox<T : Annotation>(obj: Any) : XAnnotationBox<T> {
    private val classGetter = obj as JavacClassGetter

    @Suppress("UNCHECKED_CAST")
    override val value: T = obj as T
    override fun getAsType(methodName: String): XType? = classGetter.getAsType(methodName)

    override fun getAsTypeList(methodName: String): List<XType> =
        classGetter.getAsTypeList(methodName)

    override fun <T : Annotation> getAsAnnotationBox(methodName: String): XAnnotationBox<T> {
        return classGetter.getAsAnnotationBox(methodName)
    }

    override fun <T : Annotation> getAsAnnotationBoxArray(
        methodName: String
    ): Array<XAnnotationBox<T>> {
        return classGetter.getAsAnnotationBoxArray(methodName)
    }
}

internal fun <T : Annotation> AnnotationMirror.box(
    env: JavacProcessingEnv,
    cl: Class<T>
): JavacAnnotationBox<T> {
    if (!cl.isAnnotation) {
        throw IllegalArgumentException("$cl is not annotation")
    }
    val map = cl.declaredMethods.associate { method ->
        val value = AnnotationMirrors.getAnnotationValue(this, method.name)
        val returnType = method.returnType
        val defaultValue = method.defaultValue
        val result: Any? = when {
            returnType == Int::class.java -> value.getAsInt(defaultValue as Int?)
            returnType == Double::class.java -> value.getAsDouble(defaultValue as Double?)
            returnType == Float::class.java -> value.getAsFloat(defaultValue as Float?)
            returnType == Char::class.java -> value.getAsChar(defaultValue as Char?)
            returnType == Byte::class.java -> value.getAsByte(defaultValue as Byte?)
            returnType == Short::class.java -> value.getAsShort(defaultValue as Short?)
            returnType == Long::class.java -> value.getAsLong(defaultValue as Long?)
            returnType == Boolean::class.java -> value.getAsBoolean(defaultValue as Boolean)
            returnType == String::class.java -> value.getAsString(defaultValue as String?)
            returnType == Array<String>::class.java -> value.getAsStringList().toTypedArray()
            returnType == emptyArray<Class<*>>()::class.java -> value.toListOfClassTypes(env)
            returnType == IntArray::class.java -> value.getAsIntList().toIntArray()
            returnType == DoubleArray::class.java -> value.getAsDoubleList().toDoubleArray()
            returnType == FloatArray::class.java -> value.getAsFloatList().toFloatArray()
            returnType == CharArray::class.java -> value.getAsCharList().toCharArray()
            returnType == ByteArray::class.java -> value.getAsByteList().toByteArray()
            returnType == ShortArray::class.java -> value.getAsShortList().toShortArray()
            returnType == LongArray::class.java -> value.getAsLongList().toLongArray()
            returnType == BooleanArray::class.java -> value.getAsBooleanList().toBooleanArray()
            returnType == Class::class.java -> {
                try {
                    value.toClassType(env)
                } catch (notPresent: TypeNotPresentException) {
                    null
                }
            }
            returnType.isAnnotation -> {
                @Suppress("UNCHECKED_CAST")
                AnnotationClassVisitor(env, returnType as Class<out Annotation>).visit(value)
            }
            returnType.isArray && returnType.componentType.isAnnotation -> {
                @Suppress("UNCHECKED_CAST")
                AnnotationListVisitor(env, returnType.componentType as Class<out Annotation>)
                    .visit(value)
            }
            returnType.isArray && returnType.componentType.isEnum -> {
                @Suppress("UNCHECKED_CAST")
                EnumListVisitor(returnType.componentType as Class<out Enum<*>>).visit(value)
            }
            returnType.isEnum -> {
                @Suppress("UNCHECKED_CAST")
                value.getAsEnum(returnType as Class<out Enum<*>>)
            }
            else -> {
                throw UnsupportedOperationException("$returnType isn't supported")
            }
        }
        method.name to result
    }
    return JavacAnnotationBox(
        Proxy.newProxyInstance(
            JavacClassGetter::class.java.classLoader,
            arrayOf(cl, JavacClassGetter::class.java)
        ) { _, method, args ->
            when (method.name) {
                JavacClassGetter::getAsType.name -> map[args[0]]
                JavacClassGetter::getAsTypeList.name -> map[args[0]]
                "getAsAnnotationBox" -> map[args[0]]
                "getAsAnnotationBoxArray" -> map[args[0]]
                else -> map[method.name]
            }
        }
    )
}

private val ANNOTATION_VALUE_TO_INT_VISITOR = object : SimpleAnnotationValueVisitor8<Int?, Void>() {
    override fun visitInt(i: Int, p: Void?): Int {
        return i
    }
}

private val ANNOTATION_VALUE_TO_DOUBLE_VISITOR =
    object : SimpleAnnotationValueVisitor8<Double?, Void>() {
        override fun visitDouble(i: Double, p: Void?): Double {
            return i
        }
    }

private val ANNOTATION_VALUE_TO_FLOAT_VISITOR =
    object : SimpleAnnotationValueVisitor8<Float?, Void>() {
        override fun visitFloat(i: Float, p: Void?): Float {
            return i
        }
    }

private val ANNOTATION_VALUE_TO_CHAR_VISITOR =
    object : SimpleAnnotationValueVisitor8<Char?, Void>() {
        override fun visitChar(i: Char, p: Void?): Char {
            return i
        }
    }

private val ANNOTATION_VALUE_TO_BYTE_VISITOR =
    object : SimpleAnnotationValueVisitor8<Byte?, Void>() {
        override fun visitByte(i: Byte, p: Void?): Byte {
            return i
        }
    }

private val ANNOTATION_VALUE_TO_SHORT_VISITOR =
    object : SimpleAnnotationValueVisitor8<Short?, Void>() {
        override fun visitShort(i: Short, p: Void?): Short {
            return i
        }
    }

private val ANNOTATION_VALUE_TO_LONG_VISITOR =
    object : SimpleAnnotationValueVisitor8<Long?, Void>() {
        override fun visitLong(i: Long, p: Void?): Long {
            return i
        }
    }

private val ANNOTATION_VALUE_TO_BOOLEAN_VISITOR = object :
    SimpleAnnotationValueVisitor8<Boolean?, Void>() {
    override fun visitBoolean(b: Boolean, p: Void?): Boolean {
        return b
    }
}

private val ANNOTATION_VALUE_TO_STRING_VISITOR = object :
    SimpleAnnotationValueVisitor8<String?, Void>() {
    override fun visitString(s: String?, p: Void?): String? {
        return s
    }
}

private val ANNOTATION_VALUE_STRING_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<String>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<String> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_STRING_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private val ANNOTATION_VALUE_INT_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<Int>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Int> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_INT_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private val ANNOTATION_VALUE_DOUBLE_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<Double>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Double> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_DOUBLE_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private val ANNOTATION_VALUE_FLOAT_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<Float>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Float> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_FLOAT_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private val ANNOTATION_VALUE_CHAR_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<Char>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Char> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_CHAR_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private val ANNOTATION_VALUE_BYTE_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<Byte>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Byte> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_BYTE_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private val ANNOTATION_VALUE_SHORT_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<Short>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Short> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_SHORT_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private val ANNOTATION_VALUE_LONG_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<Long>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Long> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_LONG_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private val ANNOTATION_VALUE_BOOLEAN_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor8<List<Boolean>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Boolean> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_BOOLEAN_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private fun AnnotationValue.getAsInt(def: Int? = null): Int? {
    return ANNOTATION_VALUE_TO_INT_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsDouble(def: Double? = null): Double? {
    return ANNOTATION_VALUE_TO_DOUBLE_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsFloat(def: Float? = null): Float? {
    return ANNOTATION_VALUE_TO_FLOAT_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsChar(def: Char? = null): Char? {
    return ANNOTATION_VALUE_TO_CHAR_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsByte(def: Byte? = null): Byte? {
    return ANNOTATION_VALUE_TO_BYTE_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsShort(def: Short? = null): Short? {
    return ANNOTATION_VALUE_TO_SHORT_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsLong(def: Long? = null): Long? {
    return ANNOTATION_VALUE_TO_LONG_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsIntList(): List<Int> {
    return ANNOTATION_VALUE_INT_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsDoubleList(): List<Double> {
    return ANNOTATION_VALUE_DOUBLE_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsFloatList(): List<Float> {
    return ANNOTATION_VALUE_FLOAT_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsCharList(): List<Char> {
    return ANNOTATION_VALUE_CHAR_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsByteList(): List<Byte> {
    return ANNOTATION_VALUE_BYTE_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsShortList(): List<Short> {
    return ANNOTATION_VALUE_SHORT_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsLongList(): List<Long> {
    return ANNOTATION_VALUE_LONG_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsBooleanList(): List<Boolean> {
    return ANNOTATION_VALUE_BOOLEAN_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsString(def: String? = null): String? {
    return ANNOTATION_VALUE_TO_STRING_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsBoolean(def: Boolean): Boolean {
    return ANNOTATION_VALUE_TO_BOOLEAN_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsStringList(): List<String> {
    return ANNOTATION_VALUE_STRING_ARR_VISITOR.visit(this)
}

// code below taken from dagger2
// compiler/src/main/java/dagger/internal/codegen/ConfigurationAnnotations.java
private val TO_LIST_OF_TYPES = object :
    SimpleAnnotationValueVisitor8<List<TypeMirror>, Void?>() {
    override fun visitArray(values: MutableList<out AnnotationValue>?, p: Void?): List<TypeMirror> {
        return values?.mapNotNull {
            val tmp = TO_TYPE.visit(it)
            tmp
        } ?: emptyList()
    }

    override fun defaultAction(o: Any?, p: Void?): List<TypeMirror> {
        return emptyList()
    }
}

private val TO_TYPE = object : SimpleAnnotationValueVisitor8<TypeMirror, Void>() {

    override fun visitType(t: TypeMirror, p: Void?): TypeMirror {
        return t
    }

    override fun defaultAction(o: Any?, p: Void?): TypeMirror {
        throw TypeNotPresentException(o!!.toString(), null)
    }
}

private fun AnnotationValue.toListOfClassTypes(env: JavacProcessingEnv): List<XType> {
    return TO_LIST_OF_TYPES.visit(this).map {
        env.wrap<JavacType>(
            typeMirror = it,
            kotlinType = null,
            elementNullability = XNullability.UNKNOWN
        )
    }
}

private fun AnnotationValue.toClassType(env: JavacProcessingEnv): XType? {
    return TO_TYPE.visit(this)?.let {
        env.wrap(
            typeMirror = it,
            kotlinType = null,
            elementNullability = XNullability.UNKNOWN
        )
    }
}

private class AnnotationListVisitor<T : Annotation>(
    private val env: JavacProcessingEnv,
    private val annotationClass: Class<T>
) :
    SimpleAnnotationValueVisitor8<Array<JavacAnnotationBox<T>>, Void?>() {
    override fun visitArray(
        values: MutableList<out AnnotationValue>?,
        void: Void?
    ): Array<JavacAnnotationBox<T>> {
        val visitor = AnnotationClassVisitor(env, annotationClass)
        return values?.mapNotNull { visitor.visit(it) }?.toTypedArray() ?: emptyArray()
    }
}

private class EnumListVisitor<T : Enum<T>>(private val enumClass: Class<T>) :
    SimpleAnnotationValueVisitor8<Array<T>, Void?>() {
    override fun visitArray(
        values: MutableList<out AnnotationValue>?,
        void: Void?
    ): Array<T> {
        val result = values?.map { it.getAsEnum(enumClass) }
        @Suppress("UNCHECKED_CAST")
        val resultArray = java.lang.reflect.Array
            .newInstance(enumClass, result?.size ?: 0) as Array<T>
        result?.forEachIndexed { index, value ->
            resultArray[index] = value
        }
        return resultArray
    }
}

private class AnnotationClassVisitor<T : Annotation>(
    private val env: JavacProcessingEnv,
    private val annotationClass: Class<T>
) :
    SimpleAnnotationValueVisitor8<JavacAnnotationBox<T>?, Void?>() {
    override fun visitAnnotation(a: AnnotationMirror?, v: Void?) = a?.box(env, annotationClass)
}

@Suppress("UNCHECKED_CAST", "BanUncheckedReflection")
private fun <T : Enum<*>> AnnotationValue.getAsEnum(enumClass: Class<T>): T {
    return object : SimpleAnnotationValueVisitor8<T, Void>() {
        override fun visitEnumConstant(value: VariableElement?, p: Void?): T {
            return enumClass.getDeclaredMethod("valueOf", String::class.java)
                .invoke(null, value!!.simpleName.toString()) as T
        }
    }.visit(this)
}
