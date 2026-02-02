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

package androidx.baselineprofile.gradle.utils

import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

internal inline fun <reified T : Task?> TaskContainer.maybeRegister(
    vararg nameParts: String,
    noinline configureBlock: ((T) -> (Unit))? = null
): TaskProvider<T> {
    val name = camelCase(*nameParts)
    return try {
        val task = named(name, T::class.java)
        if (configureBlock != null) task.configure(configureBlock)
        task
    } catch (e: UnknownTaskException) {
        register(name, T::class.java, configureBlock)
    }
}

internal inline fun <reified T : Task?> TaskContainer.namedOrNull(
    vararg nameParts: String,
    noinline configureBlock: ((T) -> (Unit))? = null
): TaskProvider<T>? {
    val name = camelCase(*nameParts)
    return try {
        val task = named(name, T::class.java)
        if (configureBlock != null) task.configure(configureBlock)
        task
    } catch (e: UnknownTaskException) {
        return null
    }
}
