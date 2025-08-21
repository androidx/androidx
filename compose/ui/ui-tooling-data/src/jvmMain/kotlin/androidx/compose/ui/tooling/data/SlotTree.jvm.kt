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

@file:JvmName("SlotTreeKt")

package androidx.compose.ui.tooling.data

import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.LocationSourceInformation
import androidx.compose.runtime.tooling.ParameterSourceInformation
import androidx.compose.runtime.tooling.parseSourceInformation
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntRect
import java.lang.reflect.Field
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A group in the slot table. Represents either a call or an emitted node. */
@UiToolingDataApi
sealed class Group(
    /** The key is the key generated for the group */
    val key: Any?,

    /** The name of the function called, if provided */
    val name: String?,

    /** The source location that produce the group if it can be determined */
    val location: SourceLocation?,

    /**
     * An optional value that identifies a Group independently of movement caused by recompositions.
     */
    val identity: Any?,

    /** The bounding layout box for the group. */
    val box: IntRect,

    /** Any data that was stored in the slot table for the group */
    val data: Collection<Any?>,

    /** The child groups of this group */
    val children: Collection<Group>,

    /** True if the group is for an inline function call */
    val isInline: Boolean,
) {
    /** Modifier information for the Group, or empty list if there isn't any. */
    open val modifierInfo: List<ModifierInfo>
        get() = emptyList()

    /** Parameter information for Groups that represent calls */
    open val parameters: List<ParameterInformation>
        get() = emptyList()
}

@UiToolingDataApi
@Suppress("DataClassDefinition")
data class ParameterInformation(
    val name: String,
    val value: Any?,
    val fromDefault: Boolean,
    val static: Boolean,
    val compared: Boolean,
    val inlineClass: String?,
    val stable: Boolean,
)

/** Source location of the call that produced the call group. */
@UiToolingDataApi
@Suppress("DataClassDefinition")
data class SourceLocation(
    /** A 0 offset line number of the source location. */
    val lineNumber: Int,

    /**
     * Offset into the file. The offset is calculated as the number of UTF-16 code units from the
     * beginning of the file to the first UTF-16 code unit of the call that produced the group.
     */
    val offset: Int,

    /**
     * The length of the source code. The length is calculated as the number of UTF-16 code units
     * that that make up the call expression.
     */
    val length: Int,

    /**
     * The file name (without path information) of the source file that contains the call that
     * produced the group. A source file names are not guaranteed to be unique, [packageHash] is
     * included to help disambiguate files with duplicate names.
     */
    val sourceFile: String?,

    /**
     * A hash code of the package name of the file. This hash is calculated by,
     *
     * `packageName.fold(0) { hash, current -> hash * 31 + current.toInt() }?.absoluteValue`
     *
     * where the package name is the dotted name of the package. This can be used to disambiguate
     * which file is referenced by [sourceFile]. This number is -1 if there was no package hash
     * information generated such as when the file does not contain a package declaration.
     */
    val packageHash: Int,
)

/** A group that represents the invocation of a component */
@UiToolingDataApi
class CallGroup(
    key: Any?,
    name: String?,
    box: IntRect,
    location: SourceLocation?,
    identity: Any?,
    override val parameters: List<ParameterInformation>,
    data: Collection<Any?>,
    children: Collection<Group>,
    isInline: Boolean,
) : Group(key, name, location, identity, box, data, children, isInline)

/** A group that represents an emitted node */
@UiToolingDataApi
class NodeGroup(
    key: Any?,

    /** An emitted node */
    val node: Any,
    box: IntRect,
    data: Collection<Any?>,
    override val modifierInfo: List<ModifierInfo>,
    children: Collection<Group>,
) : Group(key, null, null, null, box, data, children, false)

@UiToolingDataApi
private object EmptyGroup :
    Group(
        key = null,
        name = null,
        location = null,
        identity = null,
        box = emptyBox,
        data = emptyList(),
        children = emptyList(),
        isInline = false,
    )

/** A key that has being joined together to form one key. */
@UiToolingDataApi
@Suppress("DataClassDefinition")
data class JoinedKey(val left: Any?, val right: Any?)

