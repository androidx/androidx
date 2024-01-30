/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import java.lang.reflect.Field
import java.lang.reflect.InaccessibleObjectException
import java.lang.reflect.Method
import java.util.function.Predicate
import org.jetbrains.annotations.NonNls

internal object ReflectionUtil {

    fun getDeclaredMethodOrNull(
        clazz: Class<*>,
        name: String,
        vararg parameters: Class<*>
    ): Method? =
        try {
            clazz.getDeclaredMethod(name, *parameters)
                .apply { isAccessible = true }
        } catch (e: NoSuchMethodException) {
            null
        } catch (e: InaccessibleObjectException) {
            null
        }

    fun <T> getFieldValueOrNull(
        objectClass: Class<*>,
        `object`: Any?,
        fieldType: Class<T>?,
        fieldName: String
    ): T? =
        try {
            val field: Field = getAssignableField(objectClass, fieldType, fieldName)
            getFieldValue<T>(field, `object`)
        } catch (e: NoSuchFieldException) {
            null
        }

    fun getAssignableField(
        clazz: Class<*>,
        fieldType: Class<*>?,
        @NonNls fieldName: String
    ) = findAssignableField(clazz, fieldType, fieldName)
        ?: throw NoSuchFieldException("Class: $clazz fieldName: $fieldName fieldType: $fieldType")

    fun findAssignableField(
        clazz: Class<*>,
        fieldType: Class<*>?,
        @NonNls fieldName: String
    ): Field? {
        val result = findFieldInHierarchy(
            clazz
        ) { field: Field ->
            fieldName == field.name && (fieldType == null || fieldType.isAssignableFrom(
                field.type
            ))
        }
        return result
    }

    fun findFieldInHierarchy(
        rootClass: Class<*>,
        checker: Predicate<in Field>
    ): Field? {
        var aClass: Class<*>? = rootClass

        try {
            while (aClass != null) {
                for (field in aClass.declaredFields) {
                    if (checker.test(field)) {
                        field.isAccessible = true
                        return field
                    }
                }
                aClass = aClass.superclass
            }
        } catch (e: InaccessibleObjectException) {
            return null
        }

        return processInterfaces(rootClass.interfaces, HashSet(), checker)
    }

    private fun processInterfaces(
        interfaces: Array<Class<*>>,
        visited: MutableSet<in Class<*>>,
        checker: Predicate<in Field>
    ): Field? {
        for (anInterface in interfaces) {
            if (!visited.add(anInterface)) {
                continue
            }
            for (field in anInterface.declaredFields) {
                if (checker.test(field)) {
                    field.isAccessible = true
                    return field
                }
            }
            val field = processInterfaces(anInterface.interfaces, visited, checker)
            if (field != null) {
                return field
            }
        }
        return null
    }

    private fun <T> getFieldValue(field: Field, instance: Any?): T? =
        try {
            @Suppress("UNCHECKED_CAST")
            field[instance] as T
        } catch (e: IllegalAccessException) {
            null
        }
}
