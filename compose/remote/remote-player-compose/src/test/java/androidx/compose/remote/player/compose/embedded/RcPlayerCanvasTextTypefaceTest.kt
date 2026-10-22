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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Typeface
import androidx.collection.emptyIntObjectMap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintPathEffects
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.doc.SnapshotRemoteComposeState
import androidx.compose.remote.player.compose.embedded.draw.ComposeLocalPaint
import androidx.compose.remote.player.compose.embedded.draw.updatePaintFromBundle
import androidx.compose.remote.player.compose.embedded.resources.RcTypefaceCache
import androidx.compose.remote.player.compose.embedded.resources.applyVariationSettings
import androidx.compose.remote.player.compose.embedded.resources.rcTypefaceResolver
import androidx.compose.remote.player.compose.embedded.resources.resolveFontFamily
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.LoadedFontFamily
import androidx.compose.ui.unit.Density
import androidx.core.provider.FontRequest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerCanvasTextTypefaceTest {

    @get:Rule val rule = RcPlayerTestRule()

    private class CustomTestTypefaceResolver(val expectedTypeface: Typeface) : RcTypefaceResolver {
        var resolveCalled = false
        var lastRequest: RcTypefaceRequest? = null
        val requests = mutableListOf<RcTypefaceRequest>()

        override suspend fun resolve(request: RcTypefaceRequest): Typeface {
            resolveCalled = true
            lastRequest = request
            requests.add(request)
            return applyVariationSettings(expectedTypeface, request.variationSettings)
        }
    }

    @Test
    fun toNativeTextPaintUsesContextTypefaceResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { rcTypefaceResolver = testResolver }

        val paint = ComposeLocalPaint().apply { textSize = 24f }

        val nativePaint = paint.toNativeTextPaint(context)

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(nativePaint.typeface).isEqualTo(testTypeface)
    }

    @Test
    fun toNativeTextPaintWithGraphContextUsesConfiguredTypefaceResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val snapshotState = SnapshotRemoteComposeState()
        val graphContext =
            GraphContext(
                    snapshotState,
                    emptyIntObjectMap(),
                    mutableFloatStateOf(0f),
                    RemoteClock.SYSTEM,
                )
                .apply { setTypefaceResolver(testResolver) }

        val paint = ComposeLocalPaint().apply { textSize = 24f }
        val nativePaint = paint.toNativeTextPaint(graphContext)

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(nativePaint.typeface).isEqualTo(testTypeface)
    }

    @Test
    fun toTextStyleUsesContextTypefaceResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { rcTypefaceResolver = testResolver }

        val paint = ComposeLocalPaint().apply { textSize = 24f }
        val textStyle = paint.toTextStyle(Density(1f), context)

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(textStyle.fontFamily).isNotNull()
    }

    @Test
    fun toTextStylePreservesFontWeightAndStyleWhenFontAxisIsSet() {
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        context.loadText(10, "device:sans-serif")
        context.loadText(20, "wdth")

        val bundle =
            PaintBundle().apply {
                setTextStyle(10, 700, true)
                setTextAxis(intArrayOf(20), floatArrayOf(85f))
                setTextSize(18f)
            }
        val paint = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paint, context)

        val textStyle = paint.toTextStyle(Density(1f), context)
        assertThat(textStyle.fontWeight).isEqualTo(FontWeight(700))
        assertThat(textStyle.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(textStyle.fontFamily).isNotNull()
    }

    @Test
    fun toDrawStylePopulatesStrokeMiterAndPathEffectFromPaintBundle() {
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val bundle =
            PaintBundle().apply {
                setStyle(PaintBundle.STYLE_STROKE)
                setStrokeWidth(6f)
                setStrokeMiter(8.5f)
                setPathEffect(PaintPathEffects.dash(0f, 10f, 5f))
            }
        val paint = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paint, context)

        val style = paint.toDrawStyle()
        assertThat(style).isInstanceOf(Stroke::class.java)
        val stroke = style as Stroke
        assertThat(stroke.width).isEqualTo(6f)
        assertThat(stroke.miter).isEqualTo(8.5f)
        assertThat(stroke.pathEffect).isNotNull()
    }

    @Test
    fun customTypefaceResolverAppliesFontVariationSettings() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { rcTypefaceResolver = testResolver }
        context.loadText(20, "wght")
        context.loadText(21, "wdth")

        resolveFontFamily(
            fontFamilyType = 0,
            fontName = "default",
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
            fontAxis = intArrayOf(20, 21),
            fontAxisValues = floatArrayOf(100f, 150f),
            context = context,
            typefaceResolver = testResolver,
        )

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(testResolver.lastRequest?.variationSettings)
            .isEqualTo("'wght' 100.0, 'wdth' 150.0")
    }

    @Test
    fun toNativeTextPaintAppliesFontVariationSettingsViaResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { rcTypefaceResolver = testResolver }
        context.loadText(20, "wght")
        context.loadText(21, "wdth")

        val bundle =
            PaintBundle().apply {
                setTextStyle(PaintBundle.FONT_TYPE_SANS_SERIF, 500, false)
                setTextAxis(intArrayOf(20, 21), floatArrayOf(650f, 90f))
                setTextSize(24f)
            }
        val paint = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paint, context)

        val nativePaint = paint.toNativeTextPaint(context)

        assertThat(nativePaint.typeface).isNotNull()
        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(testResolver.lastRequest?.variationSettings)
            .isEqualTo("'wght' 650.0, 'wdth' 90.0")
    }

    @Test
    fun customTypefaceResolverAppliesTnumAndPnumSettings() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { rcTypefaceResolver = testResolver }
        context.loadText(20, "wght")
        context.loadText(21, "tnum")
        context.loadText(22, "pnum")

        resolveFontFamily(
            fontFamilyType = 0,
            fontName = "default",
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
            fontAxis = intArrayOf(20, 21),
            fontAxisValues = floatArrayOf(500f, 1f),
            context = context,
            typefaceResolver = testResolver,
        )

        assertThat(testResolver.lastRequest?.variationSettings).isEqualTo("'wght' 500.0")

        val bundle =
            PaintBundle().apply {
                setTextStyle(PaintBundle.FONT_TYPE_SANS_SERIF, 400, false)
                setTextAxis(intArrayOf(20, 22), floatArrayOf(500f, 1f))
                setTextSize(24f)
            }
        val paint = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paint, context)
        val nativePaint = paint.toNativeTextPaint(context)
        val textStyle = paint.toTextStyle(Density(1f), context)

        assertThat(testResolver.lastRequest?.variationSettings).isEqualTo("'wght' 500.0")
        assertThat(nativePaint.fontFeatureSettings).isEqualTo("'pnum' 1")
        assertThat(textStyle.fontFeatureSettings).isEqualTo("'pnum' 1")
    }

    @Test
    fun remoteTextWithTnumAndPnumAppliesSettingsThroughPlayer() {
        val testResolver = CustomTestTypefaceResolver(Typeface.SANS_SERIF)

        rule.setRemoteContent(typefaceResolver = testResolver) {
            RemoteColumn {
                RemoteText(
                    text = "tnum: 11111".rs,
                    style =
                        RemoteTextStyle(
                            fontSize = 18.rsp,
                            fontFeatureSettings = "tnum",
                            fontVariationSettings =
                                FontVariation.Settings(FontVariation.weight(500)),
                        ),
                )
                RemoteText(
                    text = "pnum: 11111".rs,
                    style =
                        RemoteTextStyle(
                            fontSize = 18.rsp,
                            fontFeatureSettings = "pnum",
                            fontVariationSettings =
                                FontVariation.Settings(FontVariation.width(110f)),
                        ),
                )
            }
        }

        rule.onNodeWithText("tnum: 11111").assertIsDisplayed()
        rule.onNodeWithText("pnum: 11111").assertIsDisplayed()
        val tnumLayout = mutableListOf<TextLayoutResult>()
        val pnumLayout = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText("tnum: 11111")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(tnumLayout)
        rule
            .onNodeWithText("pnum: 11111")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(pnumLayout)
        assertThat(tnumLayout.single().layoutInput.style.fontFeatureSettings).isEqualTo("'tnum' 1")
        assertThat(pnumLayout.single().layoutInput.style.fontFeatureSettings).isEqualTo("'pnum' 1")
        assertThat(tnumLayout.single().size.width).isGreaterThan(pnumLayout.single().size.width)
        assertThat(testResolver.requests.map { it.variationSettings })
            .containsAtLeast("'wght' 500.0", "'wdth' 110.0")
    }

    @Test
    fun gmsFontTypefaceResolverConstructs5ArgFontRequestWithVariationSettings() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        var capturedRequest: FontRequest? = null
        val resolver =
            GmsFontTypefaceResolver(
                context = context,
                fontCertsResId = 123,
                fontLoader = { _, fontRequest ->
                    capturedRequest = fontRequest
                    Typeface.MONOSPACE
                },
            )

        val resolved =
            resolver.resolve(
                RcTypefaceRequest(
                    fontFamily = "google:Roboto Flex",
                    weight = 600,
                    italic = true,
                    variationSettings = "'wght' 600.0, 'wdth' 110.0",
                )
            )

        assertThat(resolved).isNotNull()
        assertThat(capturedRequest).isNotNull()
        assertThat(capturedRequest!!.providerAuthority).isEqualTo("com.google.android.gms.fonts")
        assertThat(capturedRequest!!.providerPackage).isEqualTo("com.google.android.gms")
        assertThat(capturedRequest!!.query)
            .isEqualTo("name=Roboto Flex&weight=600&italic=1&besteffort=true")
        assertThat(capturedRequest!!.certificatesArrayResId).isEqualTo(123)
        assertThat(capturedRequest!!.variationSettings).isEqualTo("'wght' 600.0, 'wdth' 110.0")
    }

    @Test
    fun gmsFontTypefaceResolverCachesFailedRequestsWithoutRetryingFontLoader() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        var loadAttempts = 0
        val resolver =
            GmsFontTypefaceResolver(
                context = context,
                fontCertsResId = 123,
                fontLoader = { _, _ ->
                    loadAttempts++
                    null
                },
            )
        val request =
            RcTypefaceRequest(
                fontFamily = "google:Roboto Flex",
                weight = 600,
                italic = false,
                variationSettings = "'wght' 600.0",
            )

        val first = resolver.resolve(request)
        val second = resolver.resolve(request)

        assertThat(first).isNotNull()
        assertThat(second).isNotNull()
        assertThat(loadAttempts).isEqualTo(1)
    }

    @Test
    fun defaultGmsFontLoaderShortCircuitsWithoutProviderOrBackgroundThread() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val resolver = GmsFontTypefaceResolver(context = context, fontCertsResId = 123)
        val cache = RcTypefaceCache(resolver)
        val request =
            RcTypefaceRequest(
                fontFamily = "google:Roboto Flex",
                weight = 500,
                italic = false,
                variationSettings = "'wght' 500.0",
            )

        // When the GMS font provider is absent (such as in Android Studio Previews / Robolectric),
        // DefaultGmsFontLoader must short-circuit synchronously without suspending or spawning the
        // "fonts-androidx" FontRequestWorker background thread.
        val resolvedSynchronously = cache.resolve(request)

        assertThat(resolvedSynchronously).isNotNull()
        val threadNames = Thread.getAllStackTraces().keys.map { it.name }
        assertThat(threadNames).doesNotContain("fonts-androidx")
    }

    @Test
    fun synchronousTypefaceResolverPerformsZeroSnapshotReadsOrWrites() {
        val syncResolver = RcTypefaceResolver { request ->
            applyVariationSettings(Typeface.MONOSPACE, request.variationSettings)
        }
        val cache = RcTypefaceCache(syncResolver)
        val request =
            RcTypefaceRequest(
                fontFamily = "roboto-flex",
                weight = 600,
                italic = false,
                variationSettings = "'wght' 600.0",
            )

        var snapshotReads = 0
        var snapshotWrites = 0
        val resolved =
            Snapshot.observe(
                readObserver = { snapshotReads++ },
                writeObserver = { snapshotWrites++ },
            ) {
                cache.resolve(request)
            }

        assertThat(resolved).isNotNull()
        assertThat(snapshotReads).isEqualTo(0)
        assertThat(snapshotWrites).isEqualTo(0)
    }

    @OptIn(InternalComposeApi::class)
    @Test
    fun rcPlayerWithCustomTypefaceResolverLeavesNoActiveLaunchedCoroutines() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val documentBytes = runBlocking {
            captureSingleRemoteDocument(context = context) {
                    RemoteText(
                        text = "Static Preview Text".rs,
                        style =
                            RemoteTextStyle(
                                fontFamily = RemoteFontFamily.Named("google:Roboto Flex"),
                                fontSize = 16.rsp,
                                fontVariationSettings =
                                    FontVariation.Settings(FontVariation.weight(500)),
                            ),
                    )
                }
                .bytes
        }
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(documentBytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        val customResolver = RcTypefaceResolver { request ->
            applyVariationSettings(Typeface.MONOSPACE, request.variationSettings)
        }

        var showPlayer by mutableStateOf(false)
        var recomposerEffectJob: Job? = null
        rule.setContent {
            recomposerEffectJob = currentComposer.applyCoroutineContext[Job]
            if (showPlayer) {
                RcPlayer(document = document, typefaceResolver = customResolver)
            }
        }
        rule.waitForIdle()
        val baselineActiveChildren = recomposerEffectJob!!.children.count { it.isActive }

        showPlayer = true
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(64)
        rule.waitForIdle()

        rule.onNodeWithText("Static Preview Text").assertIsDisplayed()
        assertThat(recomposerEffectJob!!.children.count { it.isActive })
            .isEqualTo(baselineActiveChildren)
    }

    @Test
    fun disposingRcPlayerCancelsInFlightTypefaceResolutionAndReattachingRestartsIt() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val documentBytes = runBlocking {
            captureSingleRemoteDocument(context = context) {
                    RemoteText(
                        text = "Disposable Async Font".rs,
                        style =
                            RemoteTextStyle(
                                fontFamily = RemoteFontFamily.Named("google:Roboto Flex"),
                                fontSize = 16.rsp,
                            ),
                    )
                }
                .bytes
        }
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(documentBytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        val state = RcPlayerState(document)

        var startCount = 0
        var cancelledCount = 0
        var activeContinuation: Continuation<Typeface?>? = null
        val asyncResolver = RcTypefaceResolver {
            startCount++
            suspendCancellableCoroutine { continuation ->
                activeContinuation = continuation
                continuation.invokeOnCancellation { cancelledCount++ }
            }
        }

        var showPlayer by mutableStateOf(true)
        rule.setContent {
            if (showPlayer) {
                RcPlayer(state = state, typefaceResolver = asyncResolver)
            }
        }
        rule.waitForIdle()
        assertThat(startCount).isEqualTo(1)
        assertThat(cancelledCount).isEqualTo(0)

        // Disposing RcPlayer must cancel the in-flight typeface resolution job.
        showPlayer = false
        rule.waitForIdle()
        assertThat(cancelledCount).isEqualTo(1)

        // Re-mounting RcPlayer with the same state restarts the pending request and updates text.
        showPlayer = true
        rule.waitForIdle()
        assertThat(startCount).isEqualTo(2)

        activeContinuation!!.resume(Typeface.MONOSPACE)
        rule.waitForIdle()

        val afterLayout = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText("Disposable Async Font")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(afterLayout)
        assertThat(afterLayout.single().layoutInput.style.fontFamily)
            .isInstanceOf(LoadedFontFamily::class.java)
    }

    @Test
    fun suspendingTypefaceResolverUpdatesPlayerWhenAsyncFontArrives() {
        val deferred = CompletableDeferred<Typeface?>()
        val capturedRequests = mutableListOf<RcTypefaceRequest>()
        val asyncResolver = RcTypefaceResolver { request ->
            capturedRequests.add(request)
            deferred.await()
        }

        rule.setRemoteContent(typefaceResolver = asyncResolver) {
            RemoteText(
                text = "Async Variable Font".rs,
                style =
                    RemoteTextStyle(
                        fontFamily = RemoteFontFamily.Named("google:Roboto Flex"),
                        fontSize = 18.rsp,
                        fontVariationSettings =
                            FontVariation.Settings(
                                FontVariation.weight(500),
                                FontVariation.width(120f),
                            ),
                    ),
            )
        }

        rule.onNodeWithText("Async Variable Font").assertIsDisplayed()
        assertThat(capturedRequests).hasSize(1)
        assertThat(capturedRequests.single().fontFamily).isEqualTo("google:Roboto Flex")
        assertThat(capturedRequests.single().variationSettings)
            .isEqualTo("'wght' 500.0, 'wdth' 120.0")

        // Before completion, fallback font family is used.
        val beforeLayout = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText("Async Variable Font")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(beforeLayout)
        val initialFontFamily: FontFamily? = beforeLayout.single().layoutInput.style.fontFamily
        assertThat(initialFontFamily).isNotInstanceOf(LoadedFontFamily::class.java)

        // Complete the async typeface resolution and verify recomposition updates the FontFamily.
        deferred.complete(Typeface.MONOSPACE)
        rule.waitForIdle()

        val afterLayout = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText("Async Variable Font")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(afterLayout)
        val resolvedFontFamily: FontFamily? = afterLayout.single().layoutInput.style.fontFamily
        assertThat(resolvedFontFamily).isInstanceOf(LoadedFontFamily::class.java)
    }

    @Test
    fun coreTypefaceResolverAdapterReturnsFallbackUntilAsyncTypefaceLoads() {
        val deferred = CompletableDeferred<Typeface?>()
        val capturedRequests = mutableListOf<RcTypefaceRequest>()
        val asyncResolver = RcTypefaceResolver { request ->
            capturedRequests.add(request)
            deferred.await()
        }
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { rcTypefaceResolver = asyncResolver }

        val coreAdapter = context.typefaceResolver
        val fontInstance =
            coreAdapter.resolve(
                "google:Roboto Flex",
                500,
                false,
                Typeface.SERIF,
                400,
                false,
            )

        var loadedNotified = false
        val initialTypeface =
            fontInstance.applyVariationSettings(
                arrayOf("wght", "wdth", "tnum"),
                floatArrayOf(600f, 110f, 1f),
            )
        fontInstance.setOnLoadedListener { loadedNotified = true }

        // Immediately returns the fallback typeface while the suspending resolver is pending,
        // and forwards the variation settings (excluding OpenType feature tag "tnum").
        assertThat(initialTypeface).isNotNull()
        assertThat(loadedNotified).isFalse()
        assertThat(capturedRequests).hasSize(1)
        assertThat(capturedRequests.single().fontFamily).isEqualTo("google:Roboto Flex")
        assertThat(capturedRequests.single().variationSettings)
            .isEqualTo("'wght' 600.0, 'wdth' 110.0")

        // Once the suspending resolver completes, the listener is notified and the cached resolved
        // typeface is returned on subsequent calls.
        deferred.complete(Typeface.MONOSPACE)
        assertThat(loadedNotified).isTrue()
        val resolvedTypeface =
            fontInstance.applyVariationSettings(
                arrayOf("wght", "wdth", "tnum"),
                floatArrayOf(600f, 110f, 1f),
            )
        val resolvedTypefaceSecondCall =
            fontInstance.applyVariationSettings(
                arrayOf("wght", "wdth", "tnum"),
                floatArrayOf(600f, 110f, 1f),
            )
        assertThat(resolvedTypeface).isNotNull()
        assertThat(resolvedTypeface).isNotEqualTo(initialTypeface)
        assertThat(resolvedTypefaceSecondCall).isSameInstanceAs(resolvedTypeface)
    }
}