internal val emptyBox = IntRect(0, 0, 0, 0)

@OptIn(ComposeToolingApi::class, UiToolingDataApi::class)
private class SourceInformationContext(
    val name: String?,
    val sourceFile: String?,
    val packageHash: Int,
    val locations: List<LocationSourceInformation>,
    val repeatOffset: Int,
    val parameters: List<ParameterSourceInformation>?,
    val isCall: Boolean,
    val isInline: Boolean,
) {
    private var nextLocation = 0

    fun nextSourceLocation(): SourceLocation? {
        if (nextLocation >= locations.size && repeatOffset >= 0) {
            nextLocation = repeatOffset
        }
        if (nextLocation < locations.size) {
            val location = locations[nextLocation++]
            return SourceLocation(
                location.lineNumber,
                location.offset,
                location.length,
                sourceFile,
                packageHash,
            )
        }
        return null
    }

    fun sourceLocation(callIndex: Int, parentContext: SourceInformationContext?): SourceLocation? {
        var locationIndex = callIndex
        if (locationIndex >= locations.size && repeatOffset >= 0 && repeatOffset < locations.size) {
            locationIndex =
                (callIndex - repeatOffset) % (locations.size - repeatOffset) + repeatOffset
        }
        if (locationIndex < locations.size) {
            val location = locations[locationIndex]
            return SourceLocation(
                location.lineNumber,
                location.offset,
                location.length,
                sourceFile ?: parentContext?.sourceFile,
                (if (sourceFile == null) parentContext?.packageHash else packageHash) ?: -1,
            )
        }
        return null
    }
}

@OptIn(ComposeToolingApi::class)
private fun sourceInformationContextOf(
    information: String,
    parent: SourceInformationContext? = null,
): SourceInformationContext? {
    val parsedInfo = parseSourceInformation(information) ?: return null

    return SourceInformationContext(
        name = parsedInfo.functionName,
        sourceFile = parsedInfo.sourceFile ?: parent?.sourceFile,
        packageHash =
            if (parsedInfo.sourceFile != null) {
                parsedInfo.packageHash?.toIntOrNull(36)
            } else {
                parent?.packageHash
            } ?: -1,
        locations = parsedInfo.locations,
        repeatOffset = parsedInfo.locations.indexOfFirst { it.isRepeatable },
        parameters = parsedInfo.parameters,
        isCall = parsedInfo.isCall,
        isInline = parsedInfo.isInline,
    )
}

/** Iterate the slot table and extract a group tree that corresponds to the content of the table. */
@UiToolingDataApi
private fun CompositionGroup.getGroup(parentContext: SourceInformationContext?): Group {
    val key = key
    val context = sourceInfo?.let { sourceInformationContextOf(it, parentContext) }
    val node = node
    val data = mutableListOf<Any?>()
    val children = mutableListOf<Group>()
    data.addAll(this.data)
    for (child in compositionGroups) children.add(child.getGroup(context))

    val modifierInfo =
        if (node is LayoutInfo) {
            node.getModifierInfo()
        } else {
            emptyList()
        }

    // Calculate bounding box
    val box =
        when (node) {
            is LayoutInfo -> boundsOfLayoutNode(node)
            else ->
                if (children.isEmpty()) emptyBox
                else children.map { g -> g.box }.reduce { acc, box -> box.union(acc) }
        }
    val location =
        if (context?.isCall == true) {
            parentContext?.nextSourceLocation()
        } else {
            null
        }
    return if (node != null) NodeGroup(key, node, box, data, modifierInfo, children)
    else
        CallGroup(
            key,
            context?.name,
            box,
            location,
            identity =
                if (
                    !context?.name.isNullOrEmpty() &&
                        (box.bottom - box.top > 0 || box.right - box.left > 0)
                ) {
                    this.identity
                } else {
                    null
                },
            extractParameterInfo(data, context),
            data,
            children,
            context?.isInline == true,
        )
}

