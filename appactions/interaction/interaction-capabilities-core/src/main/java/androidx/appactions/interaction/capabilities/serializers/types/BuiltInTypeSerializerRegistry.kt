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

package androidx.appactions.interaction.capabilities.serializers.types

import androidx.appactions.builtintypes.types.Thing
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger.LogLevel.DEBUG
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger.LogLevel.INFO
import androidx.appactions.interaction.capabilities.core.impl.utils.LoggerInternal
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.LinkedList
import java.util.Queue

/** A global singleton to avoid reflection upon every instance creation. */
val builtInTypeSerializerRegistry: BuiltInTypeSerializerRegistry by lazy {
    BuiltInTypeSerializerRegistry(
        serializerRegistryClassNames =
            listOf(
                "androidx.appactions.interaction.capabilities.serializers.types.AllCoreSerializers",
            ),
        ::getClassByNameOrNull)
}

private const val LOG_TAG = "BuiltInTypeSerializerRegistry"

/**
 * A registry of [BuiltInTypeSerializer]s that allows getting serializers by type.
 *
 * Computes the entire set of serializers using reflection upon construction. This can be relatively
 * compute expensive.
 *
 * This class is thread-safe.
 *
 * @param serializerRegistryClassNames The canonical class names of the `All*Serializers` objects
 *   e.g. [androidx.appactions.interaction.capabilities.serializers.types.AllCoreSerializers]. These
 *   objects are expected to have top-level, zero param, static methods that return instances of
 *   [BuiltInTypeSerializer].
 * @param getClassOrNull Functor that returns a class ref given its canonical name, or null.
 */
@Suppress("BanUncheckedReflection")
class BuiltInTypeSerializerRegistry(
    serializerRegistryClassNames: List<String>,
    getClassOrNull: (canonicalName: String) -> Class<*>?
) {
    // Person::class.java -> PersonSerializer, PersonImpl::class.java -> PersonSerializer
    private val classToSerializer: MutableMap<Class<*>, BuiltInTypeSerializer<*>> =
        Collections.synchronizedMap(mutableMapOf())

    init {
        // Need to use reflection to dynamically access serializers based on type-name. These
        // serializers may be coming from a downstream artifact and are not available at
        // compile-time. This also aids in a binary-bloat optimization where all serializers start
        // off a dead code and then we instruct proguard to only retain the serializers for types
        // that are explicitly referenced and prune others.
        val serializers =
            serializerRegistryClassNames
                // object AllProductivitySerializers
                .mapNotNull { className -> getClassOrNull(className) }
                .flatMap { cls -> cls.methods.toList() }
                // @JvmStatic fun getPersonSerializer(): PersonSerializer
                .filter { it.isStatic && it.hasNoParams && it.returnsSomeSerializer }
                .map { it.invoke(null) as BuiltInTypeSerializer<*> }

        for (serializer in serializers) {
            classToSerializer[serializer.classRef] = serializer
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Thing> getSerializer(instance: T): BuiltInTypeSerializer<T>? {
        val instanceClass = instance::class.java
        val serializer = classToSerializer[instanceClass]
        if (serializer != null) return serializer as BuiltInTypeSerializer<T>

        logDebug(instanceClass, "No serializer found. Moving up the class hierarchy")

        // instance is likely not a Built-in Type (interface) itself but a concrete impl
        // e.g. PersonImpl. Go up the type hierarchy and try to find the first Built-in Type
        // interface that we have a serializer for.
        for ((i, classes) in instanceClass.traverseUpInLevelOrder().withIndex()) {
            logDebug(instanceClass, "Searching for serializers ${i + 1} levels above")
            val serializersAtThisLevel = classes.mapNotNull { cls -> classToSerializer[cls] }
            return when (serializersAtThisLevel.size) {
                0 -> continue
                // Match!
                1 -> {
                    val matchedSerializer = serializersAtThisLevel.first()
                    // Cache for next time so we don't have to traverse up the hierarchy then
                    classToSerializer.putIfAbsent(instanceClass, matchedSerializer)
                    logDebug(instanceClass, "Returning '${matchedSerializer.typeName}' serializer")
                    matchedSerializer as BuiltInTypeSerializer<T>
                }
                else -> {
                    val typeNames = serializersAtThisLevel.map { it.typeName }
                    logInfo(
                        instanceClass,
                        "Found multiple serializers for $typeNames. There is ambiguity " +
                            "in picking one. This can happen because of bad proguard configs or " +
                            "if the user manually inherited from 2+ Built-in Type interfaces. " +
                            "Bailing out and returning null.")
                    null
                }
            }
        }

        logInfo(instanceClass, "No serializer found. Returning null.")
        return null
    }

    private val Method.isStatic: Boolean
        get() = Modifier.isStatic(modifiers)

    private val Method.hasNoParams: Boolean
        get() = parameterTypes.isEmpty()

    private val Method.returnsSomeSerializer: Boolean
        get() = BuiltInTypeSerializer::class.java.isAssignableFrom(returnType)

    /**
     * Yields all the ancestor types in levels. For example, given:
     * ```
     *    GreatGrandparent
     *           ↑
     *      Grandparent
     *        ↗    ↖
     *  ParentA     ParentB
     *        ↖     ↗
     *         Child
     * ```
     *
     * `Child::class.java.traverseUpInLevelOrder()` yields in this order:
     * ```
     * [ParentA, ParentB],
     * [Grandparent],
     * [GreatGrandparent]
     * ```
     *
     * Duplicates are ignored guaranteeing the [Sequence] terminates.
     */
    private fun Class<*>.traverseUpInLevelOrder(): Sequence<Set<Class<*>>> = sequence {
        // Level-order traversal (very similar to BFS)
        val seen: MutableSet<Class<*>> = mutableSetOf(this@traverseUpInLevelOrder)
        val queue: Queue<Class<*>> = LinkedList()

        if (superclass != null) queue += superclass
        queue += interfaces

        while (queue.isNotEmpty()) {
            val classesInCurrentLevel = mutableSetOf<Class<*>>()
            val currentLevelSize = queue.size
            repeat(currentLevelSize) {
                val cls = queue.remove()
                seen += cls
                classesInCurrentLevel += cls

                // Add classes from the next level, i.e. parents, to the queue
                if (cls.superclass != null && cls.superclass !in seen) queue += cls.superclass
                queue += cls.interfaces.filter { it !in seen }
            }
            yield(classesInCurrentLevel)
        }
    }

    private fun logDebug(instanceClass: Class<out Thing>, message: String) {
        LoggerInternal.log(
            DEBUG, LOG_TAG, "While finding serializer for class: $instanceClass - $message")
    }

    private fun logInfo(instanceClass: Class<out Thing>, message: String) {
        LoggerInternal.log(
            INFO, LOG_TAG, "While finding serializer for class: $instanceClass - $message")
    }
}

private fun getClassByNameOrNull(canonicalName: String): Class<*>? {
    return try {
        Class.forName(canonicalName)
    } catch (_: ClassNotFoundException) {
        null
    }
}
