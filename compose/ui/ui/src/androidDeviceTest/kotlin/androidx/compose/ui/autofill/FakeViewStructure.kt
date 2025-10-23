/*
 * Copyright 2021 The Android Open Source Project
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

import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi

/**
 * A fake implementation of [ViewStructure] to use in tests.
 *
 * We use a data class to get an equals and toString implementation. The properties are marked as
 *
 * @JvmField so that they don't clash with the set* functions in the ViewStructure interface that
 *   this class implements.
 */
@RequiresApi(Build.VERSION_CODES.M)
internal data class FakeViewStructure(
    @JvmField var virtualId: Int = 0,
    @JvmField var packageName: String? = null,
    @JvmField var typeName: String? = null,
    @JvmField var entryName: String? = null,
    @JvmField var children: MutableList<FakeViewStructure> = mutableListOf(),
    @JvmField var bounds: Rect? = null,
    @JvmField var autofillId: AutofillId? = null,
    @JvmField var isCheckable: Boolean = false,
    @JvmField var isFocusable: Boolean = false,
    @JvmField var autofillType: Int = View.AUTOFILL_TYPE_NONE,
    @JvmField var autofillHints: MutableList<String> = mutableListOf(),
    @JvmField var activated: Boolean = false,
    @JvmField var alpha: Float = 1f,
    @JvmField var autofillOptions: MutableList<CharSequence>? = null,
    @JvmField var autofillValue: AutofillValue? = null,
    @JvmField var className: String? = null,
    @JvmField var contentDescription: CharSequence? = null,
    @JvmField var dataIsSensitive: Boolean = false,
    @JvmField var elevation: Float = 0f,
    @JvmField var hint: CharSequence? = null,
    @JvmField var htmlInfo: HtmlInfo? = null,
    @JvmField var inputType: Int = 0,
    @JvmField var isEnabled: Boolean = true,
    @JvmField var isAccessibilityFocused: Boolean = false,
    @JvmField var isChecked: Boolean = false,
    @JvmField var isClickable: Boolean = false,
    @JvmField var isContextClickable: Boolean = false,
    @JvmField var isFocused: Boolean = false,
    @JvmField var isLongClickable: Boolean = false,
    @JvmField var isOpaque: Boolean = false,
    @JvmField var isSelected: Boolean = false,
    @JvmField var text: CharSequence = "",
    @JvmField var textLinesCharOffsets: MutableList<Int>? = null,
    @JvmField var textLinesBaselines: MutableList<Int>? = null,
    @JvmField var transformation: Matrix? = null,
    @JvmField var visibility: Int = View.VISIBLE,
    @JvmField var maxTextLength: Int = -1,
    @JvmField var webDomain: String? = null,
) : ViewStructure() {
    @JvmField var extras: Bundle = Bundle()

    override fun getChildCount() = children.count()

    override fun addChildCount(childCount: Int): Int {
        repeat(childCount) { children.add(FakeViewStructure()) }
        return children.size - childCount
    }

    override fun newChild(index: Int): FakeViewStructure {
        if (index >= children.size) error("Call addChildCount() before calling newChild()")
        return children[index]
    }

    override fun getAutofillId(): AutofillId? = autofillId

    override fun setAutofillId(id: AutofillId) {
        autofillId = id
    }

    override fun setAutofillId(rootId: AutofillId, virtualId: Int) {
        autofillId = rootId
        this.virtualId = virtualId
    }

    override fun setId(
        virtualId: Int,
        packageName: String?,
        typeName: String?,
        entryName: String?,
    ) {
        this.virtualId = virtualId
        this.packageName = packageName
        this.typeName = typeName
        this.entryName = entryName
    }

    override fun setAutofillType(autofillType: Int) {
        this.autofillType = autofillType
    }

    override fun setAutofillHints(autofillHints: Array<out String>?) {
        autofillHints?.let { this.autofillHints = it.toMutableList() }
    }

    override fun setDimens(left: Int, top: Int, x: Int, y: Int, width: Int, height: Int) {
        this.bounds = Rect(left, top, left + width, top + height)
    }

    override fun getExtras() = extras

    override fun getHint() = hint ?: ""

    override fun getText() = text

    override fun hasExtras() = !extras.isEmpty

    override fun setActivated(state: Boolean) {
        activated = state
    }

    override fun setAccessibilityFocused(state: Boolean) {
        isAccessibilityFocused = state
    }

    override fun setAlpha(alpha: Float) {
        this.alpha = alpha
    }

    override fun setAutofillOptions(options: Array<CharSequence>?) {
        autofillOptions = options?.toMutableList()
    }

    override fun setAutofillValue(value: AutofillValue?) {
        autofillValue = value
    }

    override fun setCheckable(state: Boolean) {
        isCheckable = state
    }

    override fun setChecked(state: Boolean) {
        isChecked = state
    }

    override fun setClassName(className: String?) {
        this.className = className
    }

    override fun setClickable(state: Boolean) {
        isClickable = state
    }

    override fun setContentDescription(contentDescription: CharSequence?) {
        this.contentDescription = contentDescription
    }

    override fun setContextClickable(state: Boolean) {
        isContextClickable = state
    }

    override fun setDataIsSensitive(sensitive: Boolean) {
        dataIsSensitive = sensitive
    }

    override fun setElevation(elevation: Float) {
        this.elevation = elevation
    }

    override fun setEnabled(state: Boolean) {
        isEnabled = state
    }

    override fun setFocusable(state: Boolean) {
        isFocusable = state
    }

    override fun setFocused(state: Boolean) {
        isFocused = state
    }

    override fun setHtmlInfo(htmlInfo: HtmlInfo) {
        this.htmlInfo = htmlInfo
    }

    override fun setHint(hint: CharSequence?) {
        this.hint = hint
    }

    override fun setInputType(inputType: Int) {
        this.inputType = inputType
    }

    override fun setLongClickable(state: Boolean) {
        isLongClickable = state
    }

    override fun setMaxTextLength(maxLength: Int) {
        maxTextLength = maxLength
    }

    override fun setOpaque(opaque: Boolean) {
        isOpaque = opaque
    }

    override fun setSelected(state: Boolean) {
        isSelected = state
    }

    override fun setText(charSequence: CharSequence?) {
        charSequence?.let { text = it }
    }

    override fun setText(charSequence: CharSequence?, selectionStart: Int, selectionEnd: Int) {
        charSequence?.let { text = it.subSequence(selectionStart, selectionEnd) }
    }

    override fun setTextLines(charOffsets: IntArray?, baselines: IntArray?) {
        textLinesCharOffsets = charOffsets?.toMutableList()
        textLinesBaselines = baselines?.toMutableList()
    }

    override fun setTransformation(matrix: Matrix?) {
        transformation = matrix
    }

    override fun setVisibility(visibility: Int) {
        this.visibility = visibility
    }

    override fun setWebDomain(domain: String?) {
        webDomain = domain
    }

    // Unimplemented methods.
    override fun asyncCommit() {
        TODO("not implemented")
    }

    override fun asyncNewChild(index: Int): ViewStructure {
        TODO("not implemented")
    }

    override fun getTextSelectionEnd(): Int {
        TODO("not implemented")
    }

    override fun getTextSelectionStart(): Int {
        TODO("not implemented")
    }

    override fun newHtmlInfoBuilder(tagName: String): HtmlInfo.Builder {
        TODO("not implemented")
    }

    override fun setChildCount(num: Int) {
        TODO("not implemented")
    }

    override fun setLocaleList(localeList: LocaleList?) {
        TODO("not implemented")
    }

    override fun setTextStyle(size: Float, fgColor: Int, bgColor: Int, style: Int) {
        TODO("not implemented")
    }
}