private fun boundsOfLayoutNode(node: LayoutInfo): IntRect {
    val coordinates = node.coordinates
    if (!node.isAttached || !coordinates.isAttached) {
        return IntRect(left = 0, top = 0, right = node.width, bottom = node.height)
    }
    val position = coordinates.positionInWindow()
    if (!position.isValid()) {
        return IntRect(left = 0, top = 0, right = node.width, bottom = node.height)
    }
    val size = coordinates.size
    val left = position.x.roundToInt()
    val top = position.y.roundToInt()
    val right = left + size.width
    val bottom = top + size.height
    return IntRect(left = left, top = top, right = right, bottom = bottom)
}

@UiToolingDataApi
private class CompositionCallStack<T>(
    private val factory: (CompositionGroup, SourceContext, List<T>) -> T?,
    private val contexts: MutableMap<String, Any?>,
) : SourceContext {
    private val stack = ArrayDeque<CompositionGroup>()
    private var currentCallIndex = 0

    fun convert(group: CompositionGroup, callIndex: Int, out: MutableList<T>): IntRect {
        val children = mutableListOf<T>()
        var box = emptyBox
        push(group)
        var childCallIndex = 0
        group.compositionGroups.forEach { child ->
            box = box.union(convert(child, childCallIndex, children))
            if (isCall(child)) {
                childCallIndex++
            }
        }
        box = (group.node as? LayoutInfo)?.let { boundsOfLayoutNode(it) } ?: box
        currentCallIndex = callIndex
        bounds = box
        factory(group, this, children)?.let { out.add(it) }
        pop()
        return box
    }

    override val name: String?
        get() {
            val info = current.sourceInfo ?: return null
            val startIndex =
                when {
                    info.startsWith("CC(") -> 3
                    info.startsWith("C(") -> 2
                    else -> return null
                }
            val endIndex = info.indexOf(')')
            return if (endIndex > 2) info.substring(startIndex, endIndex) else null
        }

    override val isInline: Boolean
        get() = current.sourceInfo?.startsWith("CC") == true

    override var bounds: IntRect = emptyBox
        private set

    override val location: SourceLocation?
        get() {
            val context = parentGroup(1)?.sourceInfo?.let { contextOf(it) } ?: return null
            var parentContext: SourceInformationContext? = context
            var index = 2
            while (index < stack.size && parentContext?.sourceFile == null) {
                parentContext = parentGroup(index++)?.sourceInfo?.let { contextOf(it) }
            }
            return context.sourceLocation(currentCallIndex, parentContext)
        }

    override val parameters: List<ParameterInformation>
        get() {
            val group = current
            val context = group.sourceInfo?.let { contextOf(it) } ?: return emptyList()
            val data = mutableListOf<Any?>()
            data.addAll(group.data)
            return extractParameterInfo(data, context)
        }

    override val depth: Int
        get() = stack.size

    private fun push(group: CompositionGroup) = stack.addLast(group)

    private fun pop() = stack.removeLast()

    private val current: CompositionGroup
        get() = stack.last()

    private fun parentGroup(parentDepth: Int): CompositionGroup? =
        if (stack.size > parentDepth) stack[stack.size - parentDepth - 1] else null

    private fun contextOf(information: String): SourceInformationContext? =
        contexts.getOrPut(information) { sourceInformationContextOf(information) }
            as? SourceInformationContext

    private fun isCall(group: CompositionGroup): Boolean =
        group.sourceInfo?.startsWith("C") ?: false
}

/** A cache of [SourceInformationContext] that optionally can be specified when using [mapTree]. */
@UiToolingDataApi
class ContextCache {
    /** Clears the cache. */
    fun clear() {
        contexts.clear()
    }

    internal val contexts = mutableMapOf<String, Any?>()
}

/**
 * Context with data for creating group nodes.
 *
 * See the factory argument of [mapTree].
 */
@UiToolingDataApi
interface SourceContext {
    /** The name of the Composable or null if not applicable. */
    val name: String?

    /** The bounds of the Composable if known. */
    val bounds: IntRect

    /** The [SourceLocation] of where the Composable was called. */
    val location: SourceLocation?

    /** The parameters of the Composable. */
    val parameters: List<ParameterInformation>

    /** The current depth into the [CompositionGroup] tree. */
    val depth: Int

