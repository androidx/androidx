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

package androidx.pdf.utils

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RestrictTo
import androidx.pdf.RenderParams
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageImageContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.models.ListItem
import kotlin.math.roundToInt

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.models.PageMatchBounds.toContentClass(): PageMatchBounds =
    requireSdkExtensionVersion {
        PageMatchBounds(bounds, textStartIndex)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun SelectionBoundary.toAndroidClass():
    android.graphics.pdf.models.selection.SelectionBoundary {
    return requireSdkExtensionVersion {
        point?.let { android.graphics.pdf.models.selection.SelectionBoundary(it) }
            ?: android.graphics.pdf.models.selection.SelectionBoundary(index)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.models.selection.SelectionBoundary.toContentClass():
    SelectionBoundary {
    return requireSdkExtensionVersion {
        if (point == null) {
            SelectionBoundary(index = index, isRtl = isRtl)
        }
        SelectionBoundary(point = point, isRtl = isRtl)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.models.selection.PageSelection.toContentClass(): PageSelection =
    requireSdkExtensionVersion {
        PageSelection(
            page = page,
            start = start.toContentClass(),
            stop = stop.toContentClass(),
            selectedContents = selectedTextContents.map { it.toContentClass() },
        )
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.content.PdfPageTextContent.toContentClass(): PdfPageTextContent =
    requireSdkExtensionVersion {
        PdfPageTextContent(bounds, text)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.content.PdfPageImageContent.toContentClass(): PdfPageImageContent =
    requireSdkExtensionVersion {
        PdfPageImageContent(altText)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.content.PdfPageGotoLinkContent.toContentClass():
    PdfPageGotoLinkContent = requireSdkExtensionVersion {
    PdfPageGotoLinkContent(bounds, destination.toContentClass())
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.content.PdfPageGotoLinkContent.Destination.toContentClass():
    PdfPageGotoLinkContent.Destination = requireSdkExtensionVersion {
    PdfPageGotoLinkContent.Destination(pageNumber, xCoordinate, yCoordinate, zoom)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.content.PdfPageLinkContent.toContentClass(): PdfPageLinkContent =
    requireSdkExtensionVersion {
        PdfPageLinkContent(bounds, uri)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("WrongConstant")
public fun android.graphics.pdf.models.FormWidgetInfo.toContentClass(): FormWidgetInfo =
    requireSdkExtensionVersion {
        return when (widgetType) {
            FormWidgetInfo.WIDGET_TYPE_CHECKBOX ->
                FormWidgetInfo.createCheckbox(
                    widgetIndex,
                    widgetRect,
                    textValue,
                    accessibilityLabel,
                    isReadOnly,
                )

            FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON ->
                FormWidgetInfo.createPushButton(
                    widgetIndex,
                    widgetRect,
                    textValue,
                    accessibilityLabel,
                    isReadOnly,
                )

            FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON ->
                FormWidgetInfo.createRadioButton(
                    widgetIndex,
                    widgetRect,
                    textValue,
                    accessibilityLabel,
                    isReadOnly,
                )

            FormWidgetInfo.WIDGET_TYPE_SIGNATURE ->
                FormWidgetInfo.createSignature(
                    widgetIndex,
                    widgetRect,
                    textValue,
                    accessibilityLabel,
                    isReadOnly,
                )

            FormWidgetInfo.WIDGET_TYPE_COMBOBOX ->
                FormWidgetInfo.createComboBox(
                    widgetIndex,
                    widgetRect,
                    textValue,
                    accessibilityLabel,
                    isReadOnly,
                    isEditableText,
                    fontSize,
                    listItems.map { item -> item.toContentClass() },
                )

            FormWidgetInfo.WIDGET_TYPE_TEXTFIELD ->
                FormWidgetInfo.createTextField(
                    widgetIndex,
                    widgetRect,
                    textValue,
                    accessibilityLabel,
                    isReadOnly,
                    isEditableText,
                    isMultiLineText,
                    maxLength.takeIf { it != -1 } ?: 0,
                    fontSize,
                )

            FormWidgetInfo.WIDGET_TYPE_LISTBOX ->
                FormWidgetInfo.createListBox(
                    widgetIndex,
                    widgetRect,
                    textValue,
                    accessibilityLabel,
                    isReadOnly,
                    isMultiSelect,
                    listItems.map { item -> item.toContentClass() },
                )

            else -> throw IllegalArgumentException("Unknown widget type")
        }
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.models.ListItem.toContentClass(): ListItem =
    requireSdkExtensionVersion {
        ListItem(label, isSelected)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("WrongConstant")
public fun FormEditInfo.toAndroidClass(): android.graphics.pdf.models.FormEditRecord =
    requireSdkExtensionVersion {
        val builder =
            android.graphics.pdf.models.FormEditRecord.Builder(type, pageNumber, widgetIndex)
        when (type) {
            FormEditInfo.EDIT_TYPE_CLICK -> {
                clickPoint?.let {
                    builder.setClickPoint(Point(it.x.roundToInt(), it.y.roundToInt()))
                }
            }
            FormEditInfo.EDIT_TYPE_SET_TEXT -> builder.setText(text)
            FormEditInfo.EDIT_TYPE_SET_INDICES -> {
                val selectedIndices = IntArray(selectedIndexCount)
                for (i in 0 until selectedIndexCount) {
                    selectedIndices[i] = getSelectedIndexAt(i)
                }
                builder.setSelectedIndices(selectedIndices)
            }
            else -> {}
        }
        builder.build()
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("WrongConstant")
public fun RenderParams.toAndroidClass(): android.graphics.pdf.RenderParams =
    requireSdkExtensionVersion {
        val builder = android.graphics.pdf.RenderParams.Builder(renderMode)
        builder.setRenderFlags(renderFlags)
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 19) {
            builder.setRenderFormContentMode(renderFormContentMode)
        }
        builder.build()
    }

private inline fun <T> requireSdkExtensionVersion(block: () -> T): T {
    return if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
        block()
    } else {
        throw UnsupportedOperationException("Operation supported above S")
    }
}
