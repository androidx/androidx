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

@file:Suppress("PrimitiveInCollection")
@file:OptIn(ExperimentalRemotePlayerApi::class)

package androidx.compose.remote.player.compose.embedded.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BinaryData
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.MapRcDocVariables
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.managers.RcDocLayout
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalHostRemoteActionHandler
import androidx.compose.remote.player.compose.embedded.LocalHostRemoteNamedActionHandler
import androidx.compose.remote.player.compose.embedded.LocalRcCustomPlugins
import androidx.compose.remote.player.compose.embedded.LocalRcDocMappingsMap
import androidx.compose.remote.player.compose.embedded.LocalRcDocMaxNestingDepth
import androidx.compose.remote.player.compose.embedded.LocalRcDocumentResolver
import androidx.compose.remote.player.compose.embedded.LocalRcIsMeasureProbe
import androidx.compose.remote.player.compose.embedded.LocalRcPlayerFeatureResolver
import androidx.compose.remote.player.compose.embedded.LocalRcPlayerFeatures
import androidx.compose.remote.player.compose.embedded.LocalRcPlayerTheme
import androidx.compose.remote.player.compose.embedded.LocalRcTypefaceResolver
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.RcDocumentResolver
import androidx.compose.remote.player.compose.embedded.RcPlayerImpl
import androidx.compose.remote.player.compose.embedded.RcPlayerState
import androidx.compose.remote.player.compose.embedded.doc.SnapshotRemoteComposeState
import androidx.compose.remote.player.compose.embedded.doc.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CancellationException

private val LocalRcDocNestingDepth = compositionLocalOf { 0 }

private data class ChildHeaderDimensions(
    val width: Int,
    val height: Int,
    val densityBehavior: Int,
)

private class ParsedChildDocument(
    val state: RcPlayerState,
    val headerDimensions: ChildHeaderDimensions,
)

private fun extractHeaderDimensions(doc: CoreDocument): ChildHeaderDimensions {
    val header = doc.operations.fastFirstOrNull { it is Header } as? Header
    val width =
        (doc.getProperty(Header.DOC_WIDTH) as? Int)?.takeIf { it > 0 }
            ?: header?.getInt(Header.DOC_WIDTH.toInt(), 0)?.takeIf { it > 0 }
            ?: doc.width.takeIf { it > 0 }
            ?: 0
    val height =
        (doc.getProperty(Header.DOC_HEIGHT) as? Int)?.takeIf { it > 0 }
            ?: header?.getInt(Header.DOC_HEIGHT.toInt(), 0)?.takeIf { it > 0 }
            ?: doc.height.takeIf { it > 0 }
            ?: 0
    return ChildHeaderDimensions(
        width = width,
        height = height,
        densityBehavior = doc.densityBehavior,
    )
}

/**
 * Embedded Compose player implementation of [RcDocLayout]: resolves the embedded document bytes
 * (from an inline [BinaryData] blob or the host [RcDocumentResolver]), instantiates a nested
 * [RcPlayerImpl] with isolated state, and synchronizes [MapRcDocVariables] bindings and
 * `HostAction` interceptions across the parent/child boundary.
 */
