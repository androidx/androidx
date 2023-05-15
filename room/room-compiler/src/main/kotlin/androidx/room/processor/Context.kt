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

import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.log.RLog
import androidx.room.parser.expansion.ProjectionExpander
import androidx.room.parser.optimization.RemoveUnusedColumnQueryRewriter
import androidx.room.preconditions.Checks
import androidx.room.processor.cache.Cache
import androidx.room.solver.TypeAdapterStore
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.BuiltInConverterFlags
import androidx.room.vo.Warning
import javax.tools.Diagnostic

class Context private constructor(
    val processingEnv: XProcessingEnv,
    val logger: RLog,
    private val typeConverters: CustomConverterProcessor.ProcessResult,
    private val inheritedAdapterStore: TypeAdapterStore?,
    val cache: Cache,
    private val canRewriteQueriesToDropUnusedColumns: Boolean,
) {
    val checker: Checks = Checks(logger)
    val COMMON_TYPES = CommonTypes(processingEnv)

    /**
     * Checks whether we should use the TypeConverter store that has a specific heuristic for
     * nullability. Defaults to true in KSP, false in javac.
     */
    val useNullAwareConverter: Boolean by lazy {
        BooleanProcessorOptions.USE_NULL_AWARE_CONVERTER.getInputValue(processingEnv)
            ?: (processingEnv.backend == XProcessingEnv.Backend.KSP)
    }

    val typeAdapterStore by lazy {
        if (inheritedAdapterStore != null) {
            TypeAdapterStore.copy(this, inheritedAdapterStore)
        } else {
            TypeAdapterStore.create(
                this, typeConverters.builtInConverterFlags,
                typeConverters.converters
            )
        }
    }

    // set when database and its entities are processed.
    var databaseVerifier: DatabaseVerifier? = null
        private set

    val queryRewriter: QueryRewriter by lazy {
        val verifier = databaseVerifier
        if (verifier == null) {
            QueryRewriter.NoOpRewriter
        } else {
            if (canRewriteQueriesToDropUnusedColumns) {
                RemoveUnusedColumnQueryRewriter
            } else if (BooleanProcessorOptions.EXPAND_PROJECTION.getValue(processingEnv)) {
                ProjectionExpander(
                    tables = verifier.entitiesAndViews
                )
            } else {
                QueryRewriter.NoOpRewriter
            }
        }
    }

    val codeLanguage: CodeLanguage by lazy {
        if (BooleanProcessorOptions.GENERATE_KOTLIN.getValue(processingEnv)) {
            if (processingEnv.backend == XProcessingEnv.Backend.KSP) {
                CodeLanguage.KOTLIN
            } else {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "${BooleanProcessorOptions.GENERATE_KOTLIN.argName} can only be enabled in KSP."
                )
                CodeLanguage.JAVA
            }
        } else {
            CodeLanguage.JAVA
        }
    }

    companion object {
        val ARG_OPTIONS by lazy {
            ProcessorOptions.values().map { it.argName } +
                BooleanProcessorOptions.values().map { it.argName }
        }
    }

    fun attachDatabaseVerifier(databaseVerifier: DatabaseVerifier) {
        check(this.databaseVerifier == null) {
            "database verifier is already set"
        }
        this.databaseVerifier = databaseVerifier
    }

    constructor(processingEnv: XProcessingEnv) : this(
        processingEnv = processingEnv,
        logger = RLog(processingEnv.messager, emptySet(), null),
        typeConverters = CustomConverterProcessor.ProcessResult.EMPTY,
        inheritedAdapterStore = null,
        cache = Cache(
            parent = null,
            converters = LinkedHashSet(),
            suppressedWarnings = emptySet(),
            builtInConverterFlags = BuiltInConverterFlags.DEFAULT
        ),
        canRewriteQueriesToDropUnusedColumns = false
    )

    class CommonTypes(val processingEnv: XProcessingEnv) {
        val VOID: XType by lazy {
            processingEnv.requireType(CommonTypeNames.VOID)
        }
        val STRING: XType by lazy {
            processingEnv.requireType(CommonTypeNames.STRING)
        }
        val READONLY_COLLECTION: XType by lazy {
            processingEnv.requireType(CommonTypeNames.COLLECTION)
        }
        val LIST: XType by lazy {
            processingEnv.requireType(CommonTypeNames.LIST)
        }
        val SET: XType by lazy {
            processingEnv.requireType(CommonTypeNames.SET)
        }
    }

    val schemaInFolderPath by lazy {
        val internalInputFolder =
            processingEnv.options[ProcessorOptions.INTERNAL_SCHEMA_INPUT_FOLDER.argName]
        val legacySchemaFolder =
            processingEnv.options[ProcessorOptions.OPTION_SCHEMA_FOLDER.argName]
        if (!internalInputFolder.isNullOrBlank()) {
            internalInputFolder
        } else if (!legacySchemaFolder.isNullOrBlank()) {
            legacySchemaFolder
        } else {
            null
        }
    }

    val schemaOutFolderPath by lazy {
        val internalOutputFolder =
            processingEnv.options[ProcessorOptions.INTERNAL_SCHEMA_OUTPUT_FOLDER.argName]
        val legacySchemaFolder =
            processingEnv.options[ProcessorOptions.OPTION_SCHEMA_FOLDER.argName]
        if (!internalOutputFolder.isNullOrBlank() && !legacySchemaFolder.isNullOrBlank()) {
            logger.e(ProcessorErrors.INVALID_GRADLE_PLUGIN_AND_SCHEMA_LOCATION_OPTION)
        }
        if (!internalOutputFolder.isNullOrBlank()) {
            internalOutputFolder
        } else if (!legacySchemaFolder.isNullOrBlank()) {
            legacySchemaFolder
        } else {
            null
        }
    }

    fun <T> collectLogs(handler: (Context) -> T): Pair<T, RLog.CollectingMessager> {
        val collector = RLog.CollectingMessager()
        val subContext = Context(
            processingEnv = processingEnv,
            logger = RLog(collector, logger.suppressedWarnings, logger.defaultElement),
            typeConverters = this.typeConverters,
            inheritedAdapterStore = typeAdapterStore,
            cache = cache,
            canRewriteQueriesToDropUnusedColumns = canRewriteQueriesToDropUnusedColumns
        )
        subContext.databaseVerifier = databaseVerifier
        val result = handler(subContext)
        return Pair(result, collector)
    }

    /**
     * Forks the processor context adding suppressed warnings a type converters found in the
     * given [element].
     *
     * @param element the element from which to create the fork.
     * @param forceSuppressedWarnings the warning that will be silenced regardless if they are
     * present or not in the [element].
     * @param forceBuiltInConverters the built-in converter states that will be set regardless of
     * the states found in the [element].
     */
    fun fork(
        element: XElement,
        forceSuppressedWarnings: Set<Warning> = emptySet(),
        forceBuiltInConverters: BuiltInConverterFlags? = null
    ): Context {
        val suppressedWarnings = SuppressWarningProcessor.getSuppressedWarnings(element)
        val processConvertersResult =
            CustomConverterProcessor.findConverters(this, element).let { result ->
                if (forceBuiltInConverters != null) {
                    result.copy(
                        builtInConverterFlags =
                            result.builtInConverterFlags.withNext(forceBuiltInConverters)
                    )
                } else {
                    result
                }
            }
        val subBuiltInConverterFlags = typeConverters.builtInConverterFlags.withNext(
            processConvertersResult.builtInConverterFlags
        )
        val canReUseAdapterStore =
            subBuiltInConverterFlags == typeConverters.builtInConverterFlags &&
                processConvertersResult.classes.isEmpty()
        // order here is important since the sub context should give priority to new converters.
        val subTypeConverters = if (canReUseAdapterStore) {
            this.typeConverters
        } else {
            processConvertersResult + this.typeConverters
        }
        val subSuppressedWarnings =
            forceSuppressedWarnings + suppressedWarnings + logger.suppressedWarnings
        val subCache = Cache(
            parent = cache,
            converters = subTypeConverters.classes,
            suppressedWarnings = subSuppressedWarnings,
            builtInConverterFlags = subBuiltInConverterFlags
        )
        val subCanRemoveUnusedColumns = canRewriteQueriesToDropUnusedColumns ||
            element.hasRemoveUnusedColumnsAnnotation()
        val subContext = Context(
            processingEnv = processingEnv,
            logger = RLog(logger.messager, subSuppressedWarnings, element),
            typeConverters = subTypeConverters,
            inheritedAdapterStore = if (canReUseAdapterStore) typeAdapterStore else null,
            cache = subCache,
            canRewriteQueriesToDropUnusedColumns = subCanRemoveUnusedColumns
        )
        subContext.databaseVerifier = databaseVerifier
        return subContext
    }

    private fun XElement.hasRemoveUnusedColumnsAnnotation(): Boolean {
        return hasAnnotation(RewriteQueriesToDropUnusedColumns::class).also { annotated ->
            if (annotated && BooleanProcessorOptions.EXPAND_PROJECTION.getValue(processingEnv)) {
                logger.w(
                    warning = Warning.EXPAND_PROJECTION_WITH_REMOVE_UNUSED_COLUMNS,
                    element = this,
                    msg = ProcessorErrors.EXPAND_PROJECTION_ALONG_WITH_REMOVE_UNUSED
                )
            }
        }
    }

    fun reportMissingType(typeName: String) {
        logger.e("${RLog.MISSING_TYPE_PREFIX}: Type '$typeName' is not present")
    }

    fun reportMissingTypeReference(containerName: String) {
        logger.e(
            "${RLog.MISSING_TYPE_PREFIX}: Element '$containerName' references a type that is " +
                "not present"
        )
    }

    enum class ProcessorOptions(val argName: String) {
        OPTION_SCHEMA_FOLDER("room.schemaLocation"),
        INTERNAL_SCHEMA_INPUT_FOLDER("room.internal.schemaInput"),
        INTERNAL_SCHEMA_OUTPUT_FOLDER("room.internal.schemaOutput"),
    }

    enum class BooleanProcessorOptions(val argName: String, private val defaultValue: Boolean) {
        INCREMENTAL("room.incremental", defaultValue = true),
        EXPAND_PROJECTION("room.expandProjection", defaultValue = false),
        USE_NULL_AWARE_CONVERTER("room.useNullAwareTypeAnalysis", defaultValue = false),
        GENERATE_KOTLIN("room.generateKotlin", defaultValue = false),
        EXPORT_SCHEMA_RESOURCE("room.exportSchemaResource", defaultValue = false);

        /**
         * Returns the value of this option passed through the [XProcessingEnv]. If the value
         * is null or blank, it returns the default value instead.
         */
        fun getValue(processingEnv: XProcessingEnv): Boolean {
            return getInputValue(processingEnv) ?: defaultValue
        }

        fun getValue(options: Map<String, String>): Boolean {
            return getInputValue(options) ?: defaultValue
        }

        fun getInputValue(processingEnv: XProcessingEnv): Boolean? {
            return getInputValue(processingEnv.options)
        }

        private fun getInputValue(options: Map<String, String>): Boolean? {
            return options[argName]?.takeIf {
                it.isNotBlank()
            }?.toBoolean()
        }
    }
}
