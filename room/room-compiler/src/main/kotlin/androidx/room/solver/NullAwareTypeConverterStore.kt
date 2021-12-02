/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.solver

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XNullability.NONNULL
import androidx.room.compiler.processing.XNullability.NULLABLE
import androidx.room.compiler.processing.XNullability.UNKNOWN
import androidx.room.compiler.processing.XProcessingEnv.Backend
import androidx.room.compiler.processing.XType
import androidx.room.processor.Context
import androidx.room.solver.types.CompositeTypeConverter
import androidx.room.solver.types.NoOpConverter
import androidx.room.solver.types.NullSafeTypeConverter
import androidx.room.solver.types.RequireNotNullTypeConverter
import androidx.room.solver.types.TypeConverter
import androidx.room.solver.types.UpCastTypeConverter
import java.util.PriorityQueue

/**
 * A [TypeConverterStore] implementation that generates better code when we have the nullability
 * information in types. It is enabled by default only in KSP backend but it can also be turned
 * on via the [Context.BooleanProcessorOptions.USE_NULL_AWARE_CONVERTER] flag.
 *
 * This [TypeConverterStore] tries to maintain the nullability of the input/output type
 * when writing into/reading from database. Even though nullability preservation is preferred, it is
 * not strictly required such that it will fall back to the mismatched nullability.
 */