@Composable
internal fun RcPlayerRcDoc(layout: RcDocLayout, modifier: Modifier) {
    val depth = LocalRcDocNestingDepth.current
    val maxDepth = LocalRcDocMaxNestingDepth.current
    if (depth >= maxDepth) {
        return
    }

    val isMeasureProbe = LocalRcIsMeasureProbe.current
    val parentDoc = LocalCoreDocument.current
    val hostRemoteContext = LocalRemoteContext.current
    val hostReadContext: RemoteContext = LocalGraphContext.current ?: hostRemoteContext
    val documentResolver = LocalRcDocumentResolver.current
    val mappings = LocalRcDocMappingsMap.current[layout.componentId].orEmpty()

    // 1. Check whether urlId points directly to a TYPE_RC_DOC BinaryData blob.
    val directBlob =
        (hostRemoteContext.getObject(layout.urlId) as? BinaryData)?.takeIf {
            it.type == BinaryData.TYPE_RC_DOC
        }

    // 2. Otherwise read the URL string reactively, checking rc:embedded/<id> before resolving
    // asynchronously via RcDocumentResolver.
    val resolvedBytes: ByteArray?
    val isInlineBlob: Boolean
    if (directBlob != null) {
        resolvedBytes = directBlob.data
        isInlineBlob = true
    } else {
        val url by rememberRemoteStringAsState(layout.urlId)
        val embeddedBlob =
            if (url.startsWith(RcDocLayout.EMBEDDED_URL_PREFIX)) {
                val blobId = url.removePrefix(RcDocLayout.EMBEDDED_URL_PREFIX).trim().toIntOrNull()
                blobId?.let { id ->
                    (hostRemoteContext.getObject(id) as? BinaryData)?.takeIf {
                        it.type == BinaryData.TYPE_RC_DOC
                    }
                }
            } else {
                null
            }
        if (embeddedBlob != null) {
            resolvedBytes = embeddedBlob.data
            isInlineBlob = true
        } else if (!isMeasureProbe && url.isNotEmpty() && documentResolver != null) {
            // Key the state holder on (url, documentResolver) so switching URLs immediately clears
            // the previous child document and a cancelling in-flight fetch cannot clobber a newer
            // resolution.
            val externalBytesState =
                remember(url, documentResolver) { mutableStateOf<ByteArray?>(null) }
            LaunchedEffect(url, documentResolver) {
                externalBytesState.value =
                    try {
                        documentResolver.resolve(url)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        null
                    }
            }
            resolvedBytes = externalBytesState.value
            isInlineBlob = false
        } else {
            resolvedBytes = null
            isInlineBlob = false
        }
    }

    val density = LocalDensity.current.density

    // FitBox measure probes subcompose all candidate branches alongside the chosen branch.
    // Measure using only the child header's intrinsic size without running variable sync,
    // LaunchedEffects, or mounting a duplicate RcPlayerImpl (sound/sensor/clock loops).
    if (isMeasureProbe) {
        val probeHeaderDimensions =
            remember(resolvedBytes, parentDoc.clock) {
                if (resolvedBytes == null || resolvedBytes.isEmpty()) {
                    null
                } else {
                    runCatching {
                        val buffer =
                            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(resolvedBytes))
                        val doc = CoreDocument(parentDoc.clock).apply { initFromBuffer(buffer) }
                        extractHeaderDimensions(doc)
                    }
                        .getOrNull()
                }
            }
        val probeIntrinsicModifier =
            if (
                probeHeaderDimensions != null &&
                    probeHeaderDimensions.width > 0 &&
                    probeHeaderDimensions.height > 0
            ) {
                val w =
                    rawDimensionDp(
                        probeHeaderDimensions.width.toFloat(),
                        probeHeaderDimensions.densityBehavior,
                        density,
                    )
                val h =
                    rawDimensionDp(
                        probeHeaderDimensions.height.toFloat(),
                        probeHeaderDimensions.densityBehavior,
                        density,
                    )
                Modifier.size(w, h)
            } else {
                Modifier
            }
        Box(modifier = modifier.then(probeIntrinsicModifier).clipToBounds())
        return
    }

    // 3. Parse the resolved bytes into an isolated CoreDocument + RcPlayerState, capturing the
    // initial header dimensions before placement can mutate CoreDocument.width/height.
    val parsedChild =
        remember(resolvedBytes, parentDoc.clock) {
            if (resolvedBytes == null || resolvedBytes.isEmpty()) {
                null
            } else {
                runCatching {
                    val buffer =
                        RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(resolvedBytes))
                    val doc = CoreDocument(parentDoc.clock).apply { initFromBuffer(buffer) }
                    // Construct RcPlayerState first so Header.apply populates doc properties and
                    // densityBehavior, while still capturing dimensions before placement can
                    // mutate CoreDocument.width/height.
                    val state = RcPlayerState(doc, defaultPrefix = null)
                    val headerDimensions = extractHeaderDimensions(doc)
                    ParsedChildDocument(state = state, headerDimensions = headerDimensions)
                }
                    .getOrNull()
            }
        }
    val childState = parsedChild?.state

    // 4. Index child named variables and run synchronous initial sync so frame 0 sees mapped
    // values.
    val childVarsByName = remember(childState) { buildChildNamedVariablesMap(childState) }
    val syncState =
        remember(childState, mappings, hostReadContext, hostRemoteContext) {
            RcDocSyncState().also { state ->
                if (childState != null && mappings.isNotEmpty()) {
                    Snapshot.withoutReadObservation {
                        syncInitialVariables(
                            mappings = mappings,
                            hostReadContext = hostReadContext,
                            hostRemoteContext = hostRemoteContext,
                            childState = childState,
                            childVarsByName = childVarsByName,
                            syncState = state,
                        )
                    }
                }
            }
        }

    // 5. Reactively keep forward, reverse, and bidirectional variable mappings in sync.
    if (childState != null && mappings.isNotEmpty()) {
        val hasVariableMappings =
            remember(mappings) { mappings.fastAny { it.mode != MapRcDocVariables.MAP_HOST_ACTION } }
        if (hasVariableMappings) {
            LaunchedEffect(childState, mappings, hostReadContext, hostRemoteContext, syncState) {
                snapshotFlow {
                    captureMappingSnapshots(
                        mappings = mappings,
                        hostReadContext = hostReadContext,
                        childReadContext = childState.graphContext,
                        childVarsByName = childVarsByName,
                    )
                }
                    .collect { snapshots ->
                        applyMappingSnapshots(
                            snapshots = snapshots,
                            hostReadContext = hostReadContext,
                            hostRemoteContext = hostRemoteContext,
                            childRemoteContext = childState.remoteContext,
                            syncState = syncState,
                        )
                    }
            }
        }
    }

    // 6. Intercept MAP_HOST_ACTION actions from the child; forward unmapped actions directly to the
    // raw host callbacks (never through the parent's PendingIntent/Lambda interceptors).
    val hostActionHandler = LocalHostRemoteActionHandler.current
    val hostNamedActionHandler = LocalHostRemoteNamedActionHandler.current
    val childOnAction: (Int, String?) -> Unit =
        remember(mappings, hostRemoteContext, hostActionHandler) {
            { actionId, value ->
                var mapped = false
                val metadata = value ?: ""
                mappings.fastForEach { mapping ->
                    if (
                        mapping.mode == MapRcDocVariables.MAP_HOST_ACTION &&
                            mapping.remoteTarget == actionId
                    ) {
                        hostRemoteContext.mRemoteComposeState.overrideData(
                            mapping.localId,
                            metadata,
                        )
                        mapped = true
                    }
                }
                if (!mapped) {
                    hostActionHandler(actionId, value)
                }
            }
        }

    // 7. Size and clip: adopt the child's remembered header dimensions (using the child's own
    // densityBehavior) when no explicit size modifier constrains this component; outer size
    // modifiers take precedence over the inner intrinsic size.
    val headerDimensions = parsedChild?.headerDimensions
    val intrinsicModifier =
        if (headerDimensions != null && headerDimensions.width > 0 && headerDimensions.height > 0) {
            val w =
                rawDimensionDp(
                    headerDimensions.width.toFloat(),
                    headerDimensions.densityBehavior,
                    density,
                )
            val h =
                rawDimensionDp(
                    headerDimensions.height.toFloat(),
                    headerDimensions.densityBehavior,
                    density,
                )
            Modifier.size(w, h)
        } else {
            Modifier
        }

    val parentFeatures = LocalRcPlayerFeatures.current
    val featureResolver = LocalRcPlayerFeatureResolver.current
    val childFeatures =
        remember(childState, parentFeatures, featureResolver) {
            if (childState != null && featureResolver != null) {
                featureResolver(childState.document.profileMask)
            } else {
                parentFeatures
            }
        }
    val parentTheme = LocalRcPlayerTheme.current
    val customPlugins = LocalRcCustomPlugins.current
    val typefaceResolver = LocalRcTypefaceResolver.current
    // Only propagate trustedContent for inline BinaryData blobs authored inside the trusted host
    // document; externally resolved bytes are treated as untrusted.
    val parentTrustedContent =
        (hostRemoteContext.mRemoteComposeState as? SnapshotRemoteComposeState)?.trustedContent ==
            true
    val childTrustedContent = parentTrustedContent && isInlineBlob

    Box(modifier = modifier.then(intrinsicModifier).clipToBounds()) {
        if (childState != null) {
            CompositionLocalProvider(LocalRcDocNestingDepth provides depth + 1) {
                RcPlayerImpl(
                    state = childState,
                    features = childFeatures,
                    modifier = Modifier.fillMaxSize(),
                    imageLoader = null,
                    trustedContent = childTrustedContent,
                    onAction = childOnAction,
                    onNamedAction = hostNamedActionHandler,
                    customPlugins = customPlugins,
                    typefaceResolver = typefaceResolver,
                    theme = parentTheme,
                    documentResolver = documentResolver,
                    featureResolver = featureResolver,
                )
            }
        }
    }
}

