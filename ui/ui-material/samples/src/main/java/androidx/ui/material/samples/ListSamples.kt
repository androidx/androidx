/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.foundation.Image
import androidx.ui.foundation.Text
import androidx.ui.graphics.ImageAsset
import androidx.ui.layout.Column
import androidx.ui.material.Divider
import androidx.ui.material.ListItem

@Sampled
@Composable
fun OneLineListItems(icon24x24: ImageAsset, icon40x40: ImageAsset, icon56x56: ImageAsset) {
    Column {
        ListItem(text = "One line list item with no icon")
        Divider()
        ListItem(text = "פריט ברשימה אחת עם תמונה.", icon = icon24x24)
        Divider()
        ListItem(text = "One line list item with 24x24 icon", icon = icon24x24)
        Divider()
        ListItem(text = "One line list item with 40x40 icon", icon = icon40x40)
        Divider()
        ListItem(text = "One line list item with 56x56 icon", icon = icon56x56)
        Divider()
        ListItem(text = "One line clickable list item", icon = icon56x56, onClick = {})
        Divider()
        ListItem(
            text = { Text("One line list item with trailing icon") },
            trailing = { Image(icon24x24) }
        )
        Divider()
        ListItem(
            text = { Text("One line list item") },
            icon = { Image(icon40x40) },
            trailing = { Image(icon24x24) }
        )
        Divider()
    }
}

@Sampled
@Composable
fun TwoLineListItems(icon24x24: ImageAsset, icon40x40: ImageAsset) {
    Column {
        ListItem(text = "Two line list item", secondaryText = "Secondary text")
        Divider()
        ListItem(text = "Two line list item", overlineText = "OVERLINE")
        Divider()
        ListItem(
            text = "Two line list item with 24x24 icon",
            secondaryText = "Secondary text",
            icon = icon24x24
        )
        Divider()
        ListItem(
            text = "Two line list item with 40x40 icon",
            secondaryText = "Secondary text",
            icon = icon40x40
        )
        Divider()
        ListItem(
            text = "Two line list item with 40x40 icon",
            secondaryText = "Secondary text",
            metaText = "meta",
            icon = icon40x40
        )
        Divider()
        ListItem(
            text = { Text("Two line list item") },
            secondaryText = { Text("Secondary text") },
            icon = { Image(icon40x40) },
            trailing = {
                // TODO(popam): put checkbox here after b/140292836 is fixed
                Image(icon24x24)
            }
        )
        Divider()
    }
}

@Sampled
@Composable
fun ThreeLineListItems(icon24x24: ImageAsset, icon40x40: ImageAsset) {
    Column {
        ListItem(
            text = "Three line list item",
            secondaryText = "This is a long secondary text for the current list item, displayed" +
                    " on two lines",
            singleLineSecondaryText = false,
            metaText = "meta"
        )
        Divider()
        ListItem(
            text = "Three line list item",
            overlineText = "OVERLINE",
            secondaryText = "Secondary text"
        )
        Divider()
        ListItem(
            text = "Three line list item with 24x24 icon",
            secondaryText = "This is a long secondary text for the current list item, displayed" +
                    " on two lines",
            singleLineSecondaryText = false,
            icon = icon24x24
        )
        Divider()
        ListItem(
            text = { Text("Three line list item with trailing icon") },
            secondaryText = { Text("This is a long secondary text for the current list" +
                " item, displayed on two lines") },
            singleLineSecondaryText = false,
            trailing = { Image(icon40x40) }
        )
        Divider()
        ListItem(
            text = "Three line list item",
            overlineText = "OVERLINE",
            secondaryText = "Secondary text",
            metaText = "meta"
        )
        Divider()
    }
}
