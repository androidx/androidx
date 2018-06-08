/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.log.RLog
import androidx.room.preconditions.Checks
import androidx.room.processor.cache.Cache
import androidx.room.solver.TypeAdapterStore
import androidx.room.verifier.DatabaseVerifier
import java.io.File
import java.util.LinkedHashSet
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

class Context private constructor(
        val processingEnv: ProcessingEnvironment,
        val logger: RLog,
        private val typeConverters: CustomConverterProcessor.ProcessResult,
        private val inheritedAdapterStore: TypeAdapterStore?,
        val cache: Cache) {
    val checker: Checks = Checks(logger)
    val COMMON_TYPES: Context.CommonTypes = Context.CommonTypes(processingEnv)

    val typeAdapterStore by lazy {
        if (inheritedAdapterStore != null) {
            TypeAdapterStore.copy(this, inheritedAdapterStore)
        } else {
            TypeAdapterStore.create(this, typeConverters.converters)
        }
    }

    // set when database and its entities are processed.
    var databaseVerifier: DatabaseVerifier? = null

    companion object {
        val ARG_OPTIONS by lazy {
            ProcessorOptions.values().map { it.argName }
        }
    }

    constructor(processingEnv: ProcessingEnvironment) : this(
            processingEnv = processingEnv,
            logger = RLog(RLog.ProcessingEnvMessager(processingEnv), emptySet(), null),
            typeConverters = CustomConverterProcessor.ProcessResult.EMPTY,
            inheritedAdapterStore = null,
            cache = Cache(null, LinkedHashSet(), emptySet()))

    class CommonTypes(val processingEnv: ProcessingEnvironment) {
        val STRING: TypeMirror by lazy {
            processingEnv.elementUtils.getTypeElement("java.lang.String").asType()
        }
    }

    val schemaOutFolder by lazy {
        val arg = processingEnv.options[ProcessorOptions.OPTION_SCHEMA_FOLDER.argName]
        if (arg?.isNotEmpty() ?: false) {
            File(arg)
        } else {
            null
        }
    }

    fun <T> collectLogs(handler: (Context) -> T): Pair<T, RLog.CollectingMessager> {
        val collector = RLog.CollectingMessager()
        val subContext = Context(processingEnv = processingEnv,
                logger = RLog(collector, logger.suppressedWarnings, logger.defaultElement),
                typeConverters = this.typeConverters,
                inheritedAdapterStore = typeAdapterStore,
                cache = cache)
        subContext.databaseVerifier = databaseVerifier
        val result = handler(subContext)
        return Pair(result, collector)
    }

    fun fork(element: Element): Context {
        val suppressedWarnings = SuppressWarningProcessor.getSuppressedWarnings(element)
        val processConvertersResult = CustomConverterProcessor.findConverters(this, element)
        val canReUseAdapterStore = processConvertersResult.classes.isEmpty()
        // order here is important since the sub context should give priority to new converters.
        val subTypeConverters = if (canReUseAdapterStore) {
            this.typeConverters
        } else {
            processConvertersResult + this.typeConverters
        }
        val subSuppressedWarnings = suppressedWarnings + logger.suppressedWarnings
        val subCache = Cache(cache, subTypeConverters.classes, subSuppressedWarnings)
        val subContext = Context(
                processingEnv = processingEnv,
                logger = RLog(logger.messager, subSuppressedWarnings, element),
                typeConverters = subTypeConverters,
                inheritedAdapterStore = if (canReUseAdapterStore) typeAdapterStore else null,
                cache = subCache)
        subContext.databaseVerifier = databaseVerifier
        return subContext
    }

    enum class ProcessorOptions(val argName: String) {
        OPTION_SCHEMA_FOLDER("room.schemaLocation")
    }
}