private class RcDocSyncState {
    val lastSyncedFloat = HashMap<MapRcDocVariables, Float>()
    val lastSyncedInt = HashMap<MapRcDocVariables, Int>()
    val lastSyncedColor = HashMap<MapRcDocVariables, Int>()
    val lastSyncedText = HashMap<MapRcDocVariables, String?>()
}

private data class MappingSnapshot(
    val mapping: MapRcDocVariables,
    val entries: List<NamedVariable>,
    val varType: Int,
    val hostValue: Any?,
    val childValue: Any?,
)

private fun buildChildNamedVariablesMap(
    childState: RcPlayerState?
): Map<String, List<NamedVariable>> {
    if (childState == null) return emptyMap()
    val map = HashMap<String, MutableList<NamedVariable>>()
    childState.preprocessed.constantOps.fastForEach { op ->
        if (op is NamedVariable) {
            val list = map.getOrPut(op.mVarName) { ArrayList(1) }
            if (!list.fastAny { it.mVarId == op.mVarId && it.mVarType == op.mVarType }) {
                list.add(op)
            }
        }
    }
    return map
}

private fun readValue(context: RemoteContext, id: Int, varType: Int): Any? =
    when (varType) {
        NamedVariable.FLOAT_TYPE -> context.getFloat(id)
        NamedVariable.INT_TYPE -> context.getInteger(id)
        NamedVariable.COLOR_TYPE -> context.getColor(id)
        NamedVariable.STRING_TYPE -> context.getText(id)
        else -> null
    }

