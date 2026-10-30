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

package androidx.compose.remote.integration.view.demos.dsl.embedded

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RcDocLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.CLAMP
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DIV
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.Action
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.actions.ValueStringChange
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

// Embedded child document URLs resolved by DemoRcResolver through resolveEmbeddedControlDoc().
private const val URL_CHILD_SLIDER = "embedded_child_slider.rc"
private const val URL_NESTED_CONTAINER = "embedded_nested_container.rc"
private const val URL_CHILD_BUTTONS = "embedded_child_buttons.rc"
private const val URL_CHILD_COUNTER = "embedded_child_counter.rc"
private const val URL_CHILD_NUMBER = "embedded_child_number.rc"
private const val URL_CHILD_CARD_A = "embedded_child_card_a.rc"
private const val URL_CHILD_CARD_B = "embedded_child_card_b.rc"
private const val URL_CHILD_CARD_C = "embedded_child_card_c.rc"
private const val URL_CHAIN_LEVEL_1 = "embedded_chain_level1.rc"
private const val URL_CHAIN_LEVEL_2 = "embedded_chain_level2.rc"

private const val ACTION_PICK = 2001

private const val BG_PARENT = 0xFF0F172A.toInt()
private const val BG_CHILDREN = 0xFF1E293B.toInt()
private const val BG_BUTTON = 0xFF334155.toInt()
private const val BG_CHILD = 0xFF172554.toInt()
private const val BORDER_CHILD = 0xFF60A5FA.toInt()
private const val BG_NESTED = 0xFF1E1B4B.toInt()
private const val BORDER_NESTED = 0xFFA78BFA.toInt()
private const val TRACK_PARENT = 0xFF334155.toInt()
private const val FILL_PARENT = 0xFF38BDF8.toInt()
private const val TRACK_CHILD = 0xFF0F172A.toInt()
private const val FILL_CHILD = 0xFF60A5FA.toInt()

private const val SLIDER_PAD = 40f
private const val THUMB_R = 30f

private val sCachedControlDocs = HashMap<String, ByteArray>()

/**
 * Resolves the embedded `.rc` child documents used by the nested-control demos into raw `.rc` wire
 * bytes for [androidx.compose.remote.core.RcResolver].
 */
@Suppress("RestrictedApiAndroidX")
fun resolveEmbeddedControlDoc(url: String): ByteArray? {
    val clean = url.substringAfterLast('/')
    val builder: (() -> RemoteComposeWriter)? =
        when (clean) {
            URL_CHILD_SLIDER -> ::buildChildSliderDoc
            URL_NESTED_CONTAINER -> ::buildNestedContainerDoc
            URL_CHILD_BUTTONS -> ::buildChildButtonsDoc
            URL_CHILD_COUNTER -> ::buildChildCounterDoc
            URL_CHILD_NUMBER -> ::buildChildNumberDoc
            URL_CHILD_CARD_A -> {
                { buildChildCardDoc("Child A", 0xFF7F1D1D.toInt(), 0xFFF87171.toInt()) }
            }
            URL_CHILD_CARD_B -> {
                { buildChildCardDoc("Child B", 0xFF14532D.toInt(), 0xFF4ADE80.toInt()) }
            }
            URL_CHILD_CARD_C -> {
                { buildChildCardDoc("Child C", 0xFF713F12.toInt(), 0xFFFACC15.toInt()) }
            }
            URL_CHAIN_LEVEL_1 -> {
                { buildChainLevelDoc("Child", URL_CHAIN_LEVEL_2) }
            }
            URL_CHAIN_LEVEL_2 -> {
                { buildChainLevelDoc("Grandchild", null) }
            }
            else -> null
        }
    if (builder == null) {
        return null
    }
    return sCachedControlDocs.getOrPut(clean) { builder().encodeToByteArray() }
}

// ============================================================================
// Shared building blocks
// ============================================================================

@Suppress("RestrictedApiAndroidX")
private fun newDoc(
    width: Int,
    height: Int,
    description: String,
    content: RemoteComposeContextAndroid.() -> Unit,
): RemoteComposeWriter {
    return RemoteComposeContextAndroid(
            width = width,
            height = height,
            contentDescription = description,
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
            content,
        )
        .writer
}

