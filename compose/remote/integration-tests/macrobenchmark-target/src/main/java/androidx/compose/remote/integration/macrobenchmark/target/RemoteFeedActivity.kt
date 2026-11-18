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

package androidx.compose.remote.integration.macrobenchmark.target

import android.content.Intent
import android.os.Bundle
import android.util.LruCache
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.rememberRcPlayerState
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Target activity for feed, animation, parse-strategy, and late-content macrobenchmarks (F1, F2,
 * F3, A1, A2, S2b, P1).
 */
class RemoteFeedActivity : ComponentActivity() {

    @OptIn(ExperimentalRemotePlayerApi::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = FeedConfig.fromIntent(intent)
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = config.embeddedFlag

        setContent { FeedScreen(config) }
    }
}

private enum class FeedPlayer {
    Embedded,
    View,
    DocumentPlayerEmbedded,
    DocumentPlayerView,
    Compose,
    Empty,
}

private enum class ParseStrategy {
    Composition,
    Ahead,
    Prepared,
}

private data class FeedConfig(
    val player: FeedPlayer,
    val embeddedFlag: Boolean,
    val templates: List<BenchTemplate>,
    val count: Int,
    val parse: ParseStrategy,
    val ahead: Int,
    val cacheWindow: Boolean,
    val updateMs: Long,
    val eased: Boolean,
    val fetchDelayMs: Long,
) {
    fun templateFor(index: Int): BenchTemplate = templates[index % templates.size]

    companion object {
        fun fromIntent(intent: Intent): FeedConfig {
            val playerArg = intent.getStringExtra("player") ?: "embedded"
            val (player, embeddedFlag) =
                when (playerArg) {
                    "embedded",
                    MODE_EMBEDDED_PLAYER -> FeedPlayer.Embedded to true
                    "view",
                    MODE_VIEW_PLAYER,
                    MODE_JAVA_PLAYER -> FeedPlayer.View to false
                    "documentPlayer_embedded",
                    MODE_DOCUMENT_PLAYER_EMBEDDED -> FeedPlayer.DocumentPlayerEmbedded to true
                    "documentPlayer_view",
                    MODE_DOCUMENT_PLAYER_VIEW -> FeedPlayer.DocumentPlayerView to false
                    "compose",
                    MODE_COMPOSE -> FeedPlayer.Compose to false
                    "empty",
                    MODE_EMPTY -> FeedPlayer.Empty to false
                    else -> FeedPlayer.Embedded to true
                }
            val templates =
                when (intent.getStringExtra("templates") ?: "single") {
                    "single" -> listOf(BenchTemplate.AppCard)
                    "mixed" ->
                        listOf(
                            BenchTemplate.AppCard,
                            BenchTemplate.Headline,
                            BenchTemplate.Poll,
                            BenchTemplate.Stateful,
                        )
                    "live" ->
                        listOf(BenchTemplate.AppCard, BenchTemplate.AppCard, BenchTemplate.Live)
                    "poll" ->
                        listOf(BenchTemplate.AppCard, BenchTemplate.AppCard, BenchTemplate.Poll)
                    "stateful" -> listOf(BenchTemplate.Stateful)
                    else -> listOf(BenchTemplate.AppCard)
                }
            val parse =
                when (intent.getStringExtra("parse") ?: "composition") {
                    "ahead" -> ParseStrategy.Ahead
                    "prepared" -> ParseStrategy.Prepared
                    else -> ParseStrategy.Composition
                }
            return FeedConfig(
                player = player,
                embeddedFlag = embeddedFlag,
                templates = templates,
                count = intent.getIntExtra("count", 200),
                parse = parse,
                ahead = intent.getIntExtra("ahead", 8),
                cacheWindow = intent.getBooleanExtra("cacheWindow", false),
                updateMs = intent.getLongExtra("updateMs", 0L),
                eased = intent.getBooleanExtra("eased", false),
                fetchDelayMs = intent.getLongExtra("fetchDelayMs", 0L),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedScreen(config: FeedConfig) {
    val context = LocalContext.current
    var templateBytes by remember { mutableStateOf<Map<BenchTemplate, ByteArray>?>(null) }

    LaunchedEffect(config.templates, config.player) {
        if (config.player != FeedPlayer.Compose && config.player != FeedPlayer.Empty) {
            val useGlobalScope =
                config.player == FeedPlayer.View || config.player == FeedPlayer.DocumentPlayerView
            templateBytes =
                loadFeedTemplates(context, config.templates, globalScope = useGlobalScope)
        }
    }

    val listState =
        if (config.cacheWindow) {
            @Suppress("DEPRECATION")
            rememberLazyListState(
                cacheWindow = LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 0.5f)
            )
        } else {
            rememberLazyListState()
        }

    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(config.updateMs) {
        if (config.updateMs > 0L) {
            while (isActive) {
                delay(config.updateMs)
                tick++
            }
        }
    }

    val parsedCache = remember { LruCache<Int, CoreDocument>(64) }
    val preparedCache = remember { LruCache<Int, RemoteComposePlayer.PreparedDocument>(64) }
    val preparedPlayer =
        remember(config.parse) {
            if (config.parse == ParseStrategy.Prepared) RemoteComposePlayer(context) else null
        }

    // Track per-card fetch readiness when fetchDelayMs > 0
    val readyCards = remember(config.count) { BooleanArray(config.count) }
    var readyVersion by remember { mutableIntStateOf(0) }
    val allVisibleCardsReady by remember {
        derivedStateOf {
            @Suppress("UNUSED_EXPRESSION") readyVersion
            val loaded =
                templateBytes != null ||
                    config.player == FeedPlayer.Compose ||
                    config.player == FeedPlayer.Empty
            if (!loaded) return@derivedStateOf false
            if (config.fetchDelayMs <= 0L) return@derivedStateOf true
            val visible = listState.layoutInfo.visibleItemsInfo
            visible.isNotEmpty() && visible.all { readyCards.getOrElse(it.index) { false } }
        }
    }
    ReportDrawnWhen { allVisibleCardsReady }

    // Background parse-ahead / prepare-ahead (F2 / P1)
    LaunchedEffect(templateBytes, config.parse, config.ahead, config.fetchDelayMs) {
        val bytesMap = templateBytes ?: return@LaunchedEffect
        if (config.parse == ParseStrategy.Composition && config.fetchDelayMs <= 0L)
            return@LaunchedEffect

        var lastFirstVisible = -1
        while (isActive) {
            val firstVisible = listState.firstVisibleItemIndex
            if (firstVisible != lastFirstVisible) {
                lastFirstVisible = firstVisible
                val end = (firstVisible + config.ahead + 8).coerceAtMost(config.count - 1)
                for (idx in firstVisible..end) {
                    val template = config.templateFor(idx)
                    val bytes = bytesMap[template] ?: continue
                    if (config.fetchDelayMs > 0L && !readyCards[idx]) {
                        launch {
                            delay(config.fetchDelayMs)
                            if (
                                config.parse == ParseStrategy.Ahead && parsedCache.get(idx) == null
                            ) {
                                withContext(Dispatchers.Default) {
                                    parsedCache.put(idx, RemoteDocument(bytes).document)
                                }
                            }
                            if (!readyCards[idx]) {
                                readyCards[idx] = true
                                readyVersion++
                            }
                        }
                    } else if (
                        config.parse == ParseStrategy.Ahead && parsedCache.get(idx) == null
                    ) {
                        withContext(Dispatchers.Default) {
                            parsedCache.put(idx, RemoteDocument(bytes).document)
                        }
                    } else if (
                        config.parse == ParseStrategy.Prepared &&
                            preparedPlayer != null &&
                            preparedCache.get(idx) == null
                    ) {
                        withContext(Dispatchers.Default) {
                            val doc = RemoteDocument(bytes)
                            preparedPlayer.prepareDocument(doc)?.let { preparedCache.put(idx, it) }
                        }
                    }
                }
            }
            delay(16L)
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            Modifier.fillMaxSize().semantics { contentDescription = FEED_CONTENT_DESCRIPTION },
    ) {
        items(count = config.count, key = { it }) { index ->
            val template = config.templateFor(index)
            val isReady =
                if (config.fetchDelayMs <= 0L) {
                    true
                } else {
                    @Suppress("UNUSED_EXPRESSION") readyVersion
                    val ready = readyCards[index]
                    if (!ready) {
                        LaunchedEffect(index) {
                            delay(config.fetchDelayMs)
                            if (!readyCards[index]) {
                                readyCards[index] = true
                                readyVersion++
                            }
                        }
                    }
                    ready
                }

            if (!isReady) {
                PlaceholderCard(index)
                return@items
            }

            trace("RemoteBench:card:${template.key}") {
                when (config.player) {
                    FeedPlayer.Empty -> EmptyCard()
                    FeedPlayer.Compose -> {
                        val s0 = shareFor(index, tick, 0)
                        val s1 = shareFor(index, tick, 1)
                        val s2 = shareFor(index, tick, 2)
                        val anim0 =
                            if (config.eased) animateFloatAsState(s0, tween(300)).value else s0
                        val anim1 =
                            if (config.eased) animateFloatAsState(s1, tween(300)).value else s1
                        val anim2 =
                            if (config.eased) animateFloatAsState(s2, tween(300)).value else s2
                        ComposeTwinCard(
                            template = template,
                            share0 = anim0,
                            share1 = anim1,
                            share2 = anim2,
                            stateIndex = tick + index,
                        )
                    }
                    FeedPlayer.Embedded -> {
                        val bytes = templateBytes?.get(template)
                        if (bytes == null) {
                            PlaceholderCard(index)
                        } else {
                            val doc =
                                remember(index, bytes) {
                                    parsedCache.get(index)
                                        ?: trace("CreateRemoteDocument:parsing") {
                                            RemoteDocument(bytes).document
                                        }
                                }
                            EmbeddedFeedCard(
                                doc = doc,
                                template = template,
                                index = index,
                                tick = tick,
                                eased = config.eased,
                            )
                        }
                    }
                    FeedPlayer.View -> {
                        val bytes = templateBytes?.get(template)
                        if (bytes == null) {
                            PlaceholderCard(index)
                        } else {
                            ViewFeedCard(
                                bytes = bytes,
                                prepared = preparedCache.get(index),
                                template = template,
                                index = index,
                                tick = tick,
                            )
                        }
                    }
                    FeedPlayer.DocumentPlayerEmbedded,
                    FeedPlayer.DocumentPlayerView -> {
                        val bytes = templateBytes?.get(template)
                        if (bytes == null) {
                            PlaceholderCard(index)
                        } else {
                            DocumentPlayerFeedCard(bytes = bytes, index = index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddedFeedCard(
    doc: CoreDocument,
    template: BenchTemplate,
    index: Int,
    tick: Int,
    eased: Boolean,
) {
    val state = rememberRcPlayerState(doc)
    if ((template == BenchTemplate.Poll || template == BenchTemplate.Live) && tick > 0) {
        val target0 = shareFor(index, tick, 0)
        val target1 = shareFor(index, tick, 1)
        val target2 = shareFor(index, tick, 2)
        if (eased) {
            val anim0 by animateFloatAsState(target0, tween(300))
            val anim1 by animateFloatAsState(target1, tween(300))
            val anim2 by animateFloatAsState(target2, tween(300))
            LaunchedEffect(anim0, anim1, anim2) {
                trace("RemoteBench:hostUpdate") {
                    state.floatState("share0").value = anim0
                    state.floatState("share1").value = anim1
                    state.floatState("share2").value = anim2
                }
            }
        } else {
            LaunchedEffect(tick) {
                trace("RemoteBench:hostUpdate") {
                    state.floatState("share0").value = target0
                    state.floatState("share1").value = target1
                    state.floatState("share2").value = target2
                }
            }
        }
    } else if (template == BenchTemplate.Stateful && tick > 0) {
        LaunchedEffect(tick) {
            trace("RemoteBench:hostUpdate") { state.intState("state").value = (tick + index) % 3 }
        }
    }
    RcPlayer(state = state, modifier = Modifier.fillMaxWidth().height(FEED_CARD_HEIGHT))
}

@Composable
private fun ViewFeedCard(
    bytes: ByteArray,
    prepared: RemoteComposePlayer.PreparedDocument?,
    template: BenchTemplate,
    index: Int,
    tick: Int,
) {
    val remoteDoc =
        remember(index, bytes, prepared) {
            prepared?.originalDoc ?: trace("CreateRemoteDocument:parsing") { RemoteDocument(bytes) }
        }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(FEED_CARD_HEIGHT),
        factory = { context ->
            RemoteComposePlayer(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        },
        update = { player ->
            player.setTheme(Theme.LIGHT)
            if (prepared != null) {
                player.setPreparedDocument(prepared)
            } else {
                player.setDocument(remoteDoc)
            }
            if ((template == BenchTemplate.Poll || template == BenchTemplate.Live) && tick > 0) {
                trace("RemoteBench:hostUpdate") {
                    repeat(3) { i ->
                        player.setUserLocalFloat("share$i", shareFor(index, tick, i))
                    }
                    invalidateViewTree(player)
                }
            } else if (template == BenchTemplate.Stateful && tick > 0) {
                trace("RemoteBench:hostUpdate") {
                    player.setUserLocalInt("state", (tick + index) % 3)
                    invalidateViewTree(player)
                }
            }
        },
    )
}

@OptIn(ExperimentalRemotePlayerApi::class)
@Composable
private fun DocumentPlayerFeedCard(bytes: ByteArray, index: Int) {
    val windowInfo = LocalWindowInfo.current
    val remoteDoc =
        remember(index, bytes) {
            trace("CreateRemoteDocument:parsing") { RemoteDocument(bytes) }
        }
    RemoteDocumentPlayer(
        document = remoteDoc.document,
        documentWidth = windowInfo.containerSize.width,
        documentHeight = windowInfo.containerSize.height,
        modifier = Modifier.fillMaxWidth().height(FEED_CARD_HEIGHT),
    )
}

@Composable
private fun PlaceholderCard(index: Int) {
    trace("RemoteBench:placeholder") {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(FEED_CARD_HEIGHT)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB))
                    .padding(12.dp)
                    .semantics { contentDescription = "placeholder_$index" },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = "Loading card $index...", color = Color(0xFF6B7280))
        }
    }
}

@Composable
private fun EmptyCard() {
    Box(
        modifier =
            Modifier.fillMaxWidth().height(FEED_CARD_HEIGHT).semantics {
                contentDescription = "card_empty"
            }
    )
}

private fun shareFor(index: Int, tick: Int, bar: Int): Float {
    val phase = ((index + tick * 7 + bar * 13) % 10) + 1
    return 30f + phase * 8f
}