private fun writeToChild(
    childRemoteContext: RemoteContext,
    entries: List<NamedVariable>,
    varType: Int,
    value: Any?,
) {
    when (varType) {
        NamedVariable.FLOAT_TYPE -> {
            val f = value as? Float ?: return
            entries.fastForEach { entry -> childRemoteContext.overrideFloat(entry.mVarId, f) }
        }
        NamedVariable.INT_TYPE -> {
            val i = value as? Int ?: return
            entries.fastForEach { entry -> childRemoteContext.overrideInteger(entry.mVarId, i) }
        }
        NamedVariable.COLOR_TYPE -> {
            val c = value as? Int ?: return
            entries.fastForEach { entry ->
                childRemoteContext.mRemoteComposeState.overrideColor(entry.mVarId, c)
            }
        }
        NamedVariable.STRING_TYPE -> {
            val s = value as? String ?: return
            entries.fastForEach { entry ->
                childRemoteContext.mRemoteComposeState.overrideData(entry.mVarId, s)
            }
        }
    }
}

private fun writeToHost(
    hostReadContext: RemoteContext,
    hostRemoteContext: RemoteContext,
    localId: Int,
    varType: Int,
    value: Any?,
) {
    val hostState = hostRemoteContext.mRemoteComposeState as? SnapshotRemoteComposeState
    when (varType) {
        NamedVariable.FLOAT_TYPE -> {
            val f = value as? Float ?: return
            if (
                hostReadContext.getFloat(localId).compareTo(f) != 0 ||
                    hostState?.isFloatOverridden(localId) != true
            ) {
                hostRemoteContext.overrideFloat(localId, f)
            }
        }
        NamedVariable.INT_TYPE -> {
            val i = value as? Int ?: return
            if (
                hostReadContext.getInteger(localId) != i ||
                    hostState?.isIntegerOverridden(localId) != true
            ) {
                hostRemoteContext.overrideInteger(localId, i)
            }
        }
        NamedVariable.COLOR_TYPE -> {
            val c = value as? Int ?: return
            if (
                hostReadContext.getColor(localId) != c ||
                    hostState?.isColorOverridden(localId) != true
            ) {
                hostRemoteContext.mRemoteComposeState.overrideColor(localId, c)
            }
        }
        NamedVariable.STRING_TYPE -> {
            val s = value as? String ?: return
            if (
                hostReadContext.getText(localId) != s ||
                    hostState?.isDataOverridden(localId) != true
            ) {
                hostRemoteContext.mRemoteComposeState.overrideData(localId, s)
            }
        }
    }
}