/** Parent page: dark background, a "Parent" header, then [content]. */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.parentPage(
    content: RemoteComposeContextAndroid.() -> Unit
) {
    root {
        column(
            Modifier.fillMaxSize().background(BG_PARENT).padding(24),
            horizontal = ColumnLayout.CENTER,
            vertical = ColumnLayout.TOP,
        ) {
            text("Parent", fontSize = 44f, fontWeight = 700f, color = Color.WHITE)
            content()
        }
    }
}

/** Padded, rounded container the parent draws around its embedded children. */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.childrenFrame(
    content: RemoteComposeContextAndroid.() -> Unit
) {
    column(
        Modifier.fillMaxWidth()
            .verticalWeight(1f)
            .padding(0, 24, 0, 0)
            .clip(RoundedRectShape(24f, 24f, 24f, 24f))
            .background(BG_CHILDREN)
            .padding(24),
        horizontal = ColumnLayout.CENTER,
        vertical = ColumnLayout.TOP,
    ) {
        content()
    }
}

/** Child page: a bordered card with a title read from the `"title"` named string. */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.childPage(
    titleId: Int,
    bg: Int = BG_CHILD,
    border: Int = BORDER_CHILD,
    content: RemoteComposeContextAndroid.() -> Unit,
) {
    root {
        column(
            Modifier.fillMaxSize()
                .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                .background(bg)
                .border(3f, 20f, border, 0)
                .padding(20),
            horizontal = ColumnLayout.START,
            vertical = ColumnLayout.TOP,
        ) {
            text(stringId = titleId, fontSize = 38f, fontWeight = 700f, color = 0xFFBFDBFE.toInt())
            content()
        }
    }
}

/** A big centered number showing [value], filling the remaining space. */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.bigNumber(value: Float, textSize: Float = 96f) {
    box(Modifier.fillMaxWidth().verticalWeight(1f)) {
        canvas(Modifier.fillMaxSize()) {
            val w = addComponentWidthValue()
            val h = addComponentHeightValue()
            val txt = createTextFromFloat(value, 3, 0, TextFromFloat.PAD_PRE_NONE)
            painter.setColor(Color.WHITE).setTextSize(textSize).commit()
            drawTextAnchored(
                txt,
                floatExpression(w, 0.5f, MUL),
                floatExpression(h, 0.5f, MUL),
                0f,
                0f,
                0,
            )
        }
    }
}

/** A big centered string. */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.bigText(stringId: Int, textSize: Float = 72f) {
    box(
        Modifier.fillMaxWidth().verticalWeight(1f),
        horizontal = BoxLayout.CENTER,
        vertical = BoxLayout.CENTER,
    ) {
        text(stringId = stringId, fontSize = textSize, fontWeight = 700f, color = Color.WHITE)
    }
}

/** Draws a horizontal slider track + thumb for `value` (0..100) centered on `cy`. */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.drawSlider(
    value: Float,
    w: Float,
    cy: Float,
    trackColor: Int,
    fillColor: Int,
) {
    val trackW = floatExpression(w, SLIDER_PAD * 2f, SUB)
    // CLAMP pops (value, max, min).
    val ratio = floatExpression(value, 100f, DIV, 1f, 0f, CLAMP)
    val thumbX = floatExpression(SLIDER_PAD, trackW, ratio, MUL, ADD)
    val top = floatExpression(cy, 12f, SUB)
    val bottom = floatExpression(cy, 12f, ADD)
    painter.setColor(trackColor).commit()
    drawRoundRect(SLIDER_PAD, top, floatExpression(w, SLIDER_PAD, SUB), bottom, 12f, 12f)
    painter.setColor(fillColor).commit()
    drawRoundRect(SLIDER_PAD, top, thumbX, bottom, 12f, 12f)
    painter.setColor(Color.WHITE).commit()
    drawCircle(thumbX, cy, THUMB_R)
    painter.setColor(fillColor).commit()
    drawCircle(thumbX, cy, THUMB_R - 10f)
}

