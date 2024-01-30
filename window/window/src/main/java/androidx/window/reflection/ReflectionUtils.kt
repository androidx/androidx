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

package androidx.window.reflection

import android.util.Log
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/**
 * Utility class used for reflection guard for WindowExtensions classes' validation
 */
internal object ReflectionUtils {

    internal fun checkIsPresent(classLoader: () -> Class<*>): Boolean {
        return try {
            classLoader()
            true
        } catch (noClass: ClassNotFoundException) {
            false
        } catch (noDefinition: NoClassDefFoundError) {
            false
        }
    }

    /**
     * Validates the code block normally for reflection. If there are [ClassNotFoundException]
     * or [NoSuchMethodException] thrown, validation will fail.
     * Otherwise will return the validation result from the [block]
     */
    @JvmStatic
    internal fun validateReflection(errorMessage: String? = null, block: () -> Boolean): Boolean {
        return try {
            val result = block()
            if (!result && errorMessage != null) {
                Log.e("ReflectionGuard", errorMessage)
            }
            result
        } catch (noClass: ClassNotFoundException) {
            Log.e("ReflectionGuard", "ClassNotFound: ${errorMessage.orEmpty()}")
            false
        } catch (noMethod: NoSuchMethodException) {
            Log.e("ReflectionGuard", "NoSuchMethod: ${errorMessage.orEmpty()}")
            false
        }
    }

    /**
     * Checks if a method has public modifier
     */
    internal val Method.isPublic: Boolean
        get() {
            return Modifier.isPublic(modifiers)
        }

    /**
     * Checks if a method's return value is type of kotlin [clazz]
     */
    internal fun Method.doesReturn(clazz: KClass<*>): Boolean {
        return doesReturn(clazz.java)
    }

    /**
     * Checks if a method's return value is type of java [clazz]
     */
    internal fun Method.doesReturn(clazz: Class<*>): Boolean {
        return returnType.equals(clazz)
    }

    internal fun validateImplementation(
        implementation: Class<*>,
        requirements: Class<*>,
    ): Boolean {
        return requirements.methods.all {
            validateReflection("${implementation.name}#${it.name} is not valid") {
                val implementedMethod = implementation.getMethod(it.name, *it.parameterTypes)
                implementedMethod.isPublic && implementedMethod.doesReturn(it.returnType)
            }
        }
    }
}
