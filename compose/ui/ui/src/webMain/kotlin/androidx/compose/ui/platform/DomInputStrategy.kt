/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsName
import kotlin.js.definedExternally
import kotlin.js.js
import kotlin.js.unsafeCast
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.EventInit
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.UIEvent
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent

internal class DomInputStrategy(
    imeOptions: ImeOptions,
    private val composeSender: ComposeCommandCommunicator,
) {
    val htmlInput = imeOptions.createDomElement()

    private var lastMeaningfulUpdate = TextFieldValue("")

    // To avoid the re-triggering of the selection change
    private var pauseSelectionChangeListener = false
    private var selectionChangeListener: ((Event) -> Unit)? = null

    init {
        initEvents()
    }

    private val nativeInputEventsProcessor = object : NativeInputEventsProcessor(composeSender) {
        override fun scheduleCheckpoint() {
            window.requestAnimationFrame {
                runCheckpoint(currentTextFieldValue = lastMeaningfulUpdate)
            }
        }
    }

    fun updateState(textFieldValue: TextFieldValue) {
        htmlInput as HTMLElementWithValue

        val needsTextUpdate = lastMeaningfulUpdate.text != textFieldValue.text
        val needsSelectionUpdate = lastMeaningfulUpdate.selection != textFieldValue.selection
        lastMeaningfulUpdate = textFieldValue

        if (needsTextUpdate) {
            htmlInput.value = textFieldValue.text
        }
        if (needsSelectionUpdate) {
            pauseSelectionChangeListener = true
            htmlInput.setSelectionRange(textFieldValue.selection.min, textFieldValue.selection.max)
            pauseSelectionChangeListener = false
        }
    }

    private val tabKeyCode = Key.Tab.keyCode.toInt()

    private fun initEvents() {

        htmlInput.addEventListener("keydown", { evt ->
            nativeInputEventsProcessor.registerEvent(evt as KeyboardEvent)

            if (evt.keyCode == tabKeyCode) {
                // Compose logic will handle the focus movement or insert Tabs if necessary
                evt.preventDefault()
            }

            // Let Compose decide the selection right after a new key input
            pauseSelectionChangeListener = true
        })

        htmlInput.addEventListener("keyup", { evt ->
            nativeInputEventsProcessor.registerEvent(evt as KeyboardEvent)
        })

        htmlInput.addEventListener("beforeinput", { evt ->
            if (evt is InputEvent) {
                htmlInput as HTMLElementWithValue

                val inputExt = evt.asInputEventExt()
                inputExt.textRangeStart = htmlInput.selectionStart
                inputExt.textRangeEnd = htmlInput.selectionEnd

                nativeInputEventsProcessor.registerEvent(evt)
            }
        })

        htmlInput.addEventListener("compositionstart", { evt ->
            nativeInputEventsProcessor.registerEvent(evt as CompositionEvent)
        })

        htmlInput.addEventListener("compositionend", { evt ->
            nativeInputEventsProcessor.registerEvent(evt as CompositionEvent)
        })

        selectionChangeListener = listener@{ _ ->
            if (pauseSelectionChangeListener || !isInputActive()) return@listener
            htmlInput as HTMLElementWithValue
            val start = htmlInput.selectionStart
            val end = htmlInput.selectionEnd
            val selection = lastMeaningfulUpdate.selection

            if (start != selection.min || end != selection.max) {
                val normalizedStart = minOf(start, end)
                val normalizedEnd = maxOf(start, end)
                composeSender.sendEditCommand(SetSelectionCommand(normalizedStart, normalizedEnd))
            }
        }
        // In Chrome, we need to listen to the selection change on the document
        document.addEventListener("selectionchange", selectionChangeListener)
        // In Firefox and Safari we listen to the selection change on the input element.
        // Chrome is expected to dispatch this event too (https://chromium-review.googlesource.com/c/chromium/src/+/5598393),
        // but it doesn't: https://issuetracker.google.com/issues/518751607
        htmlInput.addEventListener("selectionchange", selectionChangeListener)
    }

    fun dispose() {
        document.removeEventListener("selectionchange", selectionChangeListener)
        htmlInput.removeEventListener("selectionchange", selectionChangeListener)
        selectionChangeListener = null
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun isInputActive(): Boolean {
        val root = htmlInput.unsafeCast<NodeWithRootNode>().getRootNode()
        val rootActive = root?.activeElement
        return rootActive == htmlInput
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private external interface NodeWithRootNode : JsAny {
    fun getRootNode(): DocumentOrShadowRootLike?
}

private external interface DocumentOrShadowRootLike : JsAny {
    val activeElement: HTMLElement?
}

@JsName("InputEvent")
internal external class InputEventExt : UIEvent {
    val data: String?
    val inputType: String
    var textRangeStart: Int
    var textRangeEnd: Int

    constructor(type: String, eventInitDict: EventInit = definedExternally)
}

internal inline fun UIEvent.asInputEventExt(): InputEventExt =  unsafeCast<InputEventExt>()

internal val InputEventExt.textRangeSize: Int
    get() = this.asInputEventExt().let { it.textRangeEnd - it.textRangeStart }


private fun ImeOptions.createDomElement(): HTMLElement {
    val htmlElement = document.createElement(
        if (singleLine) "input" else "textarea"
    ) as HTMLElement

    // without autocorrect set "on" iOS virtual keyboard won't suggest
    // see https://youtrack.jetbrains.com/issue/CMP-8807
    htmlElement.setAttribute("autocorrect", "on")
    htmlElement.setAttribute("autocomplete", "off")
    htmlElement.setAttribute("autocapitalize", "off")
    htmlElement.setAttribute("spellcheck", "false")

    val inputMode = when (keyboardType) {
        KeyboardType.Text -> "text"
        KeyboardType.Ascii -> "text"
        KeyboardType.Number -> "number"
        KeyboardType.Phone -> "tel"
        KeyboardType.Uri -> "url"
        KeyboardType.Email -> "email"
        KeyboardType.Password -> "password"
        KeyboardType.NumberPassword -> "number"
        KeyboardType.Decimal -> "decimal"
        else -> "text"
    }

    val enterKeyHint = when (imeAction) {
        ImeAction.Default -> "enter"
        ImeAction.None -> "enter"
        ImeAction.Done -> "done"
        ImeAction.Go -> "go"
        ImeAction.Next -> "next"
        ImeAction.Previous -> "previous"
        ImeAction.Search -> "search"
        ImeAction.Send -> "send"
        else -> "enter"
    }

    htmlElement.setAttribute("inputmode", inputMode)
    htmlElement.setAttribute("enterkeyhint", enterKeyHint)


    htmlElement.style.apply {
        setProperty("position", "absolute")
        setProperty("user-select", "none")
        setProperty("forced-color-adjust", "none")
        setProperty("white-space", "pre")
        setProperty("align-content", "center")
        setProperty(
            "top",
            "calc(min(var(--compose-internal-web-backing-input-top) * 1px, 100vh - var(--compose-internal-web-backing-input-height) * 1px))"
        )
        setProperty(
            "left",
            "calc(min(var(--compose-internal-web-backing-input-left) * 1px, 100vw - var(--compose-internal-web-backing-input-width) * 1px))"
        )
        setProperty("width", "calc(var(--compose-internal-web-backing-input-width) * 1px")
        setProperty("height", "calc(var(--compose-internal-web-backing-input-height) * 1px")
        setProperty("padding", "0")
        setProperty("color", "transparent")
        setProperty("background", "transparent")
        setProperty("caret-color", "transparent")
        setProperty("outline", "none")
        setProperty("border", "none")
        setProperty("resize", "none")
        setProperty("text-shadow", "none")
        setProperty("z-index", "-1")
        // TODO: do we need pointer-events: none
        //setProperty("pointer-events", "none")

        // I keep "opacity" commented to make it explicit that we can't use this property.
        // Reason: Safari iOS keyboard overlaps the text input. See CMP-8611
        // setProperty("opacity", "0")

        // To prevent auto-zoom in some mobile browsers, we set a larger font-size
        setProperty("font-size", "20px")
    }

    return htmlElement
}

private external interface HTMLElementWithValue {
    var value: String
    val selectionStart: Int
    val selectionEnd: Int
    val selectionDirection: String?
    fun setSelectionRange(start: Int, end: Int, direction: String = definedExternally)
}

internal fun isTypedEvent(evt: KeyboardEvent): Boolean =
    js("!evt.metaKey && !evt.ctrlKey && evt.key.charAt(0) === evt.key")