/**
 * An interactive slider (0..100) with its value as a big number above it. Returns the `addTouch`
 * variable.
 */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.slider(
    initial: Float,
    height: Int,
    trackColor: Int,
    fillColor: Int,
    numberSize: Float,
): Float {
    var value = 0f
    val modifier =
        if (height > 0) Modifier.fillMaxWidth().height(height)
        else Modifier.fillMaxWidth().verticalWeight(1f)
    box(modifier) {
        canvas(Modifier.fillMaxSize()) {
            val w = addComponentWidthValue()
            val h = addComponentHeightValue()
            val trackW = floatExpression(w, SLIDER_PAD * 2f, SUB)
            value =
                addTouch(
                    initial,
                    0f,
                    100f,
                    RemoteComposeWriter.STOP_INSTANTLY.toInt(),
                    0f,
                    0,
                    null,
                    null,
                    RemoteContext.FLOAT_TOUCH_POS_X,
                    SLIDER_PAD,
                    SUB,
                    100f,
                    MUL,
                    trackW,
                    DIV,
                )
            val txt = createTextFromFloat(value, 3, 0, TextFromFloat.PAD_PRE_NONE)
            painter.setColor(Color.WHITE).setTextSize(numberSize).commit()
            drawTextAnchored(
                txt,
                floatExpression(w, 0.5f, MUL),
                floatExpression(h, 0.3f, MUL),
                0f,
                0f,
                0,
            )
            drawSlider(value, w, floatExpression(h, 0.75f, MUL), trackColor, fillColor)
        }
    }
    return value
}

/**
 * A row of equally sized buttons; each pair is (label, action). A [height] of 0 fills the parent.
 */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.buttonRow(
    height: Int,
    bg: Int,
    vararg buttons: Pair<String, Action>,
) {
    val modifier =
        if (height > 0) Modifier.fillMaxWidth().height(height) else Modifier.fillMaxSize()
    row(modifier, horizontal = RowLayout.SPACE_BETWEEN, vertical = RowLayout.CENTER) {
        for ((label, action) in buttons) {
            box(
                Modifier.horizontalWeight(1f)
                    .fillMaxHeight()
                    .padding(8, 0, 8, 0)
                    .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                    .background(bg)
                    .onClick(action),
                horizontal = BoxLayout.CENTER,
                vertical = BoxLayout.CENTER,
            ) {
                text(label, fontSize = 36f, fontWeight = 700f, color = Color.WHITE)
            }
        }
    }
}

/** Embeds [url] in a box and maps a float variable to it. */
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.childSlot(
    url: String,
    topPadding: Int = 0,
    bind: RemoteComposeContextAndroid.(docId: Int) -> Unit,
) {
    box(Modifier.fillMaxWidth().verticalWeight(1f).padding(0, topPadding, 0, 0)) {
        val docId = rcDoc(url, Modifier.fillMaxSize())
        bind(docId)
    }
}

// ============================================================================
// Embedded child documents
// ============================================================================

/**
 * Child with a slider. Contract: `"title"` (STRING in), `"sliderValue"` (FLOAT 0..100, forward,
 * reverse or bidirectional).
 */
@Suppress("RestrictedApiAndroidX")
internal fun buildChildSliderDoc(): RemoteComposeWriter =
    newDoc(900, 300, "Embedded child slider") {
        val title = addNamedString("title", "Child")
        var value = 0f
        childPage(title) {
            value = slider(50f, 0, TRACK_CHILD, FILL_CHILD, 64f)
        }
        setFloatName(Utils.idFromNan(value), "sliderValue")
    }

/**
 * Nested container: receives `"inLevel"` and `"title"`, forwards `100 - inLevel` into an embedded
 * `embedded_child_slider.rc`.
 */
@Suppress("RestrictedApiAndroidX")
internal fun buildNestedContainerDoc(): RemoteComposeWriter =
    newDoc(900, 340, "Nested container") {
        val inLevel = addNamedFloat("inLevel", 65f)
        val title = addNamedString("title", "Child 2")
        val complement = floatExpression(100f, inLevel, SUB, 100f, 0f, CLAMP)
        root {
            box(
                Modifier.fillMaxSize()
                    .clip(RoundedRectShape(24f, 24f, 24f, 24f))
                    .background(BG_NESTED)
                    .border(3f, 24f, BORDER_NESTED, 0)
                    .padding(16)
            ) {
                val inner = rcDoc(URL_CHILD_SLIDER, Modifier.fillMaxSize())
                mapRcDocVariables(inner, RemoteComposeWriter.MAP_FORWARD, title, "title")
                mapRcDocVariables(inner, RemoteComposeWriter.MAP_FORWARD, complement, "sliderValue")
            }
        }
    }