class NullAwareTypeConverterStore(
    context: Context,
    /**
     * Available TypeConverters. Note that we might synthesize new type converters based on this
     * list.
     */
    typeConverters: List<TypeConverter>,
    /**
     * List of types that can be saved into db/read from without a converter.
     */
    private val knownColumnTypes: List<XType>
) : TypeConverterStore {
    override val typeConverters = if (context.processingEnv.backend == Backend.KSP) {
        val processedConverters = typeConverters.toMutableList()
        // create copies for converters that receive non-null values
        typeConverters.forEach { converter ->
            if (converter.from.nullability == NONNULL) {
                val candidate = NullSafeTypeConverter(delegate = converter)
                // before we add this null safe converter, make sure there is no other converter
                // that would already handle the same arguments.
                val match = processedConverters.any { other ->
                    other.from.isAssignableFrom(candidate.from) &&
                        candidate.to.isAssignableFrom(other.to)
                }
                if (!match) {
                    processedConverters.add(candidate)
                }
            }
        }
        processedConverters
    } else {
        typeConverters
    }

    // cache for type converter lookups to avoid traversing all of the list every time we need to
    // find possible converters for a type. Unlike JAVAC, KSP supports equality in its objects so
    // this tends to work rather well.
    private val typeConvertersByFromCache = mutableMapOf<XType, List<TypeConverter>>()
    private val typeConvertersByToCache = mutableMapOf<XType, List<TypeConverter>>()

    /**
     * Known column types that are nullable.
     * Used in [getColumnTypesInPreferenceBuckets] to avoid re-partitioning known type lists.
     */
    private val knownNullableColumnTypes by lazy {
        knownColumnTypes.filter { it.nullability == NULLABLE }
    }

    /**
     * Known column types that are non-null or have unknown nullability.
     * Used in [getColumnTypesInPreferenceBuckets] to avoid re-partitioning known type lists.
     */
    private val knownNonNullableColumnTypes by lazy {
        knownColumnTypes.filter { it.nullability != NULLABLE }
    }

    /**
     * Returns a list of lists for the given type, ordered by preference buckets for
     * the given nullability.
     */
    private fun getColumnTypesInPreferenceBuckets(
        nullability: XNullability,
        explicitColumnTypes: List<XType>?
    ): List<List<XType>> {
        return if (explicitColumnTypes == null) {
            when (nullability) {
                NULLABLE -> {
                    // prioritize nulls
                    listOf(
                        knownNullableColumnTypes,
                        knownNonNullableColumnTypes
                    )
                }
                NONNULL -> {
                    // prioritize non-null
                    listOf(
                        knownNonNullableColumnTypes,
                        knownNullableColumnTypes
                    )
                }
                else -> {
                    // we don't know, YOLO
                    listOf(knownColumnTypes)
                }
            }
        } else {
            when (nullability) {
                UNKNOWN -> listOf(explicitColumnTypes)
                else -> listOf(
                    explicitColumnTypes.filter { it.nullability == nullability },
                    explicitColumnTypes.filter { it.nullability != nullability }
                )
            }
        }
    }

    override fun findConverterIntoStatement(
        input: XType,
        columnTypes: List<XType>?
    ): TypeConverter? {
        getColumnTypesInPreferenceBuckets(
            nullability = input.nullability,
            explicitColumnTypes = columnTypes
        ).forEach { types ->
            findConverterIntoStatementInternal(
                input = input,
                columnTypes = types
            )?.getOrCreateConverter()?.let {
                return it
            }
        }
        return null
    }

    private fun findConverterIntoStatementInternal(
        input: XType,
        columnTypes: List<XType>
    ): TypeConverterEntry? {
        if (columnTypes.isEmpty()) return null
        val queue = TypeConverterQueue(
            sourceType = input,
            // each converter is keyed on which type they will take us to
            keyType = TypeConverter::to
        )

        while (true) {
            val current = queue.next() ?: break
            val match = columnTypes.any { columnType ->
                columnType.isSameType(current.type)
            }
            if (match) {
                return current
            }
            // check for assignable matches but only enqueue them as there might be another shorter
            // path
            columnTypes.forEach { columnType ->
                if (columnType.isAssignableFrom(current.type)) {
                    queue.maybeEnqueue(
                        current.appendConverter(
                            UpCastTypeConverter(
                                upCastFrom = current.type,
                                upCastTo = columnType
                            )
                        )
                    )
                }
            }
            getAllTypeConvertersFrom(current.type).forEach {
                queue.maybeEnqueue(current.appendConverter(it))
            }
        }
        return null
    }

    override fun findConverterFromCursor(
        columnTypes: List<XType>?,
        output: XType
    ): TypeConverter? {
        @Suppress("NAME_SHADOWING") // intentional
        val columnTypes = columnTypes ?: knownColumnTypes
        // prefer nullable when reading from database, regardless of the output type
        getColumnTypesInPreferenceBuckets(
            nullability = NULLABLE,
            explicitColumnTypes = columnTypes
        ).forEach { types ->
            findConverterFromCursorInternal(
                columnTypes = types,
                output = output
            )?.let {
                return it.getOrCreateConverter()
            }
        }

        // if type is non-null, try to find nullable and add null check
        return if (output.nullability == NONNULL) {
            findConverterFromCursorInternal(
                columnTypes = columnTypes,
                output = output.makeNullable()
            )?.appendConverter(
                RequireNotNullTypeConverter(
                    from = output.makeNullable()
                )
            )
        } else {
            null
        }
    }

    private fun findConverterFromCursorInternal(
        columnTypes: List<XType>,
        output: XType
    ): TypeConverterEntry? {
        if (columnTypes.isEmpty()) return null
        val queue = TypeConverterQueue(
            sourceType = output,
            // each converter is keyed on which type they receive as we are doing pathfinding
            // reverse here
            keyType = TypeConverter::from
        )

        while (true) {
            val current = queue.next() ?: break
            val match = columnTypes.any { columnType ->
                columnType.isSameType(current.type)
            }
            if (match) {
                return current
            }
            // check for assignable matches but only enqueue them as there might be another shorter
            // path
            columnTypes.forEach { columnType ->
                if (current.type.isAssignableFrom(columnType)) {
                    queue.maybeEnqueue(
                        current.prependConverter(
                            UpCastTypeConverter(
                                upCastFrom = columnType,
                                upCastTo = current.type
                            )
                        )
                    )
                }
            }
            getAllTypeConvertersTo(current.type).forEach {
                queue.maybeEnqueue(current.prependConverter(it))
            }
        }
        return null
    }

    override fun findTypeConverter(input: XType, output: XType): TypeConverter? {
        findConverterIntoStatementInternal(
            input = input,
            columnTypes = listOf(output)
        )?.let {
            return it.getOrCreateConverter()
        }
        // if output is non-null, see if we can find a converter to nullable version and add a
        // null check
        return if (output.nullability == NONNULL) {
            findConverterIntoStatementInternal(
                input = input,
                columnTypes = listOf(output.makeNullable())
            )?.let { converterEntry ->
                return converterEntry.appendConverter(
                    RequireNotNullTypeConverter(
                        from = output.makeNullable()
                    )
                )
            }
        } else {
            null
        }
    }

    /**
     * Returns all type converters that can receive input type and return into another type.
     */
    private fun getAllTypeConvertersFrom(
        input: XType
    ): List<TypeConverter> {
        // for input, check assignability because it defines whether we can use the method or not.
        return typeConvertersByFromCache.getOrPut(input) {
            // this cache avoids us many assignability checks.
            typeConverters.mapNotNull { converter ->
                when {
                    converter.from.isSameType(input) -> converter
                    converter.from.isAssignableFrom(input) -> CompositeTypeConverter(
                        conv1 = UpCastTypeConverter(
                            upCastFrom = input,
                            upCastTo = converter.from
                        ),
                        conv2 = converter
                    )
                    else -> null
                }
            }
        }
    }

    /**
     * Returns all type converters that can return the output type.
     */
    private fun getAllTypeConvertersTo(
        output: XType
    ): List<TypeConverter> {
        return typeConvertersByToCache.getOrPut(output) {
            // this cache avoids us many assignability checks.
            typeConverters.mapNotNull { converter ->
                when {
                    converter.to.isSameType(output) -> converter
                    output.isAssignableFrom(converter.to) -> CompositeTypeConverter(
                        conv1 = converter,
                        conv2 = UpCastTypeConverter(
                            upCastFrom = converter.to,
                            upCastTo = output
                        )
                    )
                    else -> null
                }
            }
        }
    }

    /**
     * Priority queue for the type converter search.
     */
    private class TypeConverterQueue(
        sourceType: XType,
        val keyType: TypeConverter.() -> XType
    ) {
        // using insertion order as the tie breaker for reproducible builds.
        private var insertionOrder = 0

        // map of XType to the converter that includes the path from the source type to the XType.
        private val cheapestEntry = mutableMapOf<XType, TypeConverterEntry>()
        private val queue = PriorityQueue<TypeConverterEntry>()

        init {
            val typeConverterEntry = TypeConverterEntry(
                tieBreakerPriority = insertionOrder++,
                type = sourceType,
                converter = null
            )
            cheapestEntry[sourceType] = typeConverterEntry
            queue.add(typeConverterEntry)
        }

        fun next(): TypeConverterEntry? {
            while (queue.isNotEmpty()) {
                val entry = queue.remove()
                // check if we processed this type as there is no reason to process it again
                if (cheapestEntry[entry.type] !== entry) {
                    continue
                }
                return entry
            }
            return null
        }

        /**
         * Enqueues the given [converter] if its target type (defined by [keyType]) is not visited
         * or visited with a more expensive converter.
         */
        fun maybeEnqueue(
            converter: TypeConverter
        ): Boolean {
            val keyType = converter.keyType()
            val existing = cheapestEntry[keyType]
            if (existing == null ||
                (existing.converter != null && existing.converter.cost > converter.cost)
            ) {
                val entry = TypeConverterEntry(insertionOrder++, keyType, converter)
                cheapestEntry[keyType] = entry
                queue.add(entry)
                return true
            }
            return false
        }
    }

    private data class TypeConverterEntry(
        // when costs are equal, tieBreakerPriority is used
        val tieBreakerPriority: Int,
        val type: XType,
        val converter: TypeConverter?
    ) : Comparable<TypeConverterEntry> {
        override fun compareTo(other: TypeConverterEntry): Int {
            if (converter == null) {
                if (other.converter != null) {
                    return -1
                }
            } else if (other.converter == null) {
                return 1
            } else {
                val costCmp = converter.cost.compareTo(other.converter.cost)
                if (costCmp != 0) {
                    return costCmp
                }
            }
            return tieBreakerPriority.compareTo(other.tieBreakerPriority)
        }

        fun getOrCreateConverter() = converter ?: NoOpConverter(type)

        fun appendConverter(nextConverter: TypeConverter): TypeConverter {
            if (converter == null) {
                return nextConverter
            }
            return CompositeTypeConverter(
                conv1 = converter,
                conv2 = nextConverter
            )
        }

        fun prependConverter(previous: TypeConverter): TypeConverter {
            if (converter == null) {
                return previous
            }
            return CompositeTypeConverter(
                conv1 = previous,
                conv2 = converter
            )
        }
    }
}