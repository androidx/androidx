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

package androidx.compose.foundation.text.contextmenu.builder

import android.content.res.Resources
import android.view.textclassifier.TextClassification
import androidx.annotation.DrawableRes
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuTextClassificationItem

/**
 * Adds an item to the list of text context menu components.
 *
 * @param key A unique key that identifies this item. Used to identify context menu items in the
 *   context menu. It is advisable to use a `data object` as a key here.
 * @param label string to display as the text of the item.
 * @param leadingIcon Icon that precedes the label in the context menu. This is expected to be a
 *   drawable resource reference. Setting this to the default value [Resources.ID_NULL] means that
 *   it will not be displayed.
 * @param onClick Action to perform upon the item being clicked/pressed.
 * @sample androidx.compose.foundation.samples.AppendItemToTextContextMenuAndroid
 */
fun TextContextMenuBuilderScope.item(
    key: Any,
    label: String,
    @DrawableRes leadingIcon: Int = Resources.ID_NULL,
    onClick: TextContextMenuSession.() -> Unit,
) {
    addComponent(TextContextMenuItem(key, label, leadingIcon, onClick))
}

/**
 * Adds an item returned by [TextClassification] to the list of text context menu components.
 *
 * @param textClassification The [TextClassification] object returned by
 *   [android.view.textclassifier.TextClassifier].
 * @param index The index of the item in the list of [TextClassification.getActions] or a negative
 *   value if the [TextClassification] hold an legacy assist item.
 */
internal fun TextContextMenuBuilderScope.textClassificationItem(
    key: Any,
    textClassification: TextClassification,
    index: Int,
) {
    addComponent(TextContextMenuTextClassificationItem(key, textClassification, index))
}