/** Child with three buttons that fire `HostAction(2001, label)`. Contract: `"title"` (in). */
@Suppress("RestrictedApiAndroidX")
internal fun buildChildButtonsDoc(): RemoteComposeWriter =
    newDoc(900, 260, "Embedded child buttons") {
        val title = addNamedString("title", "Child")
        val a = addText("A")
        val b = addText("B")
        val c = addText("C")
        childPage(title) {
            box(Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 16, 0, 0)) {
                buttonRow(
                    0,
                    0xFF1D4ED8.toInt(),
                    "A" to HostAction(ACTION_PICK, a),
                    "B" to HostAction(ACTION_PICK, b),
                    "C" to HostAction(ACTION_PICK, c),
                )
            }
        }
    }

/** Child with a counter and `+1` / `0` buttons. Contract: `"title"` (in), `"count"` (FLOAT out). */
@Suppress("RestrictedApiAndroidX")
internal fun buildChildCounterDoc(): RemoteComposeWriter =
    newDoc(900, 340, "Embedded child counter") {
        val title = addNamedString("title", "Child")
        val count = addNamedFloat("count", 0f)
        val countId = Utils.idFromNan(count)
        val incId = Utils.idFromNan(floatExpression(count, 1f, ADD))
        childPage(title) {
            bigNumber(count, 80f)
            buttonRow(
                90,
                0xFF1D4ED8.toInt(),
                "+1" to ValueFloatExpressionChange(countId, incId),
                "0" to ValueFloatChange(countId, 0f),
            )
        }
    }

/** Child showing a number it receives. Contract: `"title"` (in), `"value"` (FLOAT in). */
@Suppress("RestrictedApiAndroidX")
internal fun buildChildNumberDoc(): RemoteComposeWriter =
    newDoc(900, 300, "Embedded child number") {
        val title = addNamedString("title", "Child")
        val value = addNamedFloat("value", 0f)
        childPage(title) { bigNumber(value, 96f) }
    }

/** A colored card with a fixed label; used to show the parent swapping child documents. */
@Suppress("RestrictedApiAndroidX")
internal fun buildChildCardDoc(label: String, bg: Int, border: Int): RemoteComposeWriter =
    newDoc(900, 400, "Embedded child card $label") {
        val title = addText(label)
        childPage(title, bg, border) {
            box(
                Modifier.fillMaxWidth().verticalWeight(1f),
                horizontal = BoxLayout.CENTER,
                vertical = BoxLayout.CENTER,
            ) {
                text(label, fontSize = 96f, fontWeight = 700f, color = Color.WHITE)
            }
        }
    }

/**
 * One level of a parent -> child -> grandchild chain: shows `"value"` as a number and, when
 * [nextUrl] is set, embeds the next level and forwards `"value"` to it.
 */
@Suppress("RestrictedApiAndroidX")
internal fun buildChainLevelDoc(label: String, nextUrl: String?): RemoteComposeWriter =
    newDoc(900, if (nextUrl != null) 620 else 240, "Embedded chain $label") {
        val title = addText(label)
        val value = addNamedFloat("value", 0f)
        val bg = if (nextUrl != null) BG_CHILD else BG_NESTED
        val border = if (nextUrl != null) BORDER_CHILD else BORDER_NESTED
        childPage(title, bg, border) {
            if (nextUrl == null) {
                bigNumber(value, 96f)
            } else {
                box(Modifier.fillMaxWidth().height(140)) {
                    canvas(Modifier.fillMaxSize()) {
                        val w = addComponentWidthValue()
                        val h = addComponentHeightValue()
                        val txt = createTextFromFloat(value, 3, 0, TextFromFloat.PAD_PRE_NONE)
                        painter.setColor(Color.WHITE).setTextSize(96f).commit()
                        drawTextAnchored(
                            txt,
                            floatExpression(w, 0.5f, MUL),
                            floatExpression(h, 0.5f, MUL),
                            0f,
                            0f,
                            0,
                        )
                    }
                }
                childSlot(nextUrl) { next ->
                    mapRcDocVariables(next, RemoteComposeWriter.MAP_FORWARD, value, "value")
                }
            }
        }
    }