    /** The source context is for a call to an inline composable function */
    val isInline: Boolean
        get() = false
}

/**
 * Return a tree of custom nodes for the slot table.
 *
 * The [factory] method will be called for every [CompositionGroup] in the slot tree and can be used
 * to create custom nodes based on the passed arguments. The [SourceContext] argument gives access
 * to additional information encoded in the [CompositionGroup.sourceInfo]. A return of null from
 * [factory] means that the entire subtree will be ignored.
 *
 * A [cache] can optionally be specified. If a client is calling [mapTree] multiple times, this can
 * save some time if the values of [CompositionGroup.sourceInfo] are not unique.
 */
@UiToolingDataApi
fun <T> CompositionData.mapTree(
    factory: (CompositionGroup, SourceContext, List<T>) -> T?,
    cache: ContextCache = ContextCache(),
): T? {
    val group = compositionGroups.firstOrNull() ?: return null
    val callStack = CompositionCallStack(factory, cache.contexts)
    val out = mutableListOf<T>()
    callStack.convert(group, 0, out)
    return out.firstOrNull()
}

/** Return the parameters found for this [CompositionGroup]. */
@UiToolingDataApi
fun CompositionGroup.findParameters(cache: ContextCache? = null): List<ParameterInformation> {
    val information = sourceInfo ?: return emptyList()
    val context =
        if (cache == null) sourceInformationContextOf(information)
        else
            cache.contexts.getOrPut(information) { sourceInformationContextOf(information) }
                as? SourceInformationContext
    val data = mutableListOf<Any?>()
    data.addAll(this.data)
    return extractParameterInfo(data, context)
}

/**
 * Return a group tree for for the slot table that represents the entire content of the slot table.
 */
@UiToolingDataApi
fun CompositionData.asTree(): Group = compositionGroups.firstOrNull()?.getGroup(null) ?: EmptyGroup

internal fun IntRect.union(other: IntRect): IntRect {
    if (this == emptyBox) return other else if (other == emptyBox) return this

    return IntRect(
        left = min(left, other.left),
        top = min(top, other.top),
        bottom = max(bottom, other.bottom),
        right = max(right, other.right),
    )
}

@UiToolingDataApi
private fun keyPosition(key: Any?): String? =
    when (key) {
        is String -> key
        is JoinedKey -> keyPosition(key.left) ?: keyPosition(key.right)
        else -> null
    }

private const val parameterPrefix = "${'$'}"
private const val internalFieldPrefix = parameterPrefix + parameterPrefix
private const val defaultFieldName = "${internalFieldPrefix}default"
private const val changedFieldName = "${internalFieldPrefix}changed"
private const val jacocoDataField = "${parameterPrefix}jacoco"
private const val recomposeScopeNameSuffix = ".RecomposeScopeImpl"

@OptIn(ComposeToolingApi::class)
@UiToolingDataApi
private fun extractParameterInfo(
    data: List<Any?>,
    context: SourceInformationContext?,
): List<ParameterInformation> {
    val recomposeScope =
        data.firstOrNull { it != null && it.javaClass.name.endsWith(recomposeScopeNameSuffix) }
            ?: return emptyList()

    val block =
        recomposeScope.javaClass.accessibleField("block")?.get(recomposeScope) ?: return emptyList()

    val parametersMetadata = context?.parameters.orEmpty()
    val blockClass = block.javaClass

    return try {
        val inlineFields = filterParameterFields(blockClass.declaredFields, isIndyLambda = true)

        if (inlineFields.isNotEmpty()) {
            extractFromIndyLambdaFields(inlineFields, block, parametersMetadata)
        } else {
            val legacyFields =
                filterParameterFields(blockClass.declaredFields, isIndyLambda = false)
            extractFromLegacyFields(legacyFields, block, parametersMetadata)
        }
    } catch (e: Exception) {
        emptyList()
    }
}

