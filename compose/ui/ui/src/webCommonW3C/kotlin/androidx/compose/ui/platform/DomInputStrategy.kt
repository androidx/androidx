package androidx.compose.ui.platform

import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent


internal class DomInputStrategy(
    imeOptions: ImeOptions,
    private val composeSender: ComposeCommandCommunicator,
) {
    val htmlInput = imeOptions.createDomElement()

    private var editState: EditState = EditState.Default

    private var lastMeaningfulUpdate = TextFieldValue("")

    private var repeatDetector: RepeatDetector

    init {
        initEvents()
        repeatDetector = RepeatDetector(htmlInput)
    }

    fun updateState(textFieldValue: TextFieldValue) {
        htmlInput as HTMLElementWithValue

        if (editState != EditState.WaitingComposeActivity) return

        if (lastMeaningfulUpdate.text != textFieldValue.text) {
            htmlInput.value = textFieldValue.text
        }
        if (lastMeaningfulUpdate.selection != textFieldValue.selection) {
            htmlInput.setSelectionRange(textFieldValue.selection.start, textFieldValue.selection.end)
        }

        lastMeaningfulUpdate = textFieldValue

        editState = EditState.Default
    }

    private fun initEvents() {
        var lastKeyboardEventIsDown = false
        var lastActualCompositionTimestamp = 0

        htmlInput.addEventListener("blur", {evt ->
            // both accent dialogue and composition dialogue are lost when we switch windows
            // but can be restored later on if we are back
            editState = EditState.Default
        })

        htmlInput.addEventListener("keydown", {evt ->
            evt as KeyboardEvent
            lastKeyboardEventIsDown = evt.key != "Dead" && evt.key != "Unidentified"

            if (evt.isComposing) {
                editState = EditState.CompositeDialogue
            }

            if (editState is EditState.CompositeDialogue) {
                evt.preventDefault()
                return@addEventListener
            }

            if (evt.repeat && isTypedEvent(evt)) {
                if (repeatDetector.repeatMode == RepeatMode.Accent) {
                    editState = EditState.AccentDialogue
                    return@addEventListener
                }
                if (repeatDetector.repeatMode == RepeatMode.Unknown) {
                    return@addEventListener
                }
            }

            if (editState is EditState.AccentDialogue) {
                return@addEventListener
            }

            val timeStamp = evt.timeStamp.toInt()
            // We ignore timestamps that zero because that means that host OS/browser just doesn't provide this information
            if (timeStamp != 0  && timeStamp <= lastActualCompositionTimestamp) {
                evt.preventDefault()
                return@addEventListener
            }

            editState = EditState.WaitingComposeActivity

            val processed = composeSender.sendKeyboardEvent(evt.toComposeEvent())
            if (processed) {
                evt.preventDefault()
            } else {
                editState = EditState.Default
            }

            lastActualCompositionTimestamp = 0
        })

        htmlInput.addEventListener("keyup", { evt ->
            lastKeyboardEventIsDown = false
            evt as KeyboardEvent

            if (evt.isComposing) {
                editState = EditState.CompositeDialogue
            }

        })

        htmlInput.addEventListener("beforeinput", { evt ->
            evt as InputEvent

            if (editState is EditState.WaitingComposeActivity) return@addEventListener

            when (evt.inputType) {
                "insertCompositionText" -> {
                    editState = EditState.Default
                    composeSender.sendEditCommand(SetComposingTextCommand(evt.data!!, 1))
                }
                "insertReplacementText" -> {
                    // happens in Safari when we choose something from the Accent Dialogue
                    editState = EditState.WaitingComposeActivity
                    composeSender.sendEditCommand(listOf(
                        DeleteSurroundingTextInCodePointsCommand(1, 0),
                        CommitTextCommand(evt.data!!, 1)
                    ))
                }
                "insertText" -> {
                    evt.preventDefault()

                    val editCommands = mutableListOf<EditCommand>()
                    if (editState is EditState.AccentDialogue) {
                        editCommands.add(DeleteSurroundingTextInCodePointsCommand(1, 0))
                    }
                    editCommands.add(CommitTextCommand(evt.data!!, 1))

                    editState = EditState.WaitingComposeActivity
                    composeSender.sendEditCommand(editCommands)
                }
            }

            evt.preventDefault()
        })

        htmlInput.addEventListener("compositionstart", {evt ->
            evt as CompositionEvent
            editState = EditState.CompositeDialogue

            if (lastKeyboardEventIsDown) {
                composeSender.sendEditCommand(DeleteSurroundingTextInCodePointsCommand(1, 0))
            }
        })

        htmlInput.addEventListener("compositionend", {evt ->
            evt as CompositionEvent
            lastActualCompositionTimestamp = evt.timeStamp.toInt()

            // in Safari we can rely on "insertFromComposition" input event but unfortunately it's not present in other browsers
            editState = EditState.WaitingComposeActivity
            composeSender.sendEditCommand(CommitTextCommand(evt.data, 1))
        })
    }
}


private sealed interface EditState {
    data object Default : EditState
    data object WaitingComposeActivity : EditState
    data object CompositeDialogue: EditState
    data object AccentDialogue: EditState
}

private external class InputEvent : Event {
    val inputType: String
    val data: String?
}

private fun ImeOptions.createDomElement(): HTMLElement {
    val htmlElement = document.createElement(
        if (singleLine) "input" else "textarea"
    ) as HTMLElement

    htmlElement.setAttribute("autocorrect", "off")
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
        setProperty("white-space", "pre-wrap")
        setProperty("align-content", "center")
        setProperty("top", "calc(min(var(--compose-internal-web-backing-input-top) * 1px, 100vh - var(--compose-internal-web-backing-input-height) * 1px))")
        setProperty("left", "calc(min(var(--compose-internal-web-backing-input-left) * 1px, 100vw - var(--compose-internal-web-backing-input-width) * 1px))")
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

private external interface HTMLElementWithValue  {
    var value: String
    fun setSelectionRange(start: Int, end: Int, direction: String = definedExternally)
}

/**
 * Represents the mode of key input repetition handling during text input.
 */
private sealed interface RepeatMode {
    /**
     * The repetition behavior of a key input event cannot be determined so far
     */
    data object Unknown: RepeatMode

    /**
     * Repetition triggers Accent Dialogue
     */
    data object Accent: RepeatMode

    /**
     * Repetition does not trigger Accent Dialogue
     */
    data object Default: RepeatMode
}

private class RepeatDetector(private val input: HTMLElement) {
    private var resolving = false
    var repeatMode: RepeatMode = RepeatMode.Unknown
        private set

    init {
        initEvents()
    }

    fun initEvents() {
        input.addEventListener("keydown", { evt ->
            evt as KeyboardEvent
            if (evt.repeat && this.repeatMode === RepeatMode.Unknown) {
                // we can not deduce anything if event is not typed
                if (!isTypedEvent(evt)) return@addEventListener
                if (resolving) {
                    repeatMode = RepeatMode.Accent;
                    resolving = false;
                } else {
                    resolving = true;
                }
            }
        });

        input.addEventListener("beforeinput", {
            if (resolving && repeatMode === RepeatMode.Unknown) {
                resolving = false;
                repeatMode = RepeatMode.Default;
            }
        });
    }
}

private fun isTypedEvent(evt: KeyboardEvent): Boolean = js("!evt.metaKey && !evt.ctrlKey && evt.key.charAt(0) === evt.key")

