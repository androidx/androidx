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

package androidx.compose.ui.inspection

import android.util.Log
import android.view.View
import androidx.collection.LongList
import androidx.collection.LongObjectMap
import androidx.collection.MutableLongObjectMap
import androidx.collection.longObjectMapOf
import androidx.collection.mutableIntListOf
import androidx.collection.mutableLongObjectMapOf
import androidx.compose.ui.inspection.compose.AndroidComposeViewWrapper
import androidx.compose.ui.inspection.compose.convertToParameterGroup
import androidx.compose.ui.inspection.framework.addSlotTable
import androidx.compose.ui.inspection.framework.flatten
import androidx.compose.ui.inspection.framework.hasSlotTable
import androidx.compose.ui.inspection.framework.isAndroidComposeView
import androidx.compose.ui.inspection.framework.signature
import androidx.compose.ui.inspection.inspector.InspectorNode
import androidx.compose.ui.inspection.inspector.LayoutInspectorTree
import androidx.compose.ui.inspection.inspector.NodeParameterReference
import androidx.compose.ui.inspection.proto.ConversionContext
import androidx.compose.ui.inspection.proto.StringTable
import androidx.compose.ui.inspection.proto.convert
import androidx.compose.ui.inspection.proto.toComposableRoot
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.compose.ui.inspection.util.NO_ANCHOR_ID
import androidx.compose.ui.inspection.util.ThreadUtils
import androidx.compose.ui.inspection.util.groupByToLongObjectMap
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.IntOffset
import androidx.inspection.ArtTooling
import androidx.inspection.Connection
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import kotlin.collections.removeLast as removeLastKt
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParameterDetailsCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParameterDetailsResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ParameterGroup
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Response
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.UnknownCommandResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.UpdateSettingsCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.UpdateSettingsResponse

// The ".studio" prefix prevents logs from showing in logcat,
// unless the "logcat.ignore.studio.tags" flag is disabled.
internal const val LOG_TAG = "ComposeInspector"
internal const val SPAM_LOG_TAG = "studio.$LOG_TAG"

private const val LAYOUT_INSPECTION_ID = "layoutinspector.compose.inspection"
private const val MAX_RECURSIONS = 2
private const val MAX_ITERABLE_SIZE = 5

// created by java.util.ServiceLoader
class ComposeLayoutInspectorFactory :
    InspectorFactory<ComposeLayoutInspector>(LAYOUT_INSPECTION_ID) {
    override fun createInspector(
        connection: Connection,
        environment: InspectorEnvironment,
    ): ComposeLayoutInspector {
        return ComposeLayoutInspector(connection, environment)
    }
}

