/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.platform.a11y

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.internal.fastIndexOfFirst
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.DesktopSemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.window.toDpOffset
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.FocusListener
import java.util.*
import javax.accessibility.Accessible
import javax.accessibility.AccessibleAction
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleEditableText
import javax.accessibility.AccessibleExtendedText
import javax.accessibility.AccessibleRelation
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleState
import javax.accessibility.AccessibleStateSet
import javax.accessibility.AccessibleText
import javax.accessibility.AccessibleTextSequence
import javax.accessibility.AccessibleValue
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import org.jetbrains.skia.BreakIterator

private typealias ActionKey = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>

/**
 * An adapter for SemanticNode for AWT accessibility
 */
internal class ComposeAccessible(
    semanticsNode: SemanticsNode,
    private val ownerAccessibility: SemanticsOwnerAccessibility
) : Accessible,
    // Must be a subclass of java.awt.Component because CAccessible only registers property
    // listeners with the accessible context if the Accessible is an instance of java.awt.Component
    // (see constructor of sun.lwawt.macosx.CAccessible), even though there's no reason for it.
    // The property change listener is what allows us to update CAccessible when some value changes.
    java.awt.Component()
{

    /**
     * The semantics node this [ComposeAccessible] represents.
     */
    var semanticsNode: SemanticsNode = semanticsNode
        set(value) {
            field = value
            // Clear the cache
            cachedSemanticsConfig = null
            cachedTraversalOrderedChildren = null
        }

    private var cachedSemanticsConfig: SemanticsConfiguration? = null

    /**
     * The (cached) [SemanticsNode.config] of [semanticsNode].
     */
    val semanticsConfig: SemanticsConfiguration
        get() =
            cachedSemanticsConfig ?: semanticsNode.config.also {
                cachedSemanticsConfig = it
            }

    private var cachedTraversalOrderedChildren: List<SemanticsNode>? = null

    /**
     * The (cached) [SemanticsNode.traversalOrderedChildren] of [semanticsNode].
     */
    private val traversalOrderedChildren: List<SemanticsNode>
        get() =
            cachedTraversalOrderedChildren ?: semanticsNode.traversalOrderedChildren(semanticsConfig).also {
                cachedTraversalOrderedChildren = it
            }

    val composeAccessibleContext: ComposeAccessibleComponent by lazy { ComposeAccessibleComponent() }

    private var disposed = false

    fun dispose() {
        disposed = true
    }

    override fun getAccessibleContext(): AccessibleContext? {
        if (disposed) {
            // The accessibility system keeps calling functions on the context even after the node
            // has been removed. We return `null` so it doesn't do that.
            return null
        }

        return composeAccessibleContext
    }

    override fun toString(): String {
        if (disposed) return "ComposeAccessible(disposed)"
        return with(composeAccessibleContext) {
            val description = (accessibleDescription ?: text)?.let { "\"$it\"" } ?: "unknown"
            "ComposeAccessible(role='$accessibleRole', $description)"
        }
    }

    open inner class ComposeAccessibleComponent : AccessibleContext(), AccessibleComponent, AccessibleAction {
        val textSelectionRange
            get() = semanticsConfig.getOrNull(SemanticsProperties.TextSelectionRange)
        val setText
            get() = semanticsConfig.getOrNull(SemanticsActions.SetText)
        val setSelection
            get() = semanticsConfig.getOrNull(SemanticsActions.SetSelection)
        val text
            get() = semanticsConfig.getOrNull(SemanticsProperties.EditableText)
                ?: semanticsConfig.getOrNull(SemanticsProperties.Text)?.mergeText()

        val textLayoutResult: TextLayoutResult?
            get() {
                val textLayoutResults = mutableListOf<TextLayoutResult>()
                val getLayoutResult = semanticsConfig
                    .getOrNull(SemanticsActions.GetTextLayoutResult)
                    ?.action?.invoke(textLayoutResults)
                return if (getLayoutResult == true) {
                    textLayoutResults[0]
                } else {
                    null
                }
            }

        val focused
            get() = semanticsConfig.getOrNull(SemanticsProperties.Focused)

        val selected
            get() = semanticsConfig.getOrNull(SemanticsProperties.Selected)

        private val density: Density
            get() = ownerAccessibility.desktopComponent.density

        val horizontalScroll
            get() = semanticsConfig.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)

        val verticalScroll
            get() = semanticsConfig.getOrNull(SemanticsProperties.VerticalScrollAxisRange)

        val scrollBy
            get() = semanticsConfig.getOrNull(SemanticsActions.ScrollBy)

        val isPassword
            get() = semanticsConfig.getOrNull(SemanticsProperties.Password) != null

        val toggleableState
            get() = semanticsConfig.getOrNull(SemanticsProperties.ToggleableState)

        val auxiliaryChildren
            get() = buildList {
                horizontalScroll?.let {
                    add(makeScrollbarChild(vertical = false))
                }
                verticalScroll?.let {
                    add(makeScrollbarChild(vertical = true))
                }
            }

        val progressBarRangeInfo
            get() = semanticsConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo)

        val isContainer
            @Suppress("DEPRECATION")
            get() = semanticsConfig.getOrNull(SemanticsProperties.IsContainer)

        val isTraversalGroup
            get() = semanticsConfig.getOrNull(SemanticsProperties.IsTraversalGroup)

        private fun makeScrollbarChild(
            vertical: Boolean
        ): Accessible {
            val bar = ScrollBarAccessible(vertical)

            val controlledBy = AccessibleRelation(
                AccessibleRelation.CONTROLLED_BY,
                bar
            )
            val controllerFor = AccessibleRelation(
                AccessibleRelation.CONTROLLER_FOR,
                this@ComposeAccessible
            )
            bar.context.accessibleRelationSet.add(controllerFor)
            accessibleRelationSet.add(controlledBy)
            return bar
        }

        private fun Point.toComposeOffset() =
            toDpOffset().toOffset(density)

        private fun Dp.toAwtPx() =
            if (value.isInfinite()) Constraints.Infinity else value.fastRoundToInt()

        private fun Rect.toAwtRectangle() = with(density) {
            Rectangle(
                left.toDp().toAwtPx(),
                top.toDp().toAwtPx(),
                width.toDp().toAwtPx(),
                height.toDp().toAwtPx(),
            )
        }

        private fun Offset.toAwtPoint() = with(density) {
            Point(
                x.toDp().toAwtPx(),
                y.toDp().toAwtPx(),
            )
        }

        private fun IntSize.toAwtDimension() = with(density) {
            Dimension(
                width.toDp().toAwtPx(),
                height.toDp().toAwtPx(),
            )
        }

        override fun getAccessibleName(): String? {
            return semanticsConfig.getOrNull(SemanticsProperties.ContentDescription)?.mergeText()
                ?: semanticsConfig.getOrNull(SemanticsProperties.Text)?.mergeText()
        }

        override fun getAccessibleDescription(): String? {
            return semanticsConfig
                .getOrNull(SemanticsProperties.ContentDescription)
                ?.mergeText()
        }

        override fun getAccessibleParent(): Accessible? {
            return ownerAccessibility.accessibleParentOf(this@ComposeAccessible)
        }

        override fun getAccessibleIndexInParent(): Int {
            val parentNode = semanticsNode.parent ?: return ownerAccessibility.indexInScene()
            val parentAccessible = ownerAccessibility.accessibleByNodeId(parentNode.id)
            val traversalOrderedSiblings = parentAccessible?.traversalOrderedChildren
                // Not sure if `parentAccessible` can ever be `null`, but just in case...
                ?: parentNode.traversalOrderedChildren()
            return traversalOrderedSiblings.fastIndexOfFirst { it.id == semanticsNode.id }
        }

        override fun getAccessibleComponent(): AccessibleComponent? {
            return this
        }

        private var accessibleActions: List<Pair<String, ActionKey>>? = null

        private fun updateAccessibleActions() {
            val actions = mutableListOf<Pair<String, ActionKey>>()

            fun addActionIfExist(
                key: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>,
                label: String? = null,
            ) {
                val action = semanticsConfig.getOrNull(key) ?: return
                val label = label ?: action.label ?: return  // No point to add an action without a label
                actions.add(Pair(label, key))
            }

            // AWT expects a "click" label for click actions, at least on macOS...
            addActionIfExist(SemanticsActions.OnClick, AccessibleAction.CLICK)
            addActionIfExist(SemanticsActions.OnLongClick)
            addActionIfExist(SemanticsActions.Expand)
            addActionIfExist(SemanticsActions.Collapse)
            addActionIfExist(SemanticsActions.Dismiss)

            accessibleActions = actions.takeIf { it.isNotEmpty() }
        }

        override fun getAccessibleAction(): AccessibleAction? {
            updateAccessibleActions()
            return if (accessibleActions == null) null else this
        }

        override fun getAccessibleActionCount(): Int = accessibleActions?.size ?: 0

        override fun getAccessibleActionDescription(i: Int): String? {
            val actions = accessibleActions!!
            val (label, _) = actions[i]
            return label
        }

        override fun doAccessibleAction(i: Int): Boolean {
            val actions = accessibleActions!!
            val (_, actionKey) = actions[i]
            return semanticsConfig.getOrNull(actionKey)?.let {
                it.action?.invoke()
            } ?: false
        }

        override fun getAccessibleValue(): AccessibleValue? {
            // On macOS/VoiceOver, the a11y system appears to inspect the value we return here for
            // checkboxes and radio buttons. On Windows, it looks at getAccessibleStateSet instead.
            return when {
                toggleableState != null -> ToggleableAccessibleValue(this)
                computeAccessibleRole() == AccessibleRole.RADIO_BUTTON -> RadioButtonAccessibleValue(this)
                progressBarRangeInfo != null -> ProgressBarAccessibleValue(this)
                else -> null
            }
        }

        override fun getAccessibleChildrenCount(): Int {
            return traversalOrderedChildren.size + auxiliaryChildren.size
        }

        override fun getAccessibleChild(index: Int): Accessible? {
            val regularChildren = traversalOrderedChildren
            return if (index < regularChildren.size) {
                ownerAccessibility.accessibleByNodeId(regularChildren[index].id)
            } else {
                auxiliaryChildren[index - regularChildren.size]
            }
        }

        override fun getLocale(): Locale = Locale.getDefault()

        override fun getLocationOnScreen(): Point {
            return semanticsNode.positionOnScreen.toAwtPoint()
        }

        override fun getLocation(): Point {
            return semanticsNode.positionInRoot.toAwtPoint()
        }

        override fun getBounds(): Rectangle {
            return semanticsNode.boundsInRoot.toAwtRectangle()
        }

        override fun getSize(): Dimension {
            return semanticsNode.size.toAwtDimension()
        }

        override fun isVisible(): Boolean =
            !semanticsNode.outerSemanticsNode.requireCoordinator(Nodes.Semantics).isTransparent()

        override fun isEnabled(): Boolean =
            semanticsConfig.getOrNull(SemanticsProperties.Disabled) == null

        // TODO check actual visibility
        override fun isShowing(): Boolean = true

        override fun contains(p: Point): Boolean {
            return bounds.contains(p)
        }

        override fun getAccessibleAt(p: Point): Accessible? {
            val accessibleChildren = traversalOrderedChildren
            accessibleChildren.fastForEach { child ->
                val accessible = ownerAccessibility.accessibleByNodeId(child.id) as? Accessible ?: return@fastForEach
                val accessibleComponent = (accessible.accessibleContext as? AccessibleComponent) ?: return@fastForEach
                accessibleComponent.getAccessibleAt(p)?.let {
                    return it
                }
            }

            auxiliaryChildren.fastForEach { accessibleChild ->
                val accessibleComponent =
                    accessibleChild.accessibleContext as? AccessibleComponent ?: return@fastForEach
                accessibleComponent.getAccessibleAt(p)?.let {
                    return it
                }
            }

            if (contains(p)) {
                return this@ComposeAccessible
            }

            return null
        }

        override fun isFocusTraversable(): Boolean {
            return focused != null
        }

        override fun requestFocus() {
            if (focused == false) {
                semanticsNode.unmergedConfig.getOrNull(SemanticsActions.RequestFocus)
                    ?.action?.invoke()
            }
        }

        override fun addFocusListener(l: FocusListener?) {
            println("Not implemented: addFocusListener")
            TODO("Not yet implemented")
        }

        override fun removeFocusListener(l: FocusListener?) {
            println("Not implemented: removeFocusListener")
            TODO("Not yet implemented")
        }

        // -----------------------------------

        private fun computeAccessibleRole(): AccessibleRole {
            // AWT role takes precedence
            semanticsConfig.getOrNull(DesktopSemanticsProperties.AwtRole)?.let {
                return it
            }

            // Check semantics role
            val role = semanticsConfig.getOrNull(SemanticsProperties.Role)
            val accessibleRole = when (role) {
                null -> null  // Quickly return null
                Role.Button -> AccessibleRole.PUSH_BUTTON
                Role.Checkbox, Role.Switch -> AccessibleRole.CHECK_BOX
                Role.RadioButton -> AccessibleRole.RADIO_BUTTON
                Role.Tab -> AccessibleRole.PAGE_TAB
                Role.DropdownList -> AccessibleRole.COMBO_BOX
                else -> null
            }
            if (accessibleRole != null) return accessibleRole

            // Guess role from other semantics properties
            return when {
                isPassword -> AccessibleRole.PASSWORD_TEXT
                setText != null -> AccessibleRole.TEXT
                scrollBy != null -> AccessibleRole.SCROLL_PANE
                text != null -> AccessibleRole.LABEL
                progressBarRangeInfo != null -> {
                    if (semanticsConfig.getOrNull(SemanticsActions.SetProgress) != null)
                        AccessibleRole.SLIDER
                    else
                        AccessibleRole.PROGRESS_BAR
                }
                isContainer != null -> AccessibleRole.GROUP_BOX
                isTraversalGroup != null -> AccessibleRole.GROUP_BOX
                else -> AccessibleRole.UNKNOWN
            }
        }

        override fun getAccessibleRole(): AccessibleRole {
            SemanticsOwnerAccessibility.AccessibilityUsage.notifyInUse()
            return computeAccessibleRole()
        }

        override fun getAccessibleStateSet(): AccessibleStateSet {
            return AccessibleStateSet().apply {
                // can we support these
                // AccessibleState.SINGLE_LINE
                // AccessibleState.MULTI_LINE

                if (isEnabled)
                    add(AccessibleState.ENABLED)
                if (semanticsConfig.getOrNull(SemanticsProperties.IsEditable) == true)
                    add(AccessibleState.EDITABLE)
                if (isShowing)
                    add(AccessibleState.SHOWING)
                if (isVisible)
                    add(AccessibleState.VISIBLE)
                if (isFocusTraversable)
                    add(AccessibleState.FOCUSABLE)
                if (focused == true)
                    add(AccessibleState.FOCUSED)

                val canExpand = semanticsConfig.getOrNull(SemanticsActions.Expand) != null
                val canCollapse = semanticsConfig.getOrNull(SemanticsActions.Collapse) != null

                if (canExpand || canCollapse)
                    add(AccessibleState.EXPANDABLE)

                if (canExpand)
                    add(AccessibleState.COLLAPSED)

                if (canCollapse)
                    add(AccessibleState.EXPANDED)

                when (computeAccessibleRole()) {
                    // Note that this is not executed on macOS (for checkboxes or radio buttons),
                    // where the system inspects the value returned by getAccessibleValue instead.
                    AccessibleRole.CHECK_BOX -> addCheckedStateForCheckboxOrSwitch()
                    AccessibleRole.RADIO_BUTTON -> addCheckedStateForRadioButton()
                    else -> {  // Default case for other roles
                        addDefaultStateForToggleableState()

                        if (selected != null)
                            add(AccessibleState.SELECTABLE)

                        if (selected == true)
                            add(AccessibleState.SELECTED)
                    }
                }

                if (accessibleRole.let { it == AccessibleRole.SLIDER || it == AccessibleRole.PROGRESS_BAR }) {
                    // Without this, VoiceOver says "Circular Slider".
                    add(AccessibleState.HORIZONTAL)
                }
            }
        }

        private fun AccessibleStateSet.addCheckedStateForCheckboxOrSwitch() {
            // Checkbox uses Modifier.triStateToggleable, which sets
            // SemanticsPropertyReceiver.toggleableState
            // Switch uses Modifier.toggleable, which also sets
            // SemanticsPropertyReceiver.toggleableState
            addDefaultStateForToggleableState()
        }

        private fun AccessibleStateSet.addCheckedStateForRadioButton() {
            // RadioButton uses Modifier.selectable, which sets
            // SemanticsPropertyReceiver.selected
            if (selected == true) {
                add(AccessibleState.CHECKED)
            }
        }

        private fun AccessibleStateSet.addDefaultStateForToggleableState() {
            val state = when (toggleableState) {
                ToggleableState.On -> AccessibleState.CHECKED
                // AccessibleState.INDETERMINATE doesn't appear to affect NVDA, and it's not clear
                // whether it's even suitable to this case, but we return it anyway because there
                // don't appear to be any adverse effects, and perhaps some other screen reader,
                // or a testing framework will find it useful.
                ToggleableState.Indeterminate -> AccessibleState.INDETERMINATE
                ToggleableState.Off, null -> null
            }
            if (state != null)
                add(state)
        }

        override fun toString(): String {
            val description = (accessibleDescription ?: text)?.let { "\"$it\"" } ?: "unknown"
            return "ComposeAccessibleComponent(role='$accessibleRole', $description)"
        }

        open inner class ComposeAccessibleText : AccessibleText,
            AccessibleExtendedText {
            override fun getIndexAtPoint(p: Point): Int {
                return textLayoutResult!!.getOffsetForPosition(p.toComposeOffset())
            }

            override fun getCharacterBounds(i: Int): Rectangle {
                if (i < 0 || i >= text!!.length) {
                    return Rectangle(
                        (location.x / density.density).toInt(),
                        (location.y / density.density).toInt(),
                        0,
                        0
                    )
                }
                return textLayoutResult!!.getBoundingBox(i).toAwtRectangle()
            }

            override fun getCharCount(): Int {
                return text!!.length
            }

            override fun getCaretPosition(): Int {
                return textSelectionRange?.start ?: -1
            }

            private fun partToBreakIterator(part: Int): BreakIterator {
                val iter = when (part) {
                    AccessibleText.SENTENCE -> BreakIterator.makeSentenceInstance()
                    AccessibleText.WORD -> BreakIterator.makeWordInstance()
                    AccessibleText.CHARACTER -> BreakIterator.makeCharacterInstance()
                    else -> throw IllegalArgumentException()
                }
                iter.setText(text!!.toString())
                return iter
            }

            override fun getAtIndex(part: Int, index: Int): String {
                return when (val end = partToBreakIterator(part).following(index)) {
                    BreakIterator.DONE -> ""
                    else -> text!!.subSequence(index, end).toString()
                }
            }

            override fun getAfterIndex(part: Int, index: Int): String {
                val iterator = partToBreakIterator(part)
                var start = index
                do {
                    start = iterator.following(start)
                    if (start == BreakIterator.DONE) return ""
                } while (text!![start] == ' ' || text!![start] == '\n')
                val end = when (val end = iterator.next()) {
                    BreakIterator.DONE -> iterator.last()
                    else -> end
                }
                return text!!.subSequence(start, end).toString()
            }

            override fun getBeforeIndex(part: Int, index: Int): String {
                return when (val start = partToBreakIterator(part).preceding(index)) {
                    BreakIterator.DONE -> ""
                    else -> text!!.subSequence(start, index).toString()
                }
            }

            override fun getCharacterAttribute(i: Int): AttributeSet {
                println("Not implemented: getCharacterAttribute")
                // TODO("Not yet implemented")
                return SimpleAttributeSet()
            }

            override fun getSelectionStart(): Int {
                return textSelectionRange?.min ?: 0
            }

            override fun getSelectionEnd(): Int {
                return textSelectionRange?.max ?: 0
            }

            override fun getSelectedText(): String? {
                val selection = textSelectionRange ?: return null
                val start = selection.min
                val end = selection.max
                return if (start == end) null
                else text!!.subSequence(start, end).toString()
            }

            override fun getTextRange(startIndex: Int, endIndex: Int): String {
                return text!!.subSequence(startIndex, endIndex).toString()
            }

            override fun getTextSequenceAt(part: Int, index: Int): AccessibleTextSequence {
                println("Not implemented: getBeforeIndex")
                TODO("Not yet implemented")
            }

            override fun getTextSequenceAfter(part: Int, index: Int): AccessibleTextSequence {
                println("Not implemented: getTextSequenceAfter")
                TODO("Not yet implemented")
            }

            override fun getTextSequenceBefore(part: Int, index: Int): AccessibleTextSequence {
                println("Not implemented: getTextSequenceBefore")
                TODO("Not yet implemented")
            }

            override fun getTextBounds(startIndex: Int, endIndex: Int): Rectangle {
                println("Not implemented: getTextBounds")
                TODO("Not yet implemented")
            }
        }

        inner class ScrollBarAccessible(
            val vertical: Boolean
        ) : Accessible {
            val context: AccessibleContext = object : AccessibleContext(),
                AccessibleValue {
                private val range = if (vertical) {
                    verticalScroll!!
                } else {
                    horizontalScroll!!
                }

                override fun getAccessibleValue(): AccessibleValue = this

                override fun getAccessibleRole(): AccessibleRole =
                    AccessibleRole.SCROLL_BAR

                override fun getAccessibleStateSet(): AccessibleStateSet {
                    return AccessibleStateSet().apply {
                        add(AccessibleState.ENABLED)
                        if (vertical) {
                            add(AccessibleState.VERTICAL)
                        } else {
                            add(AccessibleState.HORIZONTAL)
                        }
                    }
                }

                override fun getAccessibleParent(): Accessible {
                    return this@ComposeAccessible
                }

                override fun getAccessibleIndexInParent(): Int {
                    return auxiliaryChildren.indexOf(this@ScrollBarAccessible)
                }

                override fun getAccessibleChildrenCount(): Int = 0

                override fun getAccessibleChild(i: Int): Accessible? = null
                override fun getLocale(): Locale {
                    return Locale.getDefault()
                }

                override fun getCurrentAccessibleValue(): Number = range.value()

                override fun setCurrentAccessibleValue(n: Number?): Boolean {
                    return if (vertical) {
                        scrollBy!!.action!!.invoke(0f, n!!.toFloat() - range.value())
                    } else {
                        scrollBy!!.action!!.invoke(n!!.toFloat() - range.value(), 0f)
                    }
                }

                override fun getMinimumAccessibleValue(): Number = 0

                override fun getMaximumAccessibleValue(): Number = range.maxValue()
            }

            override fun getAccessibleContext(): AccessibleContext = context
        }

        private val accessibleText by lazy {
            // Technically it's wrong to cache this because `setText` or `text` may change
            when {
                setText != null -> ComposeAccessibleEditableText()
                text != null -> ComposeAccessibleText()
                else -> null
            }
        }

        override fun getAccessibleText(): AccessibleText? {
            return accessibleText
        }

        inner class ComposeAccessibleEditableText :
            ComposeAccessibleText(), AccessibleEditableText {
            override fun setTextContents(s: String) {
                setText!!.action!!.invoke(AnnotatedString(s))
            }

            override fun insertTextAtIndex(index: Int, s: String) {
                val text = text!!
                setText!!.action!!.invoke(
                    buildAnnotatedString {
                        append(text.subSequence(0, index))
                        append(s)
                        append(text.subSequence(index, text.length - 1))
                    }
                )
            }

            override fun getTextRange(startIndex: Int, endIndex: Int): String {
                return text!!.substring(startIndex, endIndex)
            }

            override fun delete(startIndex: Int, endIndex: Int) {
                val text = text!!
                setText!!.action!!.invoke(
                    buildAnnotatedString {
                        append(text.subSequence(0, startIndex))
                        append(text.subSequence(endIndex, text.length - 1))
                    }
                )
            }

            override fun cut(startIndex: Int, endIndex: Int) {
                TODO("Not yet implemented")
            }

            override fun paste(startIndex: Int) {
                TODO("Not yet implemented")
            }

            override fun replaceText(startIndex: Int, endIndex: Int, s: String) {
                val text = text!!
                setText!!.action!!.invoke(
                    buildAnnotatedString {
                        append(text.subSequence(0, startIndex))
                        append(s)
                        append(text.subSequence(endIndex, text.length - 1))
                    }
                )
            }

            override fun selectText(startIndex: Int, endIndex: Int) {
                // I'm not sure about traversalMode = true here
                setSelection!!.action!!.invoke(startIndex, endIndex, false)
            }

            override fun setAttributes(startIndex: Int, endIndex: Int, `as`: AttributeSet?) {
                println("Not implemented: setAttributes")
                TODO("Not yet implemented")
            }
        }

        override fun getAccessibleEditableText(): AccessibleEditableText? {
            val accessibleText = accessibleText
            return accessibleText as? AccessibleEditableText
        }

        // -----------------------------------

        override fun setBounds(r: Rectangle?) {
            println("Not implemented: setBounds")
            TODO("Not yet implemented")
        }

        override fun setSize(d: Dimension?) {
            println("Not implemented: setSize")
            TODO("Not yet implemented")
        }

        override fun setLocation(p: Point?) {
            println("Not implemented: setLocation")
            TODO("Not yet implemented")
        }

        override fun getBackground(): Color {
            println("Not implemented: getBackground")
            TODO("Not yet implemented")
        }

        override fun setBackground(c: Color?) {
            println("Not implemented: setBackground")
            TODO("Not yet implemented")
        }

        override fun getForeground(): Color {
            println("Not implemented: getForeground")
            TODO("Not yet implemented")
        }

        override fun setForeground(c: Color?) {
            println("Not implemented: setForeground")
            TODO("Not yet implemented")
        }

        override fun getCursor(): Cursor {
            println("Not implemented: getCursor")
            TODO("Not yet implemented")
        }

        override fun setCursor(cursor: Cursor?) {
            println("Not implemented: setCursor")
            TODO("Not yet implemented")
        }

        override fun getFont(): Font {
            println("Not implemented: getFont")
            TODO("Not yet implemented")
        }

        override fun setFont(f: Font?) {
            println("Not implemented: setFont")
            TODO("Not yet implemented")
        }

        override fun getFontMetrics(f: Font?): FontMetrics {
            println("Not implemented: getFontMetrics")
            TODO("Not yet implemented")
        }

        override fun setEnabled(b: Boolean) {
            println("Not implemented: setEnabled")
            TODO("Not yet implemented")
        }

        override fun setVisible(b: Boolean) {
            println("Not implemented: setVisible")
            TODO("Not yet implemented")
        }

        private fun List<CharSequence>.mergeText() = fastJoinToString(", ")
    }
}

