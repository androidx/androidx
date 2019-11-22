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

package androidx.ui.tooling.preview

import java.lang.reflect.Modifier

internal fun invokeComposableViaReflection(className: String, methodName: String) {
    try {
        val composableClass = Class.forName(className)
        val method = composableClass.getDeclaredMethod(methodName)
        method.isAccessible = true

        if (Modifier.isStatic(method.modifiers)) {
            // This is a top level or static method
            method.invoke(null)
        } else {
            // The method is part of a class. We try to instantiate the class with an empty
            // constructor.
            val instance = composableClass.getConstructor().newInstance()
            method.invoke(instance)
        }
    } catch (e: ReflectiveOperationException) {
        throw ClassNotFoundException("Composable Method not found", e)
    }
}