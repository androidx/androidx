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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font as NativeFont
import android.graphics.fonts.FontFamily as NativeFontFamily
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintPathEffects
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.LoadedFontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.provider.FontRequest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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
            return applyVariations(expectedTypeface, request.variationSettings)
        }
    }

    @Test
    fun toNativeTextPaintUsesContextTypefaceResolver() {
        val monoResolver = CustomTestTypefaceResolver(Typeface.MONOSPACE)
        val sansResolver = CustomTestTypefaceResolver(loadKarlaTypeface())
        val sample = "iiiiWWWW"
        fun makeDoc(): CoreDocument = buildDocument {
            val textId = addText(sample)
            val pathId = addPathString("M 5 50 L 145 50")
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 150f, 80f)
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFF000000.toInt())
                    setTextSize(28f)
                }
            )
            drawTextOnPath(textId, pathId, 0f, 0f)
        }

        var currentResolver by mutableStateOf<RcTypefaceResolver>(monoResolver)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(150.dp, 80.dp)) {
                    key(currentResolver) {
                        RcPlayer(document = makeDoc(), typefaceResolver = currentResolver)
                    }
                }
            }
        }
        val monoPixels = rule.captureRootPixels(150, 80)
        assertThat(monoResolver.resolveCalled).isTrue()

        currentResolver = sansResolver
        rule.waitForIdle()
        val sansPixels = rule.captureRootPixels(150, 80)
        assertThat(sansResolver.resolveCalled).isTrue()
        assertThat(monoPixels).isNotEqualTo(sansPixels)
    }

    @Test
    fun toNativeTextPaintWithGraphContextUsesConfiguredTypefaceResolver() {
        val monoResolver = CustomTestTypefaceResolver(Typeface.MONOSPACE)
        val sansResolver = CustomTestTypefaceResolver(Typeface.SANS_SERIF)
        val document = buildDocument {
            val sampleId = addText("iiiiiiWWWWWW")
            writer.buffer.addPaint(PaintBundle().apply { setTextSize(24f) })
            val measuredWidth = textMeasure(sampleId, 0)
            val widthTextId = createTextFromFloat(measuredWidth, 4, 1, 0)
            root {
                startTextComponent(
                    RecordingModifier().width(200f).height(60f),
                    widthTextId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    18f,
                    -1f,
                    -1f,
                    0,
                    400f,
                    null,
                    CoreText.TEXT_ALIGN_START,
                    CoreText.OVERFLOW_CLIP,
                    1,
                    0f,
                    0f,
                    1f,
                    0,
                    0,
                    0,
                    false,
                    false,
                    null,
                    null,
                    false,
                    0,
                )
                endTextComponent()
            }
        }

        var currentResolver by mutableStateOf<RcTypefaceResolver>(monoResolver)
        rule.setContent {
            Box(Modifier.size(200.dp, 60.dp)) {
                RcPlayer(document = document, typefaceResolver = currentResolver)
            }
        }
        rule.waitForIdle()
        assertThat(monoResolver.resolveCalled).isTrue()

        currentResolver = sansResolver
        rule.waitForIdle()
        assertThat(sansResolver.resolveCalled).isTrue()
    }

    @Test
    fun toTextStyleUsesContextTypefaceResolver() {
        val testResolver = CustomTestTypefaceResolver(Typeface.MONOSPACE)
        val document = buildDocument {
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFF000000.toInt())
                    setTextSize(24f)
                }
            )
            drawTextAnchored("Anchored", 50f, 50f, 0f, 0f, 0)
        }

        rule.setContent {
            Box(Modifier.size(100.dp)) {
                RcPlayer(document = document, typefaceResolver = testResolver)
            }
        }
        rule.onRoot().captureToImage()

        assertThat(testResolver.resolveCalled).isTrue()
    }

    @Test
    fun toTextStylePreservesFontWeightAndStyleWhenFontAxisIsSet() {
        val document = buildDocument {
            val textId = addText("Weighted Italic Axis")
            root {
                startTextComponent(
                    RecordingModifier().width(200f).height(80f),
                    textId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    18f,
                    -1f,
                    -1f,
                    1, // italic
                    700f, // weight
                    "device:sans-serif",
                    CoreText.TEXT_ALIGN_START,
                    CoreText.OVERFLOW_CLIP,
                    1,
                    0f,
                    0f,
                    1f,
                    0,
                    0,
                    0,
                    false,
                    false,
                    arrayOf("wdth"),
                    floatArrayOf(85f),
                    false,
                    0,
                )
                endTextComponent()
            }
        }

        rule.setContent { Box(Modifier.size(200.dp, 80.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        val style = rule.textLayoutResultOf("Weighted Italic Axis").layoutInput.style
        assertThat(style.fontWeight).isEqualTo(FontWeight(700))
        assertThat(style.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(style.fontFamily).isNotNull()
    }

    @Test
    fun toDrawStylePopulatesStrokeMiterAndPathEffectFromPaintBundle() {
        val bitmap = rule.renderDocument {
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFFFF0000.toInt())
                    setStyle(PaintBundle.STYLE_STROKE)
                    setStrokeWidth(10f)
                    setStrokeMiter(8.5f)
                    setPathEffect(PaintPathEffects.dash(0f, 10f, 10f))
                }
            )
            drawLine(0f, 50f, 100f, 50f)
        }

        // Dash pattern 10px on (0..10, 20..30), 10px off (10..20, 30..40).
        assertRed(bitmap, 5, 50)
        assertNotRed(bitmap, 15, 50)
        assertRed(bitmap, 25, 50)
    }

    @Test
    fun customTypefaceResolverAppliesFontVariationSettings() {
        val testResolver = CustomTestTypefaceResolver(Typeface.MONOSPACE)
        val document = buildDocument {
            val textId = addText("Variation Text")
            root {
                startTextComponent(
                    RecordingModifier().width(200f).height(80f),
                    textId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    18f,
                    -1f,
                    -1f,
                    0,
                    400f,
                    "default",
                    CoreText.TEXT_ALIGN_START,
                    CoreText.OVERFLOW_CLIP,
                    1,
                    0f,
                    0f,
                    1f,
                    0,
                    0,
                    0,
                    false,
                    false,
                    arrayOf("wght", "wdth"),
                    floatArrayOf(100f, 150f),
                    false,
                    0,
                )
                endTextComponent()
            }
        }

        rule.setContent {
            Box(Modifier.size(200.dp, 80.dp)) {
                RcPlayer(document = document, typefaceResolver = testResolver)
            }
        }
        rule.waitForIdle()

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(testResolver.lastRequest?.variationSettings)
            .isEqualTo("'wght' 100.0, 'wdth' 150.0")
    }

    @Test
    fun toNativeTextPaintAppliesFontVariationSettingsViaResolver() {
        val testResolver = CustomTestTypefaceResolver(Typeface.MONOSPACE)
        val sample = "Canvas Axis"
        val document = buildDocument {
            val textId = addText(sample)
            val wghtId = addText("wght")
            val wdthId = addText("wdth")
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFF000000.toInt())
                    setTextStyle(PaintBundle.FONT_TYPE_SANS_SERIF, 500, false)
                    setTextAxis(intArrayOf(wghtId, wdthId), floatArrayOf(650f, 90f))
                    setTextSize(24f)
                }
            )
            drawTextRun(textId, 0, sample.length, 0, sample.length, 5f, 50f, false)
        }

        rule.setContent {
            Box(Modifier.size(150.dp, 80.dp)) {
                RcPlayer(document = document, typefaceResolver = testResolver)
            }
        }
        rule.onRoot().captureToImage()

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(testResolver.lastRequest?.variationSettings)
            .isEqualTo("'wght' 650.0, 'wdth' 90.0")
    }

    @Test
    fun customTypefaceResolverAppliesTnumAndPnumSettings() {
        val testResolver = CustomTestTypefaceResolver(Typeface.MONOSPACE)
        val document = buildDocument {
            val textId = addText("Feature Pnum")
            root {
                startTextComponent(
                    RecordingModifier().width(200f).height(80f),
                    textId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    24f,
                    -1f,
                    -1f,
                    0,
                    400f,
                    "default",
                    CoreText.TEXT_ALIGN_START,
                    CoreText.OVERFLOW_CLIP,
                    1,
                    0f,
                    0f,
                    1f,
                    0,
                    0,
                    0,
                    false,
                    false,
                    arrayOf("wght", "pnum"),
                    floatArrayOf(500f, 1f),
                    false,
                    0,
                )
                endTextComponent()
            }
        }

        rule.setContent {
            Box(Modifier.size(200.dp, 80.dp)) {
                RcPlayer(document = document, typefaceResolver = testResolver)
            }
        }
        rule.waitForIdle()

        assertThat(testResolver.lastRequest?.variationSettings).isEqualTo("'wght' 500.0")
        assertThat(rule.textLayoutResultOf("Feature Pnum").layoutInput.style.fontFeatureSettings)
            .isEqualTo("'pnum' 1")
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
        val tnumLayout = rule.textLayoutResultOf("tnum: 11111")
        val pnumLayout = rule.textLayoutResultOf("pnum: 11111")
        assertThat(tnumLayout.layoutInput.style.fontFeatureSettings).isEqualTo("'tnum' 1")
        assertThat(pnumLayout.layoutInput.style.fontFeatureSettings).isEqualTo("'pnum' 1")
        assertThat(tnumLayout.size.width).isGreaterThan(pnumLayout.size.width)
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

        // When the GMS font provider is absent (such as in Android Studio Previews / Robolectric),
        // DefaultGmsFontLoader must short-circuit synchronously without suspending or spawning the
        // "fonts-androidx" FontRequestWorker background thread.
        rule.setRemoteContent(typefaceResolver = resolver) {
            RemoteText(
                text = "GMS Preview Text".rs,
                style =
                    RemoteTextStyle(
                        fontFamily = RemoteFontFamily.Named("google:Roboto Flex"),
                        fontSize = 18.rsp,
                        fontVariationSettings = FontVariation.Settings(FontVariation.weight(500)),
                    ),
            )
        }

        rule.onNodeWithText("GMS Preview Text").assertIsDisplayed()
        val threadNames = Thread.getAllStackTraces().keys.map { it.name }
        assertThat(threadNames).doesNotContain("fonts-androidx")
    }

    @Test
    fun synchronousTypefaceResolverPerformsZeroSnapshotWritesDuringDraw() {
        var resolvedCount = 0
        val syncResolver = RcTypefaceResolver { request ->
            resolvedCount++
            applyVariations(Typeface.MONOSPACE, request.variationSettings)
        }
        val sample = "Sync Canvas Font"
        val document = buildDocument {
            val textId = addText(sample)
            val fontNameId = addText("roboto-flex")
            val wghtId = addText("wght")
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFF000000.toInt())
                    setTextStyle(fontNameId, 600, false)
                    setTextAxis(intArrayOf(wghtId), floatArrayOf(600f))
                    setTextSize(20f)
                }
            )
            drawTextRun(textId, 0, sample.length, 0, sample.length, 5f, 40f, false)
        }

        rule.setContent {
            Box(Modifier.size(150.dp, 60.dp)) {
                RcPlayer(document = document, typefaceResolver = syncResolver)
            }
        }
        rule.waitForIdle()

        var snapshotWrites = 0
        Snapshot.observe(writeObserver = { snapshotWrites++ }) { rule.onRoot().captureToImage() }

        assertThat(resolvedCount).isGreaterThan(0)
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
            applyVariations(Typeface.MONOSPACE, request.variationSettings)
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

        assertThat(rule.textLayoutResultOf("Disposable Async Font").layoutInput.style.fontFamily)
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
        val initialFontFamily: FontFamily? =
            rule.textLayoutResultOf("Async Variable Font").layoutInput.style.fontFamily
        assertThat(initialFontFamily).isNotInstanceOf(LoadedFontFamily::class.java)

        // Complete the async typeface resolution and verify recomposition updates the FontFamily.
        deferred.complete(Typeface.MONOSPACE)
        rule.waitForIdle()

        val resolvedFontFamily: FontFamily? =
            rule.textLayoutResultOf("Async Variable Font").layoutInput.style.fontFamily
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
        val playerState = RcPlayerState(CoreDocument(RemoteClock.SYSTEM))
        rule.setContent { RcPlayer(state = playerState, typefaceResolver = asyncResolver) }
        rule.waitForIdle()

        val context =
            playerState.inspectableElements.first { it.name == "remoteContext" }.value
                as AndroidRemoteContext
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

    private fun loadKarlaTypeface(): Typeface {
        val bytes =
            requireNotNull(javaClass.getResourceAsStream("/font/karla_regular.ttf")).use {
                it.readBytes()
            }
        val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { rewind() }
        val font = NativeFont.Builder(buffer).build()
        val family = NativeFontFamily.Builder(font).build()
        return Typeface.CustomFallbackBuilder(family).build()
    }

    private companion object {
        fun applyVariations(typeface: Typeface, variationSettings: String?): Typeface {
            if (variationSettings.isNullOrEmpty()) return typeface
            val paint = Paint()
            paint.typeface = typeface
            paint.fontVariationSettings = variationSettings
            return paint.typeface ?: typeface
        }
    }
}
