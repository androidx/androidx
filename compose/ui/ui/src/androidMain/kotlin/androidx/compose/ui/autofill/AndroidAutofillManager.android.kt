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

package androidx.compose.ui.autofill

import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillManager as PlatformAndroidManager
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.intObjectMapOf
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.TextClassName
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.TextFieldClassName
import androidx.compose.ui.platform.SemanticsNodeCopy
import androidx.compose.ui.platform.SemanticsNodeWithAdjustedBounds
import androidx.compose.ui.platform.getAllUncoveredSemanticsNodesToIntObjectMap
import androidx.compose.ui.semantics.Role.Companion.Tab
import androidx.compose.ui.semantics.SemanticsActions.OnAutofillText
import androidx.compose.ui.semantics.SemanticsActions.OnClick
import androidx.compose.ui.semantics.SemanticsActions.OnLongClick
import androidx.compose.ui.semantics.SemanticsActions.SetText
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties.ContentDataType as SemanticsContentDataType
import androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
import androidx.compose.ui.semantics.SemanticsProperties.ContentType
import androidx.compose.ui.semantics.SemanticsProperties.Disabled
import androidx.compose.ui.semantics.SemanticsProperties.EditableText
import androidx.compose.ui.semantics.SemanticsProperties.Focused
import androidx.compose.ui.semantics.SemanticsProperties.MaxTextLength
import androidx.compose.ui.semantics.SemanticsProperties.Password
import androidx.compose.ui.semantics.SemanticsProperties.Role
import androidx.compose.ui.semantics.SemanticsProperties.Selected
import androidx.compose.ui.semantics.SemanticsProperties.Text
import androidx.compose.ui.semantics.SemanticsProperties.ToggleableState
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState.On
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.util.fastForEach

/**
 * Semantic autofill implementation for Android.
 *
 * @param view The parent compose view.
 */
@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
internal class AndroidAutofillManager(val view: AndroidComposeView) : AutofillManager {
    internal var autofillManager: AutofillManagerWrapper = AutofillManagerWrapperImpl(view)

    init {
        view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
    }

    private val handler = Handler(Looper.getMainLooper())

    // `previousSemanticsNodes` holds the previous pruned semantics tree so that we can compare the
    // current and previous trees in onSemanticsChange(). We use SemanticsNodeCopy here because
    // SemanticsNode's children are dynamically generated and always reflect the current children.
    // We need to keep a copy of its old structure for comparison.
    private var previousSemanticsNodes: MutableIntObjectMap<SemanticsNodeCopy> =
        mutableIntObjectMapOf()
    private var previousSemanticsRoot =
        SemanticsNodeCopy(view.semanticsOwner.unmergedRootSemanticsNode, intObjectMapOf())
    private var checkingForSemanticsChanges = false

    internal var currentSemanticsNodesInvalidated = true

    internal var currentSemanticsNodes: IntObjectMap<SemanticsNodeWithAdjustedBounds> =
        intObjectMapOf()
        get() {
            if (currentSemanticsNodesInvalidated) { // first instance of retrieving all nodes
                currentSemanticsNodesInvalidated = false
                field = view.semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap()
            }
            return field
        }

    private fun updateSemanticsCopy() {
        previousSemanticsNodes.clear()
        currentSemanticsNodes.forEach { key, value ->
            previousSemanticsNodes[key] =
                SemanticsNodeCopy(value.semanticsNode, currentSemanticsNodes)
        }
        previousSemanticsRoot =
            SemanticsNodeCopy(view.semanticsOwner.unmergedRootSemanticsNode, currentSemanticsNodes)
    }

    private val autofillChangeChecker = Runnable {
        checkForAutofillPropertyChanges(currentSemanticsNodes)
        updateSemanticsCopy()
        checkingForSemanticsChanges = false
    }

