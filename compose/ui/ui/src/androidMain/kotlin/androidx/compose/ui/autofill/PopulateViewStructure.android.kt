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

import android.os.Build
import android.text.InputType
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.TextClassName
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.TextFieldClassName
import androidx.compose.ui.platform.toLegacyClassName
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.mergedSemanticsConfiguration
import androidx.compose.ui.spatial.RectManager
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.state.ToggleableState.On
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.util.fastForEach
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat

@RequiresApi(Build.VERSION_CODES.O)
internal fun ViewStructure.populate(
    semanticsInfo: SemanticsInfo,
    rootAutofillId: AutofillId,
    packageName: String?,
    rectManager: RectManager,
) {
    val autofillApi = AutofillApi26Helper
    val properties = SemanticsProperties
    val actions = SemanticsActions

    // Semantics properties.
    var contentDataTypeProp: ContentDataType? = null
    var contentTypeProp: ContentType? = null
    var editableTextProp: AnnotatedString? = null
    var isPasswordProp = false
    var fillableDataProp: AndroidFillableData? = null
    // We will set the `isSensitiveData` prop to true by default; the only way this value is false
    // is if the developer explicitly marks `isSensitiveData = false` on a node. This mirrors
    // the `setDataIsSensitive` flag on `ViewStructure.java`, which states that "by default, all
    // nodes are assumed to be sensitive."
    var isSensitiveDataProp = true
    var maxTextLengthProp: Int? = null
    var roleProp: Role? = null
    var selectedProp: Boolean? = null
    var toggleableStateProp: ToggleableState? = null

    // Semantics properties form merged configuration.
    var textMergedProp: List<AnnotatedString>? = null

    // Semantics actions.
    var hasSetTextAction = false

    semanticsInfo.semanticsConfiguration?.props?.forEach { property, value ->
        @Suppress("UNCHECKED_CAST")
        when (property) {
            properties.ContentDataType -> contentDataTypeProp = value as ContentDataType
            properties.ContentDescription ->
                (value as List<String>).firstOrNull()?.let {
                    autofillApi.setContentDescription(this, it)
                }
            properties.ContentType -> contentTypeProp = value as ContentType
            properties.FillableData -> fillableDataProp = value as AndroidFillableData
            properties.EditableText -> editableTextProp = value as AnnotatedString
            properties.Focused -> autofillApi.setFocused(this, value as Boolean)
            properties.MaxTextLength -> maxTextLengthProp = value as Int
            properties.Password -> isPasswordProp = true
            properties.IsSensitiveData -> isSensitiveDataProp = value as Boolean
            properties.Role -> roleProp = value as Role
            properties.Selected -> selectedProp = value as Boolean
            properties.ToggleableState -> toggleableStateProp = value as ToggleableState
            actions.OnClick -> autofillApi.setClickable(this, true)
            actions.OnLongClick -> autofillApi.setLongClickable(this, true)
            actions.RequestFocus -> autofillApi.setFocusable(this, true)
            actions.SetText -> hasSetTextAction = true
        }
    }

    semanticsInfo.mergedSemanticsConfiguration()?.props?.forEach { property, value ->
        @Suppress("UNCHECKED_CAST")
        when (property) {
            properties.Disabled -> autofillApi.setEnabled(this, false)
            properties.Text -> textMergedProp = value as List<AnnotatedString>
        }
    }

    // Id.
    val semanticsId =
        semanticsInfo.semanticsId.takeUnless { semanticsInfo.parentInfo == null }
            ?: AccessibilityNodeProviderCompat.HOST_VIEW_ID
    autofillApi.setAutofillId(this, rootAutofillId, semanticsId)
    autofillApi.setId(this, semanticsId, packageName, null, null)

    // Autofill Type.
    val autofillType =
        contentDataTypeProp?.dataType
            ?: when {
                hasSetTextAction -> View.AUTOFILL_TYPE_TEXT
                toggleableStateProp != null -> View.AUTOFILL_TYPE_TOGGLE
                else -> null
            }
    autofillType?.let { autofillApi.setAutofillType(this, it) }

    // Use autofillTextValue first, and then overwrite it with autofillValue (if present).
    editableTextProp?.let { textProp ->
        autofillApi.setAutofillValue(this, autofillApi.getAutofillTextValue(textProp.text))
    }
    fillableDataProp?.let { fillableData ->
        fillableData.autofillValue.let { autofillApi.setAutofillValue(this, it) }
    }

    // Autofill Hints.
    contentTypeProp?.contentHints?.let { autofillApi.setAutofillHints(this, it) }

    // Dimensions.
    rectManager.rects.withRect(semanticsInfo.semanticsId) { left, top, right, bottom ->
        autofillApi.setDimens(this, left, top, 0, 0, right - left, bottom - top)
    }

    // Selected.
    selectedProp?.let { autofillApi.setSelected(this, it) }

    // Checkable.
    val toggleableState = toggleableStateProp
    val selected = selectedProp
    if (toggleableState != null) {
        autofillApi.setCheckable(this, true)
        autofillApi.setChecked(this, toggleableState == On)
    } else if (selected != null && roleProp != Role.Tab) {
        autofillApi.setCheckable(this, true)
        autofillApi.setChecked(this, selected)
    }

    // Password.
    val passwordHint = ContentType.Password.contentHints.first()
    val contentTypePassword = contentTypeProp?.contentHints?.contains(passwordHint) == true
    val isPassword = isPasswordProp || contentTypePassword
    val isSensitive = isPassword || isSensitiveDataProp
    autofillApi.setDataIsSensitive(this, isSensitive)

    // Visibility.
    // TODO(b/383198004): This only checks transparency. We should also check whether the layoutNode
    //  is within visible bounds.
    autofillApi.setVisibility(this, if (semanticsInfo.isTransparent()) INVISIBLE else VISIBLE)

    // TODO(335726351): will call the below method when b/335726351 has been fulfilled and
    // `isOpaque` is added back.
    // autofillApi.setOpaque(this, isOpaque)

    // Text.
    textMergedProp?.let {
        var concatenatedText = ""
        it.fastForEach { text -> concatenatedText += text.text + "\n" }
        autofillApi.setText(this, concatenatedText)
        autofillApi.setClassName(this, TextClassName)
    }

    // Role.
    if (semanticsInfo.childrenInfo.isEmpty()) {
        roleProp?.toLegacyClassName()?.let { autofillApi.setClassName(this, it) }
    }

    // TextField.
    if (hasSetTextAction) {
        autofillApi.setClassName(this, TextFieldClassName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            maxTextLengthProp?.let { AutofillApi28Helper.setMaxTextLength(this, it) }
        }

        // Password.
        if (isPassword) {
            autofillApi.setInputType(
                this,
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
            )
        }
    }
}
