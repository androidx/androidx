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

package androidx.room3.solver

import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XNullability.NONNULL
import androidx.room3.compiler.processing.XNullability.NULLABLE
import androidx.room3.compiler.processing.XNullability.UNKNOWN
import androidx.room3.compiler.processing.XProcessingEnv.Backend
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.isAssignableFromWithNullability
import androidx.room3.processor.Context
import androidx.room3.solver.types.ColumnTypeConverter
import androidx.room3.solver.types.CompositeTypeConverter
import androidx.room3.solver.types.NoOpConverter
import androidx.room3.solver.types.NullSafeTypeConverter
import androidx.room3.solver.types.RequireNotNullTypeConverter
import androidx.room3.solver.types.UpCastTypeConverter
import java.util.PriorityQueue

/**
 * A [ColumnTypeConverterStore] implementation that generates better code when we have the
 * nullability information in types.
 *
 * This [ColumnTypeConverterStore] tries to maintain the nullability of the input/output type when
 * writing into/reading from database. Even though nullability preservation is preferred, it is not
 * strictly required such that it will fall back to the mismatched nullability.
 */
class NullAwareColumnTypeConverterStore(
    context: Context,
    /**
     * Available ColumnTypeConverters. Note that we might synthesize new type converters based on
     * this list.
     */
    columnTypeConverters: List<ColumnTypeConverter>,
    /** List of types that can be saved into db/read from without a converter. */
    private val knownColumnTypes: List<XType>,
) : ColumnTypeConverterStore {
    private val knownColumnTypeNames = knownColumnTypes.map { it.asTypeName() }
    override val columnTypeConverters =
        if (context.processingEnv.backend == Backend.KSP) {
            val processedConverters = columnTypeConverters.toMutableList()
            // create copies for converters that receive non-null values
            columnTypeConverters.forEach { converter ->
                if (converter.from.nullability == NONNULL) {
                    val candidate = NullSafeTypeConverter(delegate = converter)
                    // before we add this null safe converter, make sure there is no other converter
                    // that would already handle the same arguments.
                    val match =
                        processedConverters.any { other ->
                            other.from.isAssignableFromWithNullability(candidate.from) &&
                                candidate.to.isAssignableFromWithNullability(other.to)
                        }
                    if (!match) {
                        processedConverters.add(candidate)
                    }
                }
            }
            processedConverters
        } else {
            columnTypeConverters
        }

    // cache for type converter lookups to avoid traversing all of the list every time we need to
    // find possible converters for a type. Unlike JAVAC, KSP supports equality in its objects so
    // this tends to work rather well.
    private val columnTypeConvertersByFromCache = mutableMapOf<XType, List<ColumnTypeConverter>>()
    private val columnTypeConvertersByToCache = mutableMapOf<XType, List<ColumnTypeConverter>>()

    /**
     * Known column types that are nullable. Used in [getColumnTypesInPreferenceBuckets] to avoid
     * re-partitioning known type lists.
     */
    private val knownNullableColumnTypes by lazy {
        knownColumnTypes.filter { it.nullability == NULLABLE }
    }

    /**
     * Known column types that are non-null or have unknown nullability. Used in
     * [getColumnTypesInPreferenceBuckets] to avoid re-partitioning known type lists.
     */
    private val knownNonNullableColumnTypes by lazy {
        knownColumnTypes.filter { it.nullability != NULLABLE }
    }

    /**
     * Returns a list of lists for the given type, ordered by preference buckets for the given
     * nullability.
     */
    private fun getColumnTypesInPreferenceBuckets(
        nullability: XNullability,
        explicitColumnTypes: List<XType>?,
    ): List<List<XType>> {
        return if (explicitColumnTypes == null) {
            when (nullability) {
                NULLABLE -> {
                    // prioritize nulls
                    listOf(knownNullableColumnTypes, knownNonNullableColumnTypes)
                }
                NONNULL -> {
                    // prioritize non-null
                    listOf(knownNonNullableColumnTypes, knownNullableColumnTypes)
                }
                else -> {
                    // we don't know, YOLO
                    listOf(knownColumnTypes)
                }
            }
        } else {
            when (nullability) {
                UNKNOWN -> listOf(explicitColumnTypes)
                else ->
                    listOf(
                        explicitColumnTypes.filter { it.nullability == nullability },
                        explicitColumnTypes.filter { it.nullability != nullability },
                    )
            }
        }
    }

    override fun findConverterIntoStatement(
        input: XType,
        columnTypes: List<XType>?,
    ): ColumnTypeConverter? {
        getColumnTypesInPreferenceBuckets(
                nullability = input.nullability,
                explicitColumnTypes = columnTypes,
            )
            .forEach { types ->
                findConverterIntoStatementInternal(input = input, columnTypes = types)
                    ?.getOrCreateConverter()
                    ?.let {
                        return it
                    }
            }
        return null
    }

    private fun isColumnType(type: XType): Boolean {
        // compare using type names to handle both null and non-null.
        return knownColumnTypeNames.contains(type.asTypeName())
    }

    private fun findConverterIntoStatementInternal(
        input: XType,
        columnTypes: List<XType>,
    ): ColumnTypeConverterEntry? {
        if (columnTypes.isEmpty()) return null
        val queue =
            ColumnTypeConverterQueue(
                sourceType = input,
                // each converter is keyed on which type they will take us to
                keyType = ColumnTypeConverter::to,
                isKnownColumnType = this::isColumnType,
            )

        while (true) {
            val current = queue.next() ?: break
            val match = columnTypes.any { columnType -> columnType.isSameType(current.type) }
            if (match) {
                return current
            }
            // check for assignable matches but only enqueue them as there might be another shorter
            // path
            columnTypes.forEach { columnType ->
                if (columnType.isAssignableFromWithNullability(current.type)) {
                    queue.maybeEnqueue(
                        prevEntry = current,
                        converter =
                            current.appendConverter(
                                UpCastTypeConverter(
                                    upCastFrom = current.type,
                                    upCastTo = columnType,
                                )
                            ),
                    )
                }
            }
            getAllColumnTypeConvertersFrom(current.type).forEach {
                queue.maybeEnqueue(prevEntry = current, converter = current.appendConverter(it))
            }
        }
        return null
    }

    override fun findConverterFromStatement(
        columnTypes: List<XType>?,
        output: XType,
    ): ColumnTypeConverter? {
        @Suppress("NAME_SHADOWING") // intentional
        val columnTypes = columnTypes ?: knownColumnTypes
        // prefer nullable when reading from database, regardless of the output type
        getColumnTypesInPreferenceBuckets(nullability = NULLABLE, explicitColumnTypes = columnTypes)
            .forEach { types ->
                findConverterFromStatementInternal(columnTypes = types, output = output)?.let {
                    return it.getOrCreateConverter()
                }
            }

        // if type is non-null, try to find nullable and add null check
        return if (output.nullability == NONNULL) {
            findConverterFromStatementInternal(
                    columnTypes = columnTypes,
                    output = output.makeNullable(),
                )
                ?.appendConverter(RequireNotNullTypeConverter(from = output.makeNullable()))
        } else {
            null
        }
    }

    private fun findConverterFromStatementInternal(
        columnTypes: List<XType>,
        output: XType,
    ): ColumnTypeConverterEntry? {
        if (columnTypes.isEmpty()) return null
        val queue =
            ColumnTypeConverterQueue(
                sourceType = output,
                // each converter is keyed on which type they receive as we are doing pathfinding
                // reverse here
                keyType = ColumnTypeConverter::from,
                isKnownColumnType = this::isColumnType,
            )

        while (true) {
            val current = queue.next() ?: break
            val match = columnTypes.any { columnType -> columnType.isSameType(current.type) }
            if (match) {
                return current
            }
            // check for assignable matches but only enqueue them as there might be another shorter
            // path
            columnTypes.forEach { columnType ->
                if (current.type.isAssignableFromWithNullability(columnType)) {
                    queue.maybeEnqueue(
                        prevEntry = current,
                        converter =
                            current.prependConverter(
                                UpCastTypeConverter(
                                    upCastFrom = columnType,
                                    upCastTo = current.type,
                                )
                            ),
                    )
                }
            }
            getAllColumnTypeConvertersTo(current.type).forEach {
                queue.maybeEnqueue(prevEntry = current, converter = current.prependConverter(it))
            }
        }
        return null
    }

    override fun findColumnTypeConverter(input: XType, output: XType): ColumnTypeConverter? {
        findConverterIntoStatementInternal(input = input, columnTypes = listOf(output))?.let {
            return it.getOrCreateConverter()
        }
        // if output is non-null, see if we can find a converter to nullable version and add a
        // null check
        return if (output.nullability == NONNULL) {
            findConverterIntoStatementInternal(
                    input = input,
                    columnTypes = listOf(output.makeNullable()),
                )
                ?.let { converterEntry ->
                    return converterEntry.appendConverter(
                        RequireNotNullTypeConverter(from = output.makeNullable())
                    )
                }
        } else {
            null
        }
    }

    /** Returns all type converters that can receive input type and return into another type. */
    private fun getAllColumnTypeConvertersFrom(input: XType): List<ColumnTypeConverter> {
        // for input, check assignability because it defines whether we can use the function or not.
        return columnTypeConvertersByFromCache.getOrPut(input) {
            // this cache avoids us many assignability checks.
            columnTypeConverters.mapNotNull { converter ->
                when {
                    converter.from.isSameType(input) -> converter
                    converter.from.isAssignableFromWithNullability(input) ->
                        CompositeTypeConverter(
                            conv1 =
                                UpCastTypeConverter(upCastFrom = input, upCastTo = converter.from),
                            conv2 = converter,
                        )
                    else -> null
                }
            }
        }
    }

    /** Returns all type converters that can return the output type. */
    private fun getAllColumnTypeConvertersTo(output: XType): List<ColumnTypeConverter> {
        return columnTypeConvertersByToCache.getOrPut(output) {
            // this cache avoids us many assignability checks.
            columnTypeConverters.mapNotNull { converter ->
                when {
                    converter.to.isSameType(output) -> converter
                    output.isAssignableFromWithNullability(converter.to) ->
                        CompositeTypeConverter(
                            conv1 = converter,
                            conv2 =
                                UpCastTypeConverter(upCastFrom = converter.to, upCastTo = output),
                        )
                    else -> null
                }
            }
        }
    }

    /** Priority queue for the type converter search. */
    private class ColumnTypeConverterQueue(
        sourceType: XType,
        val isKnownColumnType: (XType) -> Boolean,
        val keyType: ColumnTypeConverter.() -> XType,
    ) {
        // using insertion order as the tie breaker for reproducible builds.
        private var insertionOrder = 0

        // map of XType to the converter that includes the path from the source type to the XType.
        private val cheapestEntry = mutableMapOf<XType, ColumnTypeConverterEntry>()
        private val queue = PriorityQueue<ColumnTypeConverterEntry>()

        init {
            val typeConverterEntry =
                ColumnTypeConverterEntry(
                    tieBreakerPriority = insertionOrder++,
                    type = sourceType,
                    converter = null,
                    convertsBetweenDbAndNonDbType = false,
                )
            cheapestEntry[sourceType] = typeConverterEntry
            queue.add(typeConverterEntry)
        }

        fun next(): ColumnTypeConverterEntry? {
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
            prevEntry: ColumnTypeConverterEntry,
            converter: ColumnTypeConverter,
        ): Boolean {
            val keyType = converter.keyType()
            val convertsBetweenDbAndNonDbType =
                isKnownColumnType(converter.from) != isKnownColumnType(converter.to)
            if (prevEntry.convertsBetweenDbAndNonDbType) {
                // if previous entry converted from db type to user type (or vice versa), the new
                // converter must also be converting between db type and non-db type.
                if (!convertsBetweenDbAndNonDbType) {
                    // prev entry already visited a column type, we cannot add any converters that
                    // will visit a non-column type
                    return false
                }
            }
            val existing = cheapestEntry[keyType]
            if (
                existing == null ||
                    (existing.converter != null && existing.converter.cost > converter.cost)
            ) {
                val entry =
                    ColumnTypeConverterEntry(
                        tieBreakerPriority = insertionOrder++,
                        type = keyType,
                        converter = converter,
                        convertsBetweenDbAndNonDbType = convertsBetweenDbAndNonDbType,
                    )
                cheapestEntry[keyType] = entry
                queue.add(entry)
                return true
            }
            return false
        }
    }

    private data class ColumnTypeConverterEntry(
        // when costs are equal, tieBreakerPriority is used
        val tieBreakerPriority: Int,
        val type: XType,
        val converter: ColumnTypeConverter?,
        /**
         * If true, this entry converts between a column type and a non column type. Once a
         * converter entry converts between a column type and user type, it can never go back. This
         * is to ensure that we don't find converters between unrelated user types just because they
         * both convert to the same database type. For instance, both TypeA and TypeB might be
         * convertible to `String` to be persisted, yet, this doesn't mean TypeA can be converted
         * into TypeB.
         */
        val convertsBetweenDbAndNonDbType: Boolean,
    ) : Comparable<ColumnTypeConverterEntry> {
        override fun compareTo(other: ColumnTypeConverterEntry): Int {
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

        fun appendConverter(nextConverter: ColumnTypeConverter): ColumnTypeConverter {
            if (converter == null) {
                return nextConverter
            }
            return CompositeTypeConverter(conv1 = converter, conv2 = nextConverter)
        }

        fun prependConverter(previous: ColumnTypeConverter): ColumnTypeConverter {
            if (converter == null) {
                return previous
            }
            return CompositeTypeConverter(conv1 = previous, conv2 = converter)
        }
    }
}