// ============================================================================
// DEMO 1: Parent slider -> child sliders (MAP_BIDIRECTIONAL + nested MAP_FORWARD)
// ============================================================================

/**
 * The parent slider and preset buttons move Child 1's slider (bidirectional, so Child 1 moves the
 * parent too) and Child 2's slider two levels deep (`100 - parent`).
 */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedParentControlsChild(): RemoteComposeWriter =
    newDoc(1000, 1400, "Parent slider controls child sliders") {
        val child1Title = addText("Child 1")
        val child2Title = addText("Child 2")
        var master = 0f
        parentPage {
            master = slider(65f, 200, TRACK_PARENT, FILL_PARENT, 72f)
            val masterId = Utils.idFromNan(master)
            buttonRow(
                80,
                BG_BUTTON,
                "0" to ValueFloatChange(masterId, 0f),
                "50" to ValueFloatChange(masterId, 50f),
                "100" to ValueFloatChange(masterId, 100f),
            )
            childrenFrame {
                childSlot(URL_CHILD_SLIDER) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, child1Title, "title")
                    mapRcDocVariables(
                        id,
                        RemoteComposeWriter.MAP_BIDIRECTIONAL,
                        master,
                        "sliderValue",
                    )
                }
                childSlot(URL_NESTED_CONTAINER, topPadding = 24) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, child2Title, "title")
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, master, "inLevel")
                }
            }
        }
        setFloatName(Utils.idFromNan(master), "parentMasterSlider")
    }

// ============================================================================
// DEMO 2: Child slider -> parent number (MAP_REVERSE)
// ============================================================================

/** Dragging the slider inside the child updates a number shown by the parent. */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedChildSliderToParent(): RemoteComposeWriter =
    newDoc(1000, 1000, "Child slider controls parent number") {
        val childTitle = addText("Child")
        val parentValue = addNamedFloat("parentValue", 0f)
        parentPage {
            bigNumber(parentValue, 140f)
            childrenFrame {
                childSlot(URL_CHILD_SLIDER) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, childTitle, "title")
                    mapRcDocVariables(
                        id,
                        RemoteComposeWriter.MAP_REVERSE,
                        parentValue,
                        "sliderValue",
                    )
                }
            }
        }
    }

// ============================================================================
// DEMO 3: Child buttons -> parent text (MAP_HOST_ACTION)
// ============================================================================

/** Tapping A / B / C inside the child sets a string shown by the parent. */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedChildButtonsToParent(): RemoteComposeWriter =
    newDoc(1000, 900, "Child buttons control parent text") {
        val childTitle = addText("Child")
        val picked = addNamedString("picked", "-")
        parentPage {
            bigText(picked, 140f)
            childrenFrame {
                childSlot(URL_CHILD_BUTTONS) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, childTitle, "title")
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_HOST_ACTION, picked, ACTION_PICK)
                }
            }
        }
    }

// ============================================================================
// DEMO 4: Two children share one slider (MAP_BIDIRECTIONAL on both)
// ============================================================================

/** Dragging either child's slider moves the other one; the parent shows the shared value. */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedSharedSlider(): RemoteComposeWriter =
    newDoc(1000, 1200, "Two children share a slider") {
        val child1Title = addText("Child 1")
        val child2Title = addText("Child 2")
        val shared = addNamedFloat("shared", 50f)
        parentPage {
            bigNumber(shared, 120f)
            childrenFrame {
                childSlot(URL_CHILD_SLIDER) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, child1Title, "title")
                    mapRcDocVariables(
                        id,
                        RemoteComposeWriter.MAP_BIDIRECTIONAL,
                        shared,
                        "sliderValue",
                    )
                }
                childSlot(URL_CHILD_SLIDER, topPadding = 24) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, child2Title, "title")
                    mapRcDocVariables(
                        id,
                        RemoteComposeWriter.MAP_BIDIRECTIONAL,
                        shared,
                        "sliderValue",
                    )
                }
            }
        }
    }

