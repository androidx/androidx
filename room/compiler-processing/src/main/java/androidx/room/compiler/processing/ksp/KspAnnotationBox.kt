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
import com.google.devtools.ksp.symbol.KSType
import java.lang.reflect.Proxy

@Suppress("UNCHECKED_CAST")
internal class KspAnnotationBox<T : Annotation>(
    private val env: KspProcessingEnv,
    private val annotationClass: Class<T>,
    private val annotation: KSAnnotation
) : XAnnotationBox<T> {
    override fun getAsType(methodName: String): XType? {
        val value = getFieldValue<KSType>(methodName)
        return value?.let {
            env.wrap(
                ksType = it,
                allowPrimitives = true
            )
        }
    }

    override fun getAsTypeList(methodName: String): List<XType> {
        val values = getFieldValue<List<KSType>>(methodName) ?: return emptyList()
        return values.map {
            env.wrap(
                ksType = it,
                allowPrimitives = true
            )
        }
    }

    override fun <R : Annotation> getAsAnnotationBox(methodName: String): XAnnotationBox<R> {
        val value = getFieldValue<KSAnnotation>(methodName) ?: error("cannot get annotation")
        val annotationType = annotationClass.methods.first {
            it.name == methodName
        }.returnType as Class<R>
        return KspAnnotationBox(
            env = env,
            annotationClass = annotationType,
            annotation = value
        )
    }

    private inline fun <reified R> getFieldValue(methodName: String): R? {
        val value = annotation.arguments.firstOrNull {
            it.name?.asString() == methodName
        }?.value ?: return null
        return value as R?
    }

    override fun <R : Annotation> getAsAnnotationBoxArray(
        methodName: String
    ): Array<XAnnotationBox<R>> {
        val values = getFieldValue<ArrayList<*>>(methodName) ?: return emptyArray()
        val annotationType = annotationClass.methods.first {
            it.name == methodName
        }.returnType.componentType as Class<R>
        return values.map {
            KspAnnotationBox<R>(
                env = env,
                annotationClass = annotationType,
                annotation = it as KSAnnotation
            )
        }.toTypedArray()
    }

    private val valueProxy: T = Proxy.newProxyInstance(
        KspAnnotationBox::class.java.classLoader,
        arrayOf(annotationClass)
    ) { _, method, _ ->
        val fieldValue = getFieldValue(method.name) ?: method.defaultValue
        // java gives arrays, kotlin gives array list (sometimes?) so fix it up
        when {
            fieldValue == null -> null
            method.returnType.isArray && (fieldValue is ArrayList<*>) -> {
                val componentType = method.returnType.componentType!!
                val result =
                    java.lang.reflect.Array.newInstance(componentType, fieldValue.size) as Array<*>
                fieldValue.toArray(result)
                result
            }
            else -> fieldValue
        }
    } as T

    override val value: T
        get() = valueProxy
}