private fun syncInitialVariables(
    mappings: List<MapRcDocVariables>,
    hostReadContext: RemoteContext,
    hostRemoteContext: RemoteContext,
    childState: RcPlayerState,
    childVarsByName: Map<String, List<NamedVariable>>,
    syncState: RcDocSyncState,
) {
    val childReadContext = childState.graphContext
    val childRemoteContext = childState.remoteContext

    // Forward pass first (MAP_FORWARD + MAP_BIDIRECTIONAL: parent wins initial value).
    mappings.fastForEach { mapping ->
        val mode = mapping.mode
        if (mode != MapRcDocVariables.MAP_FORWARD && mode != MapRcDocVariables.MAP_BIDIRECTIONAL) {
            return@fastForEach
        }
        val remoteName = hostReadContext.getText(mapping.remoteTarget) ?: return@fastForEach
        val entries = childVarsByName[remoteName] ?: return@fastForEach
        if (entries.isEmpty()) return@fastForEach
        val varType = entries[0].mVarType
        val hostVal = readValue(hostReadContext, mapping.localId, varType)
        writeToChild(childRemoteContext, entries, varType, hostVal)
        recordLastSynced(syncState, mapping, varType, hostVal)
    }

    // Reverse pass second (MAP_REVERSE only; MAP_BIDIRECTIONAL already synced from parent).
    mappings.fastForEach { mapping ->
        if (mapping.mode != MapRcDocVariables.MAP_REVERSE) return@fastForEach
        val remoteName = hostReadContext.getText(mapping.remoteTarget) ?: return@fastForEach
        val entries = childVarsByName[remoteName] ?: return@fastForEach
        if (entries.isEmpty()) return@fastForEach
        val varType = entries[0].mVarType
        val childVal = readValue(childReadContext, entries[0].mVarId, varType)
        writeToHost(hostReadContext, hostRemoteContext, mapping.localId, varType, childVal)
        recordLastSynced(syncState, mapping, varType, childVal)
    }
}

private fun captureMappingSnapshots(
    mappings: List<MapRcDocVariables>,
    hostReadContext: RemoteContext,
    childReadContext: RemoteContext,
    childVarsByName: Map<String, List<NamedVariable>>,
): List<MappingSnapshot> {
    val result = ArrayList<MappingSnapshot>(mappings.size)
    mappings.fastForEach { mapping ->
        val mode = mapping.mode
        if (mode == MapRcDocVariables.MAP_HOST_ACTION) return@fastForEach
        val remoteName = hostReadContext.getText(mapping.remoteTarget) ?: return@fastForEach
        val entries = childVarsByName[remoteName] ?: return@fastForEach
        if (entries.isEmpty()) return@fastForEach
        val varType = entries[0].mVarType
        val hostVal =
            if (
                mode == MapRcDocVariables.MAP_FORWARD || mode == MapRcDocVariables.MAP_BIDIRECTIONAL
            ) {
                readValue(hostReadContext, mapping.localId, varType)
            } else {
                null
            }
        val childVal =
            if (
                mode == MapRcDocVariables.MAP_REVERSE || mode == MapRcDocVariables.MAP_BIDIRECTIONAL
            ) {
                readValue(childReadContext, entries[0].mVarId, varType)
            } else {
                null
            }
        result.add(MappingSnapshot(mapping, entries, varType, hostVal, childVal))
    }
    return result
}