private class ToggleableAccessibleValue(
    val component: ComposeAccessible.ComposeAccessibleComponent
): AccessibleValue {
    override fun getCurrentAccessibleValue(): Number {
        return when (component.toggleableState) {
            ToggleableState.On -> 1
            else -> 0
        }
    }

    override fun setCurrentAccessibleValue(n: Number?): Boolean {
        // TODO: Implement this
        return false
    }

    override fun getMinimumAccessibleValue(): Number {
        return 0
    }

    override fun getMaximumAccessibleValue(): Number {
        return 1
    }
}

private class RadioButtonAccessibleValue(
    val component: ComposeAccessible.ComposeAccessibleComponent
): AccessibleValue {
    override fun getCurrentAccessibleValue(): Number {
        return if (component.selected == true) 1 else 0
    }

    override fun setCurrentAccessibleValue(n: Number?): Boolean {
        // TODO: Implement this
        return false
    }

    override fun getMinimumAccessibleValue(): Number {
        return 0
    }

    override fun getMaximumAccessibleValue(): Number {
        return 1
    }
}

private class ProgressBarAccessibleValue(
    val component: ComposeAccessible.ComposeAccessibleComponent
): AccessibleValue {

    private val rangeInfo: ProgressBarRangeInfo?
        get() = component.progressBarRangeInfo

    override fun getCurrentAccessibleValue(): Number {
        return rangeInfo?.current ?: 0f
    }

    override fun setCurrentAccessibleValue(n: Number?): Boolean {
        // Can't set the value of a progress bar
        return false
    }

    override fun getMinimumAccessibleValue(): Number {
        return rangeInfo?.range?.start ?: 0f
    }

    override fun getMaximumAccessibleValue(): Number {
        return rangeInfo?.range?.endInclusive ?: 1f
    }
}

private fun SemanticsNode.traversalOrderedChildren(
    cachedConfig: SemanticsConfiguration? = null
): List<SemanticsNode> {
    val config = cachedConfig ?: config
    val children = replacedChildren
    if (config.getOrNull(SemanticsProperties.IsTraversalGroup) != true) return children

    // Note that the `SemanticsProperties.TraversalIndex` is not merged anyway, so it's ok to get it
    // from `unmergedConfig`.
    // `config` is currently very slow (b/184376083) and wrong (b/384549982)?

    val allIndicesAreNull = children.fastAll {
        it.unmergedConfig.getOrNull(SemanticsProperties.TraversalIndex) == null
    }
    if (allIndicesAreNull) return children  // Common case

    return children.sortedBy {
        it.unmergedConfig.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f
    }
}