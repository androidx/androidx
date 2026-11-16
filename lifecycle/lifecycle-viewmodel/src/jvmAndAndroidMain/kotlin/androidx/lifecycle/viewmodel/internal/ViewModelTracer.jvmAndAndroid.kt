/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.internal

import androidx.lifecycle.ViewModelProvider
import androidx.tracing.Tracer
import kotlin.reflect.KClass

internal actual object ViewModelTracer {
    actual inline fun <T> trace(
        name: String,
        modelClass: KClass<*>,
        key: String,
        factory: ViewModelProvider.Factory,
        crossinline block: () -> T,
    ): T =
        Tracer.global.trace(
            category = "androidx.lifecycle.viewmodel",
            name = name,
            metadataBlock = {
                addMetadataEntry("key", key)
                addMetadataEntry("class", modelClass.java.simpleName)
                addMetadataEntry("factory", factory.javaClass.simpleName)
            },
            block = block,
        )
}