// ============================================================================
// DEMO 5: Child 1 counter -> Child 2 number (MAP_REVERSE -> MAP_FORWARD)
// ============================================================================

/** Tapping `+1` in Child 1 updates the number shown in Child 2, routed through the parent. */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedChildToChild(): RemoteComposeWriter =
    newDoc(1000, 1200, "Child 1 counter controls Child 2") {
        val child1Title = addText("Child 1")
        val child2Title = addText("Child 2")
        val bus = addNamedFloat("bus", 0f)
        parentPage {
            childrenFrame {
                childSlot(URL_CHILD_COUNTER) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, child1Title, "title")
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_REVERSE, bus, "count")
                }
                childSlot(URL_CHILD_NUMBER, topPadding = 24) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, child2Title, "title")
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, bus, "value")
                }
            }
        }
    }

// ============================================================================
// DEMO 6: Parent swaps which child document is loaded (dynamic rcDoc URL)
// ============================================================================

/** Tapping A / B / C in the parent changes the URL string the `rcDoc` is bound to. */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedSwitchChild(): RemoteComposeWriter =
    newDoc(1000, 1000, "Parent switches the child document") {
        val url = addNamedString("childUrl", URL_CHILD_CARD_A)
        parentPage {
            buttonRow(
                90,
                BG_BUTTON,
                "A" to ValueStringChange(url, URL_CHILD_CARD_A),
                "B" to ValueStringChange(url, URL_CHILD_CARD_B),
                "C" to ValueStringChange(url, URL_CHILD_CARD_C),
            )
            childrenFrame {
                box(Modifier.fillMaxWidth().verticalWeight(1f)) {
                    rcDoc(url, Modifier.fillMaxSize())
                }
            }
        }
    }

// ============================================================================
// DEMO 7: Parent -> child -> grandchild (MAP_FORWARD chained through three levels)
// ============================================================================

/** The parent slider value is forwarded to a child, which forwards it on to a grandchild. */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedThreeLevels(): RemoteComposeWriter =
    newDoc(1000, 1300, "Parent -> child -> grandchild") {
        var master = 0f
        parentPage {
            master = slider(42f, 200, TRACK_PARENT, FILL_PARENT, 72f)
            childrenFrame {
                childSlot(URL_CHAIN_LEVEL_1) { id ->
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, master, "value")
                }
            }
        }
    }

// ============================================================================
// DEMO 8: Children carried inside the parent's bytes (BinaryData, no RcResolver)
// ============================================================================

/**
 * The two child documents are serialized and stored in the parent with (a `BINARY_DATA` blob of
 * type `RC_DOC`), so the whole page is one byte array and the host's `RcResolver` is never
 * consulted. Child 1 is referenced by id, Child 2 through a `rc:embedded/<id>` url string. The
 * parent slider drives both.
 */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedInlineDocuments(): RemoteComposeWriter =
    newDoc(1000, 1300, "Children embedded in the parent bytes") {
        val child1Title = addText("Child 1 (by id)")
        val child2Title = addText("Child 2 (by url)")
        val sliderDoc = addEmbeddedDocument(buildChildSliderDoc().encodeToByteArray())
        val numberDoc = addEmbeddedDocument(buildChildNumberDoc().encodeToByteArray())
        val numberUrl = addText(RcDocLayout.EMBEDDED_URL_PREFIX + numberDoc)
        var master = 0f
        parentPage {
            master = slider(30f, 200, TRACK_PARENT, FILL_PARENT, 72f)
            childrenFrame {
                box(Modifier.fillMaxWidth().verticalWeight(1f)) {
                    val id = rcDoc(sliderDoc, Modifier.fillMaxSize())
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, child1Title, "title")
                    mapRcDocVariables(
                        id,
                        RemoteComposeWriter.MAP_BIDIRECTIONAL,
                        master,
                        "sliderValue",
                    )
                }
                box(Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 24, 0, 0)) {
                    val id = rcDoc(numberUrl, Modifier.fillMaxSize())
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, child2Title, "title")
                    mapRcDocVariables(id, RemoteComposeWriter.MAP_FORWARD, master, "value")
                }
            }
        }
    }
