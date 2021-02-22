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

import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import java.lang.reflect.Proxy

@Suppress("UNCHECKED_CAST")
internal class KspAnnotationBox<T : Annotation>(
    private val env: KspProcessingEnv,
    private val annotationClass: Class<T>,
    private val annotation: KSAnnotation
) : XAnnotationBox<T> {
    override fun getAsType(methodName: String): XType? {
        val value = getFieldValue(methodName, KSType::class.java)
        return value?.let {
            env.wrap(
                ksType = it,
                allowPrimitives = true
            )
        }
    }

    override fun getAsTypeList(methodName: String): List<XType> {
        val values = getFieldValue(methodName, Array::class.java) ?: return emptyList()
        return values.filterIsInstance<KSType>().map {
            env.wrap(
                ksType = it,
                allowPrimitives = true
            )
        }
    }

    override fun <R : Annotation> getAsAnnotationBox(methodName: String): XAnnotationBox<R> {
        val value = getFieldValue(methodName, KSAnnotation::class.java)
        @Suppress("FoldInitializerAndIfToElvis")
        if (value == null) {
            // see https://github.com/google/ksp/issues/53
            return KspReflectiveAnnotationBox.createFromDefaultValue(
                env = env,
                annotationClass = annotationClass,
                methodName = methodName
            )
        }

        val annotationType = annotationClass.methods.first {
            it.name == methodName
        }.returnType as Class<R>
        return KspAnnotationBox(
            env = env,
            annotationClass = annotationType,
            annotation = value
        )
    }

    @Suppress("SyntheticAccessor")
    private fun <R : Any> getFieldValue(
        methodName: String,
        returnType: Class<R>
    ): R? {
        val methodValue = annotation.arguments.firstOrNull {
            it.name?.asString() == methodName
        }?.value
        return methodValue?.readAs(returnType)
    }

    override fun <R : Annotation> getAsAnnotationBoxArray(
        methodName: String
    ): Array<XAnnotationBox<R>> {
        val values = getFieldValue(methodName, Array::class.java) ?: return emptyArray()
        val annotationType = annotationClass.methods.first {
            it.name == methodName
        }.returnType.componentType as Class<R>
        if (values.isEmpty()) {
            // KSP is unable to read defaults and returns empty array in that case.
            // Subsequently, we don't know if developer set it to empty array intentionally or
            // left it to default.
            // we error on the side of default
            return KspReflectiveAnnotationBox.createFromDefaultValues(
                env = env,
                annotationClass = annotationClass,
                methodName = methodName
            )
        }
        return values.map {
            KspAnnotationBox(
                env = env,
                annotationClass = annotationType,
                annotation = it as KSAnnotation
            )
        }.toTypedArray()
    }

    private val valueProxy: T = Proxy.newProxyInstance(
        annotationClass.classLoader,
        arrayOf(annotationClass)
    ) { _, method, _ ->
        getFieldValue(method.name, method.returnType) ?: method.defaultValue
    } as T

    override val value: T
        get() = valueProxy
}

@Suppress("UNCHECKED_CAST")
private fun <R> Any.readAs(returnType: Class<R>): R? {
    return when {
        returnType.isArray -> {
            val values: List<Any?> = when (this) {
                is List<*> -> {
                    // KSP might return list for arrays. convert it back.
                    this.mapNotNull {
                        it?.readAs(returnType.componentType)
                    }
                }
                is Array<*> -> mapNotNull { it?.readAs(returnType.componentType) }
                else -> {
                    // If array syntax is not used in java code, KSP might return it as a single
                    // item instead of list or array
                    // see: https://github.com/google/ksp/issues/214
                    listOf(this.readAs(returnType.componentType))
                }
            }
            if (returnType.componentType.isPrimitive) {
                when (returnType) {
                    IntArray::class.java ->
                        (values as Collection<Int>).toIntArray()
                    else -> {
                        // We don't have the use case for these yet but could be implemented in
                        // the future. Also need to implement them in JavacAnnotationBox
                        // b/179081610
                        error("Unsupported primitive array type: $returnType")
                    }
                }
            } else {
                val resultArray = java.lang.reflect.Array.newInstance(
                    returnType.componentType,
                    values.size
                ) as Array<Any?>
                values.forEachIndexed { index, value ->
                    resultArray[index] = value
                }
                resultArray
            }
        }
        returnType.isEnum -> {
            this.readAsEnum(returnType)
        }
        else -> this
    } as R?
}

private fun <R> Any.readAsEnum(enumClass: Class<R>): R? {
    val ksType = this as? KSType ?: return null
    val classDeclaration = ksType.declaration as? KSClassDeclaration ?: return null
    val enumValue = classDeclaration.simpleName.asString()
    // get the instance from the valueOf function.
    @Suppress("UNCHECKED_CAST", "BanUncheckedReflection")
    return enumClass.getDeclaredMethod("valueOf", String::class.java)
        .invoke(null, enumValue) as R?
}