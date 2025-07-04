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
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RestrictTo
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageImageContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.models.FormEditRecord
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.models.ListItem

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
            selectedTextContents = selectedTextContents.map { it.toContentClass() },
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
        FormWidgetInfo(
            widgetType,
            widgetIndex,
            widgetRect,
            textValue,
            accessibilityLabel,
            isReadOnly,
            isEditableText,
            isMultiSelect,
            isMultiLineText,
            maxLength = maxLength.takeIf { it != -1 },
            fontSize = fontSize.takeIf { it.toDouble() != 0.0 },
            listItems.map { item -> item.toContentClass() }.takeIf { it.isNotEmpty() },
        )
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun android.graphics.pdf.models.ListItem.toContentClass(): ListItem =
    requireSdkExtensionVersion {
        ListItem(label, isSelected)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("WrongConstant")
public fun FormEditRecord.toAndroidClass(): android.graphics.pdf.models.FormEditRecord =
    requireSdkExtensionVersion {
        val builder =
            android.graphics.pdf.models.FormEditRecord.Builder(type, pageNumber, widgetIndex)
        when (type) {
            FormEditRecord.EDIT_TYPE_CLICK -> builder.setClickPoint(clickPoint)
            FormEditRecord.EDIT_TYPE_SET_TEXT -> builder.setText(text)
            FormEditRecord.EDIT_TYPE_SET_INDICES -> builder.setSelectedIndices(selectedIndices)
            else -> {}
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