private fun applyMappingSnapshots(
    snapshots: List<MappingSnapshot>,
    hostReadContext: RemoteContext,
    hostRemoteContext: RemoteContext,
    childRemoteContext: RemoteContext,
    syncState: RcDocSyncState,
) {
    snapshots.fastForEach { snap ->
        val mapping = snap.mapping
        when (mapping.mode) {
            MapRcDocVariables.MAP_FORWARD -> {
                if (valueDiffers(syncState, mapping, snap.varType, snap.hostValue)) {
                    writeToChild(childRemoteContext, snap.entries, snap.varType, snap.hostValue)
                    recordLastSynced(syncState, mapping, snap.varType, snap.hostValue)
                }
            }
            MapRcDocVariables.MAP_REVERSE -> {
                if (valueDiffers(syncState, mapping, snap.varType, snap.childValue)) {
                    writeToHost(
                        hostReadContext,
                        hostRemoteContext,
                        mapping.localId,
                        snap.varType,
                        snap.childValue,
                    )
                    recordLastSynced(syncState, mapping, snap.varType, snap.childValue)
                }
            }
            MapRcDocVariables.MAP_BIDIRECTIONAL -> {
                val hostChanged = valueDiffers(syncState, mapping, snap.varType, snap.hostValue)
                val childChanged = valueDiffers(syncState, mapping, snap.varType, snap.childValue)
                if (hostChanged) {
                    writeToChild(childRemoteContext, snap.entries, snap.varType, snap.hostValue)
                    recordLastSynced(syncState, mapping, snap.varType, snap.hostValue)
                } else if (childChanged) {
                    writeToHost(
                        hostReadContext,
                        hostRemoteContext,
                        mapping.localId,
                        snap.varType,
                        snap.childValue,
                    )
                    recordLastSynced(syncState, mapping, snap.varType, snap.childValue)
                }
            }
        }
    }
}

private fun valueDiffers(
    syncState: RcDocSyncState,
    mapping: MapRcDocVariables,
    varType: Int,
    value: Any?,
): Boolean =
    when (varType) {
        NamedVariable.FLOAT_TYPE -> {
            val f = value as? Float ?: return false
            val last = syncState.lastSyncedFloat[mapping]
            last == null || last.compareTo(f) != 0
        }
        NamedVariable.INT_TYPE -> {
            val i = value as? Int ?: return false
            syncState.lastSyncedInt[mapping] != i
        }
        NamedVariable.COLOR_TYPE -> {
            val c = value as? Int ?: return false
            syncState.lastSyncedColor[mapping] != c
        }
        NamedVariable.STRING_TYPE -> {
            val s = value as? String ?: return false
            syncState.lastSyncedText[mapping] != s
        }
        else -> false
    }

private fun recordLastSynced(
    syncState: RcDocSyncState,
    mapping: MapRcDocVariables,
    varType: Int,
    value: Any?,
) {
    when (varType) {
        NamedVariable.FLOAT_TYPE ->
            (value as? Float)?.let { syncState.lastSyncedFloat[mapping] = it }
        NamedVariable.INT_TYPE -> (value as? Int)?.let { syncState.lastSyncedInt[mapping] = it }
        NamedVariable.COLOR_TYPE -> (value as? Int)?.let { syncState.lastSyncedColor[mapping] = it }
        NamedVariable.STRING_TYPE ->
            (value as? String)?.let { syncState.lastSyncedText[mapping] = it }
    }
}