    private fun checkForAutofillPropertyChanges(
        newSemanticsNodes: IntObjectMap<SemanticsNodeWithAdjustedBounds>
    ) {
        newSemanticsNodes.forEachKey { id ->
            // We do this search because the new configuration is set as a whole, so we
            // can't indicate which property is changed when setting the new configuration.
            val previousNode = previousSemanticsNodes[id]
            val currNode =
                checkPreconditionNotNull(newSemanticsNodes[id]?.semanticsNode) {
                    "no value for specified key"
                }

            if (previousNode == null) {
                return@forEachKey
            }

            // Notify Autofill that the value has changed if there is a difference between
            // the previous and current values.

            // Check Editable Text —————————
            val previousText = previousNode.unmergedConfig.getOrNull(EditableText)?.text
            val newText = currNode.unmergedConfig.getOrNull(EditableText)?.text
            if (previousText != newText && newText != null) {
                notifyAutofillValueChanged(id, newText)
            }

            // Check Focus —————————
            val previousFocus = previousNode.unmergedConfig.getOrNull(Focused)
            val currFocus = currNode.unmergedConfig.getOrNull(Focused)
            if (previousFocus != true && currFocus == true) {
                notifyViewEntered(id)
            }
            if (previousFocus == true && currFocus != true) {
                notifyViewExited(id)
            }

            // Check Visibility —————————
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val prevTransparency = previousNode.isTransparent
                val currTransparency = currNode.isTransparent
                if (prevTransparency != currTransparency) {
                    notifyVisibilityChanged(id, currTransparency)
                }
            }
        }
    }

    internal fun onSemanticsChange() {
        currentSemanticsNodesInvalidated = true
        if (!checkingForSemanticsChanges) {
            checkingForSemanticsChanges = true
            handler.post(autofillChangeChecker)
        }
    }

    private fun notifyViewEntered(semanticsId: Int) {
        // When a field is entered for the first time, a pop-up dialog should appear. Compose does
        // not need to keep track of whether or not this is the first time a field is entered;
        // the Autofill framework takes care of that. Compose simply calls `showAutofillDialog`
        // each time a field is entered and the dialog appears if the appropriate conditions are
        // met.
        autofillManager.showAutofillDialog(semanticsId)
        currentSemanticsNodes[semanticsId]?.adjustedBounds?.let {
            autofillManager.notifyViewEntered(semanticsId, it)
        }
    }

    private fun notifyViewExited(semanticsId: Int) {
        autofillManager.notifyViewExited(semanticsId)
    }

    private fun notifyAutofillValueChanged(semanticsId: Int, newAutofillValue: Any) {
        val currSemanticsNode = currentSemanticsNodes[semanticsId]?.semanticsNode
        val currDataType = currSemanticsNode?.unmergedConfig?.getOrNull(SemanticsContentDataType)

        when (currDataType) {
            ContentDataType.Text ->
                autofillManager.notifyValueChanged(
                    semanticsId,
                    AutofillValue.forText(newAutofillValue.toString())
                )
            ContentDataType.Date ->
                TODO("b/138604541: Add Autofill support for ContentDataType.Date")
            ContentDataType.List ->
                TODO("b/138604541: Add Autofill support for ContentDataType.List")
            ContentDataType.Toggle ->
                TODO("b/138604541: Add Autofill support for ContentDataType.Toggle")
            else -> {
                TODO("b/138604541: Add Autofill support for ContentDataType.None")
            }
        }
    }

    private fun notifyVisibilityChanged(semanticsId: Int, isInvisible: Boolean) {
        autofillManager.notifyViewVisibilityChanged(semanticsId, !isInvisible)
    }

    @ExperimentalComposeUiApi
    override fun commit() {
        autofillManager.commit()
    }

    override fun cancel() {
        autofillManager.cancel()
    }

    internal fun onTextFillHelper(toFillId: Int, autofillValue: String) {
        // Use mapping to find lambda corresponding w semanticsNodeId,
        // then invoke the lambda. This will change the field text.
        val currSemanticsNode = currentSemanticsNodes[toFillId]?.semanticsNode
        currSemanticsNode
            ?.unmergedConfig
            ?.getOrNull(OnAutofillText)
            ?.action
            ?.invoke(AnnotatedString(autofillValue))
    }

    companion object {
        /**
         * Autofill Manager callback.
         *
         * This callback is called when we receive autofill events. It adds some logs that can be
         * useful for debug purposes.
         */
        internal object AutofillSemanticCallback : PlatformAndroidManager.AutofillCallback() {
            override fun onAutofillEvent(view: View, virtualId: Int, event: Int) {
                super.onAutofillEvent(view, virtualId, event)
                Log.d(
                    "Autofill Status",
                    when (event) {
                        EVENT_INPUT_SHOWN -> "Autofill popup was shown."
                        EVENT_INPUT_HIDDEN -> "Autofill popup was hidden."
                        EVENT_INPUT_UNAVAILABLE ->
                            """
                        |Autofill popup isn't shown because autofill is not available.
                        |
                        |Did you set up autofill?
                        |1. Go to Settings > System > Languages&input > Advanced > Autofill Service
                        |2. Pick a service
                        |
                        |Did you add an account?
                        |1. Go to Settings > System > Languages&input > Advanced
                        |2. Click on the settings icon next to the Autofill Service
                        |3. Add your account
                        """
                                .trimMargin()
                        else -> "Unknown status event."
                    }
                )
            }

            /** Registers the autofill debug callback. */
            fun register(androidAutofillManager: AndroidAutofillManager) {
                androidAutofillManager.autofillManager.autofillManager.registerCallback(this)
            }

            /** Unregisters the autofill debug callback. */
            fun unregister(androidAutofillManager: AndroidAutofillManager) {
                androidAutofillManager.autofillManager.autofillManager.unregisterCallback(this)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun AndroidAutofillManager.populateViewStructure(root: ViewStructure) {
    // Add child nodes. The function returns the index to the first item.
    val count =
        currentSemanticsNodes.count { _, semanticsNodeWithAdjustedBounds ->
            // TODO(333102566): remove the `isRelatedToAutofill` check below
            //  for heuristics based autofill support.
            semanticsNodeWithAdjustedBounds.semanticsNode.unmergedConfig.contains(ContentType) ||
                semanticsNodeWithAdjustedBounds.semanticsNode.unmergedConfig.contains(
                    SemanticsContentDataType
                )
        }
    // TODO(b/138549623): Instead of creating a flattened tree by using the nodes from the map, we
    //  can use SemanticsOwner to get the root SemanticsInfo and create a more representative tree.
    var index = AutofillApi26Helper.addChildCount(root, count)

    // Iterate through currentSemanticsNodes, finding autofill-related nodes
    // and call corresponding APIs on the viewStructure as listed above
    currentSemanticsNodes.forEach { semanticsId, adjustedNode ->
        if (
            adjustedNode.semanticsNode.unmergedConfig.contains(ContentType) ||
                adjustedNode.semanticsNode.unmergedConfig.contains(SemanticsContentDataType)
        ) {
            AutofillApi26Helper.newChild(root, index)?.also { child ->
                AutofillApi26Helper.setAutofillId(
                    child,
                    AutofillApi26Helper.getAutofillId(root)!!,
                    semanticsId
                )
                AutofillApi26Helper.setId(child, semanticsId, view.context.packageName, null, null)

                adjustedNode.semanticsNode.unmergedConfig.getOrNull(SemanticsContentDataType)?.let {
                    AutofillApi26Helper.setAutofillTypeForViewStruct(child, it)
                }

                adjustedNode.semanticsNode.unmergedConfig
                    .getOrNull(ContentType)
                    ?.contentHints
                    ?.toTypedArray()
                    ?.let { AutofillApi26Helper.setAutofillHints(child, it) }

                adjustedNode.adjustedBounds.run {
                    AutofillApi26Helper.setDimens(child, left, top, 0, 0, width(), height())
                }

                adjustedNode.semanticsNode.populateViewStructure(child)
            }
            index++
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun SemanticsNode.populateViewStructure(child: ViewStructure) {
    // ———————— Interactions (clicking, checking, selecting, etc.)
    AutofillApi26Helper.setClickable(child, unmergedConfig.contains(OnClick))
    AutofillApi26Helper.setCheckable(child, unmergedConfig.contains(ToggleableState))
    AutofillApi26Helper.setEnabled(child, (!config.contains(Disabled)))
    AutofillApi26Helper.setFocused(child, unmergedConfig.getOrNull(Focused) == true)
    AutofillApi26Helper.setFocusable(child, unmergedConfig.contains(Focused))
    AutofillApi26Helper.setLongClickable(child, unmergedConfig.contains(OnLongClick))
    unmergedConfig.getOrNull(Selected)?.let { AutofillApi26Helper.setSelected(child, it) }

    unmergedConfig.getOrNull(ToggleableState)?.let {
        AutofillApi26Helper.setChecked(child, it == On)
    }
    // TODO(MNUZEN): Set setAccessibilityFocused as well

    // ———————— Visibility, elevation, alpha
    // Transparency should be the only thing affecting View.VISIBLE (pruning will take care of all
    // covered nodes).
    // TODO(mnuzen): since we are removing pruning in semantics/accessibility with `semanticInfo`,
    // double check that this is the correct behavior even after switching.
    AutofillApi26Helper.setVisibility(
        child,
        if (!isTransparent || isRoot) View.VISIBLE else View.INVISIBLE
    )

    // TODO(335726351): will call the below method when b/335726351 has been fulfilled and
    // `isOpaque` is added back.
    // AutofillApi26Helper.setOpaque(child, isOpaque)

    // ———————— Text, role, content description
    config.getOrNull(Text)?.let { textList ->
        var concatenatedText = ""
        textList.fastForEach { text -> concatenatedText += text.text + "\n" }
        AutofillApi26Helper.setText(child, concatenatedText)
        AutofillApi26Helper.setClassName(child, TextClassName)
    }

    unmergedConfig.getOrNull(MaxTextLength)?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            AutofillApi28Helper.setMaxTextLength(child, it)
        }
    }

    val role = unmergedConfig.getOrNull(Role)
    role?.let {
        if (isFake || replacedChildren.isEmpty()) {
            AutofillApi26Helper.setClassName(child, it.toLegacyClassName())
        }
    }

    if (unmergedConfig.contains(SetText)) {
        // If `SetText` action exists, then the element is a TextField
        AutofillApi26Helper.setClassName(child, TextFieldClassName)
        // If the element is a TextField, then we also want to set the current text value for
        // autofill. (This data is used when we save autofilled values.)
        unmergedConfig.getOrNull(Text)?.let { textList ->
            var concatenatedText = ""
            textList.fastForEach { text -> concatenatedText += text.text + "\n" }
            AutofillApi26Helper.setAutofillValue(
                child,
                AutofillApi26Helper.getAutofillTextValue(concatenatedText)
            )
        }
    }

    unmergedConfig.getOrNull(Selected)?.let {
        if (role == Tab) {
            AutofillApi26Helper.setSelected(child, it)
        } else {
            AutofillApi26Helper.setCheckable(child, true)
            AutofillApi26Helper.setChecked(child, it)
        }
    }

    unmergedConfig.getOrNull(ContentDescription)?.firstOrNull()?.let {
        AutofillApi26Helper.setContentDescription(child, it)
    }

    // ———————— Parsing autofill hints and types
    // If there is no explicitly set data type, parse it from semantics.
    if (unmergedConfig.contains(SetText)) {
        if (!unmergedConfig.contains(SemanticsContentDataType)) {
            AutofillApi26Helper.setAutofillType(child, View.AUTOFILL_TYPE_TEXT)
        }
    }

    // If it's a password, setInputType and the sensitive flag
    if (unmergedConfig.contains(Password)) {
        AutofillApi26Helper.setInputType(
            child,
            InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        )
        AutofillApi26Helper.setDataIsSensitive(child, true)
    }

    // if the toggleableState is not null, set autofillType to AUTOFILL_TYPE_TOGGLE
    if (unmergedConfig.contains(ToggleableState)) {
        AutofillApi26Helper.setAutofillType(child, View.AUTOFILL_TYPE_TOGGLE)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun AndroidAutofillManager.performAutofill(values: SparseArray<AutofillValue>) {
    for (index in 0 until values.size()) {
        val itemId = values.keyAt(index)
        val value = values[itemId]
        when {
            AutofillApi26Helper.isText(value) -> // passing in the autofillID
            onTextFillHelper(itemId, AutofillApi26Helper.textValue(value).toString())
            AutofillApi26Helper.isDate(value) ->
                TODO("b/138604541: Add Autofill support for ContentDataType.Date")
            AutofillApi26Helper.isList(value) ->
                TODO("b/138604541: Add Autofill support for ContentDataType.List")
            AutofillApi26Helper.isToggle(value) ->
                TODO("b/138604541: Add Autofill support for ContentDataType.Toggle")
        }
    }
}

/** Wrapper for the final AutofillManager class. This can be mocked in testing. */
@RequiresApi(Build.VERSION_CODES.O)
internal interface AutofillManagerWrapper {
    val autofillManager: PlatformAndroidManager

    fun notifyViewEntered(semanticsId: Int, bounds: Rect)

    fun notifyViewExited(semanticsId: Int)

    fun notifyValueChanged(semanticsId: Int, autofillValue: AutofillValue)

    fun notifyViewVisibilityChanged(semanticsId: Int, isVisible: Boolean)

    fun showAutofillDialog(semanticsId: Int)

    fun commit()

    fun cancel()
}

@RequiresApi(Build.VERSION_CODES.O)
private class AutofillManagerWrapperImpl(val view: View) : AutofillManagerWrapper {
    override val autofillManager =
        view.context.getSystemService(PlatformAndroidManager::class.java)
            ?: error("Autofill service could not be located.")

    override fun notifyViewEntered(semanticsId: Int, bounds: Rect) {
        autofillManager.notifyViewEntered(view, semanticsId, bounds)
    }

    override fun notifyViewExited(semanticsId: Int) {
        autofillManager.notifyViewExited(view, semanticsId)
    }

    override fun notifyValueChanged(semanticsId: Int, autofillValue: AutofillValue) {
        autofillManager.notifyValueChanged(view, semanticsId, autofillValue)
    }

    override fun notifyViewVisibilityChanged(semanticsId: Int, isVisible: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            AutofillApi27Helper.notifyViewVisibilityChanged(
                view,
                autofillManager,
                semanticsId,
                isVisible
            )
        }
    }

    override fun showAutofillDialog(semanticsId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AutofillApi33Helper.showAutofillDialog(view, autofillManager, semanticsId)
        }
    }

    override fun commit() {
        autofillManager.commit()
    }

    override fun cancel() {
        autofillManager.cancel()
    }
}