@OptIn(UiToolingDataApi::class, ComposeToolingApi::class)
private fun extractFromIndyLambdaFields(
    fields: List<Field>,
    block: Any,
    metadata: List<ParameterSourceInformation>,
): List<ParameterInformation> {
    val sortedFields =
        fields.sortedBy { it.name.substringAfter("f$").toIntOrNull() ?: Int.MAX_VALUE }

    val hasParameterNames = metadata.isEmpty() || metadata.any { it.name != null }
    val realFields =
        if (hasParameterNames) {
            // Lambda fields might contain additional synthetic parameters.
            // If we know the definitive list with parameter names, only take those parameters.
            sortedFields.take(metadata.size)
        } else {
            sortedFields
        }

    // todo: parameter logic assumes one changed parameter and one default
    val changedIndex = if (hasParameterNames) metadata.size else sortedFields.size
    val changed = (sortedFields.getOrNull(changedIndex)?.get(block) as? Int) ?: 0
    val defaults = (sortedFields.getOrNull(changedIndex + 1)?.get(block) as? Int) ?: 0

    return realFields.mapIndexed { index, field ->
        buildParameterInfo(
            field,
            block,
            index,
            defaults,
            changed,
            metadata.firstOrNull { it.sortedIndex == index },
        )
    }
}

@OptIn(UiToolingDataApi::class, ComposeToolingApi::class)
private fun extractFromLegacyFields(
    fields: List<Field>,
    block: Any,
    metadata: List<ParameterSourceInformation>,
): List<ParameterInformation> {
    val blockClass = block.javaClass
    val defaults = blockClass.accessibleField(defaultFieldName)?.get(block) as? Int ?: 0
    val changed = blockClass.accessibleField(changedFieldName)?.get(block) as? Int ?: 0

    val hasParameterNames = metadata.isEmpty() || metadata.any { it.name != null }
    val sorted =
        if (hasParameterNames) {
            metadata.sortedBy { it.name }
        } else {
            metadata
        }

    return fields.mapIndexedNotNull { index, _ ->
        val paramMeta = sorted.getOrNull(index) ?: ParameterSourceInformation(index)
        val sortedIndex = paramMeta.sortedIndex
        if (sortedIndex >= fields.size) return@mapIndexedNotNull null

        val field = fields[sortedIndex]
        buildParameterInfo(field, block, index, defaults, changed, paramMeta)
    }
}

@OptIn(ComposeToolingApi::class)
@UiToolingDataApi
private fun buildParameterInfo(
    field: Field,
    block: Any,
    index: Int,
    defaults: Int,
    changed: Int,
    metadata: ParameterSourceInformation?,
): ParameterInformation {
    field.isAccessible = true
    val value = field.get(block)

    val fromDefault = (1 shl index) and defaults != 0
    val changedOffset = index * BITS_PER_SLOT + 1
    val parameterChanged = ((SLOT_MASK shl changedOffset) and changed) shr changedOffset

    val static = parameterChanged and STATIC_BITS == STATIC_BITS
    val compared = parameterChanged and STATIC_BITS == 0
    val stable = parameterChanged and STABLE_BITS == 0

    return ParameterInformation(
        name = metadata?.name ?: field.name.substring(1),
        value = value,
        fromDefault = fromDefault,
        static = static,
        compared = compared && !fromDefault,
        inlineClass = metadata?.inlineClass,
        stable = stable,
    )
}

private fun filterParameterFields(fields: Array<Field>, isIndyLambda: Boolean): List<Field> {
    return fields.filter { field ->
        val name = field.name
        val matchesInlinePattern = name.matches(Regex("^f\\$\\d+$"))
        val matchesLegacyPattern = name.startsWith(parameterPrefix)

        val validPrefix =
            isIndyLambda && matchesInlinePattern || !isIndyLambda && matchesLegacyPattern

        validPrefix && !name.startsWith(internalFieldPrefix) && !name.startsWith(jacocoDataField)
    }
}

private const val BITS_PER_SLOT = 3
private const val SLOT_MASK = 0b111
private const val STATIC_BITS = 0b011
private const val STABLE_BITS = 0b100

/** The source position of the group extracted from the key, if one exists for the group. */
@UiToolingDataApi
val Group.position: String?
    get() = keyPosition(key)

private fun Class<*>.accessibleField(name: String): Field? =
    declaredFields.firstOrNull { it.name == name }?.apply { isAccessible = true }