class ComposeLayoutInspector(
    connection: Connection,
    // Keep this instance for easy access through reflection:
    private val environment: InspectorEnvironment,
) : Inspector(connection) {

    /** Cache data which allows us to reuse previously queried inspector nodes */
    private class CacheData(val rootView: View, val trees: List<CacheTree>) {
        /** The cached nodes as a map from node id to InspectorNode */
        val lookup: LongObjectMap<InspectorNode>
            get() = _lookup ?: createLookup()

        private fun createLookup(): LongObjectMap<InspectorNode> {
            val result = mutableLongObjectMapOf<InspectorNode>()
            val stack = mutableListOf<InspectorNode>()
            trees.forEach { stack.addAll(it.nodes) }
            while (stack.isNotEmpty()) {
                val node = stack.removeLastKt()
                stack.addAll(node.children)
                result.put(node.id, node)
            }
            _lookup = result
            return result
        }

        private var _lookup: LongObjectMap<InspectorNode>? = null
    }

    /** Cache data for a tree of [InspectorNode]s under a [viewParent] */
    internal class CacheTree(
        val viewParent: View,
        val nodes: List<InspectorNode>,
        val viewsToSkip: LongList,
    )

    private val rootsDetector = RootsDetector(environment)
    private val anchorMap = AnchorMap()
    private val layoutInspectorTree = LayoutInspectorTree(anchorMap)
    private val recompositionHandler = RecompositionHandler(environment.artTooling(), anchorMap)
    private var delayParameterExtractions = false
    // Reduce the protobuf nesting of ComposableNode by storing nested nodes with only 1 child each
    // as children under the top node. This limits the stack used when computing the protobuf size.
    private var reduceChildNesting = false

    // Sidestep threading concerns by only ever accessing cachedNodes on the inspector thread
    private val inspectorThread = Thread.currentThread()
    private val _cachedNodes = mutableLongObjectMapOf<CacheData>()
    private var cachedGeneration = 0
    private var cachedSystemComposablesSkipped = false
    private var cachedHasAllParameters = false
    // Keep this instance for easy access through reflection
    private var disposed = false
    private val cachedNodes: MutableLongObjectMap<CacheData>
        get() {
            check(Thread.currentThread() == inspectorThread) {
                "cachedNodes should be accessed by the inspector thread"
            }
            return _cachedNodes
        }

    init {
        enableInspection(environment.artTooling())
    }

    override fun onDispose() {
        disposed = true
        recompositionHandler.dispose()
        cachedNodes.clear()
    }

    override fun onReceiveCommand(data: ByteArray, callback: CommandCallback) {
        val command =
            try {
                Command.parseFrom(data)
            } catch (ignored: InvalidProtocolBufferException) {
                handleUnknownCommand(data, callback)
                return
            }

        when (command.specializedCase) {
            Command.SpecializedCase.GET_COMPOSABLES_COMMAND -> {
                handleGetComposablesCommand(command.getComposablesCommand, callback)
            }
            Command.SpecializedCase.GET_PARAMETERS_COMMAND -> {
                handleGetParametersCommand(command.getParametersCommand, callback)
            }
            Command.SpecializedCase.GET_ALL_PARAMETERS_COMMAND -> {
                handleGetAllParametersCommand(command.getAllParametersCommand, callback)
            }
            Command.SpecializedCase.GET_PARAMETER_DETAILS_COMMAND -> {
                handleGetParameterDetailsCommand(command.getParameterDetailsCommand, callback)
            }
            Command.SpecializedCase.UPDATE_SETTINGS_COMMAND -> {
                handleUpdateSettingsCommand(command.updateSettingsCommand, callback)
            }
            else -> handleUnknownCommand(data, callback)
        }
    }

    private fun handleUnknownCommand(commandBytes: ByteArray, callback: CommandCallback) {
        callback.reply {
            unknownCommandResponse =
                UnknownCommandResponse.newBuilder()
                    .apply { this.commandBytes = ByteString.copyFrom(commandBytes) }
                    .build()
        }
    }

    private fun handleGetComposablesCommand(
        getComposablesCommand: GetComposablesCommand,
        callback: CommandCallback,
    ) {
        val data =
            getComposableNodes(
                getComposablesCommand.rootViewId,
                getComposablesCommand.skipSystemComposables,
                getComposablesCommand.extractAllParameters || !delayParameterExtractions,
                getComposablesCommand.generation,
                getComposablesCommand.generation == 0,
            )

        val location = IntArray(2)
        data?.rootView?.getLocationOnScreen(location)
        val windowPos = IntOffset(location[0], location[1])

        val stringTable = StringTable()
        val context =
            ConversionContext(stringTable, windowPos, recompositionHandler, reduceChildNesting)
        val trees = data?.trees ?: emptyList()
        val roots = trees.map { it.toComposableRoot(context) }

        callback.reply {
            getComposablesResponse =
                GetComposablesResponse.newBuilder()
                    .apply {
                        addAllStrings(stringTable.toStringEntries())
                        addAllRoots(roots)
                    }
                    .build()
        }
    }

    private fun handleGetParametersCommand(
        getParametersCommand: GetParametersCommand,
        callback: CommandCallback,
    ) {
        val foundComposable =
            if (
                delayParameterExtractions &&
                    !cachedHasAllParameters &&
                    getParametersCommand.anchorHash != NO_ANCHOR_ID
            ) {
                getComposableFromAnchor(getParametersCommand.anchorHash)
            } else {
                getComposableNodes(
                        getParametersCommand.rootViewId,
                        getParametersCommand.skipSystemComposables,
                        true,
                        getParametersCommand.generation,
                    )
                    ?.lookup
                    ?.get(getParametersCommand.composableId)
            }
        val semanticsNode =
            getCachedComposableNodes(getParametersCommand.rootViewId)
                ?.lookup
                ?.get(getParametersCommand.composableId)

        callback.reply {
            getParametersResponse =
                if (foundComposable != null) {
                    val stringTable = StringTable()
                    GetParametersResponse.newBuilder()
                        .apply {
                            parameterGroup =
                                foundComposable.convertToParameterGroup(
                                    semanticsNode ?: foundComposable,
                                    layoutInspectorTree,
                                    getParametersCommand.rootViewId,
                                    getParametersCommand.maxRecursions.orElse(MAX_RECURSIONS),
                                    getParametersCommand.maxInitialIterableSize.orElse(
                                        MAX_ITERABLE_SIZE
                                    ),
                                    stringTable,
                                )
                            addAllStrings(stringTable.toStringEntries())
                        }
                        .build()
                } else {
                    GetParametersResponse.getDefaultInstance()
                }
        }
    }

    private fun handleGetAllParametersCommand(
        getAllParametersCommand: GetAllParametersCommand,
        callback: CommandCallback,
    ) {
        val allComposables =
            getComposableNodes(
                    getAllParametersCommand.rootViewId,
                    getAllParametersCommand.skipSystemComposables,
                    true,
                    getAllParametersCommand.generation,
                )
                ?.lookup ?: longObjectMapOf()

        callback.reply {
            val stringTable = StringTable()
            val parameterGroups = mutableListOf<ParameterGroup>()
            allComposables.forEachValue { composable ->
                parameterGroups.add(
                    composable.convertToParameterGroup(
                        composable,
                        layoutInspectorTree,
                        getAllParametersCommand.rootViewId,
                        getAllParametersCommand.maxRecursions.orElse(MAX_RECURSIONS),
                        getAllParametersCommand.maxInitialIterableSize.orElse(MAX_ITERABLE_SIZE),
                        stringTable,
                    )
                )
            }

            getAllParametersResponse =
                GetAllParametersResponse.newBuilder()
                    .apply {
                        rootViewId = getAllParametersCommand.rootViewId
                        addAllParameterGroups(parameterGroups)
                        addAllStrings(stringTable.toStringEntries())
                    }
                    .build()
        }
    }

    private fun handleGetParameterDetailsCommand(
        getParameterDetailsCommand: GetParameterDetailsCommand,
        callback: CommandCallback,
    ) {
        val indices = mutableIntListOf()
        getParameterDetailsCommand.reference.compositeIndexList.forEach { indices.add(it) }
        val reference =
            NodeParameterReference(
                getParameterDetailsCommand.reference.composableId,
                getParameterDetailsCommand.reference.anchorHash,
                getParameterDetailsCommand.reference.kind.convert(),
                getParameterDetailsCommand.reference.parameterIndex,
                indices,
            )
        val foundComposable =
            if (
                delayParameterExtractions &&
                    !cachedHasAllParameters &&
                    reference.anchorId != NO_ANCHOR_ID
            ) {
                getComposableFromAnchor(reference.anchorId)
            } else {
                getComposableNodes(
                        getParameterDetailsCommand.rootViewId,
                        getParameterDetailsCommand.skipSystemComposables,
                        true,
                        getParameterDetailsCommand.generation,
                    )
                    ?.lookup
                    ?.get(reference.nodeId)
            }
        val semanticsNode =
            getCachedComposableNodes(getParameterDetailsCommand.rootViewId)
                ?.lookup
                ?.get(getParameterDetailsCommand.reference.composableId)
        val expanded =
            foundComposable?.let { composable ->
                layoutInspectorTree.expandParameter(
                    getParameterDetailsCommand.rootViewId,
                    semanticsNode ?: composable,
                    reference,
                    getParameterDetailsCommand.startIndex,
                    getParameterDetailsCommand.maxElements,
                    getParameterDetailsCommand.maxRecursions.orElse(MAX_RECURSIONS),
                    getParameterDetailsCommand.maxInitialIterableSize.orElse(MAX_ITERABLE_SIZE),
                )
            }

        callback.reply {
            getParameterDetailsResponse =
                if (expanded != null) {
                    val stringTable = StringTable()
                    GetParameterDetailsResponse.newBuilder()
                        .apply {
                            rootViewId = getParameterDetailsCommand.rootViewId
                            parameter = expanded.convert(stringTable)
                            addAllStrings(stringTable.toStringEntries())
                        }
                        .build()
                } else {
                    GetParameterDetailsResponse.getDefaultInstance()
                }
        }
    }

    private fun handleUpdateSettingsCommand(
        updateSettingsCommand: UpdateSettingsCommand,
        callback: CommandCallback,
    ) {
        recompositionHandler.changeCollectionMode(
            updateSettingsCommand.includeRecomposeCounts,
            updateSettingsCommand.keepRecomposeCounts,
        )
        delayParameterExtractions = updateSettingsCommand.delayParameterExtractions
        reduceChildNesting = updateSettingsCommand.reduceChildNesting
        callback.reply {
            updateSettingsResponse =
                UpdateSettingsResponse.newBuilder()
                    .apply { canDelayParameterExtractions = true }
                    .build()
        }
    }

    /**
     * Get all [InspectorNode]s found under the layout tree rooted by [rootViewId]. They will be
     * mapped with their ID as the key.
     *
     * This will return cached data if possible, but may request new data otherwise, blocking the
     * current thread potentially.
     */
    private fun getComposableNodes(
        rootViewId: Long,
        skipSystemComposables: Boolean,
        includeAllParameters: Boolean,
        generation: Int,
        forceRegeneration: Boolean = false,
    ): CacheData? {
        if (
            !forceRegeneration &&
                generation == cachedGeneration &&
                skipSystemComposables == cachedSystemComposablesSkipped &&
                (!includeAllParameters || cachedHasAllParameters)
        ) {
            return cachedNodes[rootViewId]
        }

        val data =
            ThreadUtils.runOnMainThread {
                    layoutInspectorTree.includeAllParameters = includeAllParameters
                    val composeViewWrappers =
                        getAndroidComposeViews(rootViewId, skipSystemComposables, generation)
                    val composeViews = composeViewWrappers.map { it.composeView }
                    val nodesByComposeView =
                        layoutInspectorTree
                            .apply { this.hideSystemNodes = skipSystemComposables }
                            .convert(composeViews)
                    val composeViewsByRoot =
                        mutableLongObjectMapOf<MutableList<AndroidComposeViewWrapper>>()
                    composeViewWrappers.groupByToLongObjectMap(composeViewsByRoot) {
                        it.rootView.uniqueDrawingId
                    }
                    val data = mutableLongObjectMapOf<CacheData>()
                    composeViewsByRoot.forEach { key, value ->
                        data.put(
                            key,
                            CacheData(
                                value.first().rootView,
                                value.map {
                                    val nodes =
                                        nodesByComposeView[it.composeView.uniqueDrawingId]
                                            ?: emptyList()
                                    CacheTree(it.viewParent, nodes, it.viewsToSkip)
                                },
                            ),
                        )
                    }
                    data
                }
                .get()

        cachedNodes.clear()
        cachedNodes.putAll(data)
        cachedGeneration = generation
        cachedSystemComposablesSkipped = skipSystemComposables
        cachedHasAllParameters = includeAllParameters
        return cachedNodes[rootViewId]
    }

    /** Return the cached [InspectorNode]s found under the layout tree rooted by [rootViewId]. */
    private fun getCachedComposableNodes(rootViewId: Long): CacheData? = cachedNodes[rootViewId]

    /**
     * Find an [InspectorNode] with extracted parameters that represent the composable with the
     * specified anchor hash.
     */
    private fun getComposableFromAnchor(anchorId: Int): InspectorNode? =
        ThreadUtils.runOnMainThread {
                layoutInspectorTree.includeAllParameters = false
                val composeViews = getAndroidComposeViews(-1L, false, 1)
                composeViews.firstNotNullOfOrNull {
                    layoutInspectorTree.findParameters(it.composeView, anchorId)
                }
            }
            .get()

    /** Get all AndroidComposeView instances found within the layout tree rooted by [rootViewId]. */
    private fun getAndroidComposeViews(
        rootViewId: Long,
        skipSystemComposables: Boolean,
        generation: Int,
    ): List<AndroidComposeViewWrapper> {
        ThreadUtils.assertOnMainThread()

        val roots =
            rootsDetector.getRoots().asSequence().filter { root ->
                root.visibility == View.VISIBLE &&
                    root.isAttachedToWindow &&
                    (generation > 0 || root.uniqueDrawingId == rootViewId)
            }

        val wrappers = mutableListOf<AndroidComposeViewWrapper>()
        roots.forEach { root ->
            root.flatten().mapNotNullTo(wrappers) { view ->
                AndroidComposeViewWrapper.tryCreateFor(root, view, skipSystemComposables)
            }
        }
        return wrappers
    }

    /** Enable inspection in this app. */
    @Suppress("BanUncheckedReflection")
    private fun enableInspection(artTooling: ArtTooling) {
        // Enable debug inspector information.
        enableDebugInspectorInfo()

        // Install a hook that will add a slot table to any AndroidComposeViews added later.
        // This is needed to get composables from dialogs etc.
        val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
        val method = wrapper.declaredMethods.firstOrNull { it.name == "setContent" } ?: return
        val field = wrapper.getDeclaredMethod("getOwner")
        val signature = method.signature
        artTooling.registerEntryHook(wrapper, signature) { wr, _ ->
            val owner = field(wr) as? View
            owner?.addSlotTable()
        }

        // Add slot tables to all already existing AndroidComposeViews:
        addSlotTableToComposeViews()
    }

    /** Add a slot table to all AndroidComposeViews that doesn't already have one. */
    private fun addSlotTableToComposeViews() =
        ThreadUtils.runOnMainThread {
                val roots = rootsDetector.getAllRoots()
                val composeViews =
                    roots.flatMap { it.flatten() }.filter { it.isAndroidComposeView() }

                if (composeViews.any { !it.hasSlotTable }) {
                    val slotTablesAdded = composeViews.sumOf { it.addSlotTable() }
                    if (slotTablesAdded > 0) {
                        // The slot tables added to existing views will be empty until the
                        // composables are reloaded. Do that now:
                        hotReload()
                    }
                }
            }
            .get()

    /**
     * Perform a hot reload after adding SlotTable storage.
     *
     * This will populate the slot tables that were just added.
     */
    @Suppress("BanUncheckedReflection")
    private fun hotReload() {
        val hotReload = Class.forName("androidx.compose.runtime.HotReloader")
        val companion = hotReload.getField("Companion").get(null)
        val save = companion.javaClass.getDeclaredMethod("saveStateAndDispose", Any::class.java)
        val load = companion.javaClass.getDeclaredMethod("loadStateAndCompose", Any::class.java)
        save.isAccessible = true
        load.isAccessible = true
        // Add a context parameter even though it is not currently used.
        // (It was required in earlier versions of the Compose runtime.)
        val context =
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentApplication")
                .apply { isAccessible = true }
                .invoke(null)
        val state = save(companion, context)
        load(companion, state)
    }

    private fun enableDebugInspectorInfo() {
        // Set isDebugInspectorInfoEnabled to true via reflection such that R8 cannot see the
        // assignment. This allows the InspectorInfo lambdas to be stripped from release builds.
        if (!isDebugInspectorInfoEnabled) {
            try {
                val packageClass = Class.forName("androidx.compose.ui.platform.InspectableValueKt")
                val field = packageClass.getDeclaredField("isDebugInspectorInfoEnabled")
                field.isAccessible = true
                field.setBoolean(null, true)
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Could not access isDebugInspectorInfoEnabled.", ex)
            }
        }
    }
}

private fun Inspector.CommandCallback.reply(initResponse: Response.Builder.() -> Unit) {
    val response = Response.newBuilder()
    response.initResponse()
    reply(response.build().toByteArray())
}

// Provide default for older version:
private fun Int.orElse(defaultValue: Int): Int = if (this == 0) defaultValue else this
