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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList

@Composable
fun ListItemSampleUsage() {
    VerticalList(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { ListItemSample() }
        item { ListItemWithSupportingLabelSample() }
        item { ListItemWithSupportingLabelAndLeadingIconSample() }
        item { ListItemWithSupportingLabelAndIcons() }
        item { ListItemWithLeadingIconLongText() }
        item { ListItemWithSupportingLabelAndLeadingIconLongText() }
    }
}

@Sampled
@Composable
fun ListItemSample() {
    ListItem { Text("Primary Label") }
}

@Sampled
@Composable
fun ListItemWithSupportingLabelSample() {
    ListItem(supportingLabel = { Text("Supporting Label") }) { Text("Primary Label") }
}

@Sampled
@Composable
fun ListItemWithSupportingLabelAndLeadingIconSample() {
    ListItem(
        supportingLabel = { Text("Supporting Label") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("Primary Label")
    }
}

@Sampled
@Composable
fun ClickableListItemSample() {
    ListItem(onClick = {}) { Text("Primary Label") }
}

@Sampled
@Composable
fun ClickableListItemWithSupportingLabelSample() {
    ListItem(onClick = {}, supportingLabel = { Text("Supporting Label") }) { Text("Primary Label") }
}

@Sampled
@Composable
fun ClickableListItemWithSupportingLabelAndLeadingIconSample() {
    ListItem(
        onClick = {},
        supportingLabel = { Text("Supporting Label") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("Primary Label")
    }
}

@Composable
private fun ListItemWithSupportingLabelAndIcons() {
    ListItem(
        supportingLabel = { Text("Supporting Label") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
        trailingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("Primary Label")
    }
}

@Composable
private fun ListItemWithLeadingIconLongText() {
    ListItem(leadingIcon = { Icon(FavoriteIcon, "Localized description") }) {
        Text("Primary label with some very long text that will wrap to multiple lines")
    }
}

@Composable
private fun ListItemWithSupportingLabelAndLeadingIconLongText() {
    ListItem(
        supportingLabel = { Text("Supporting Label") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("Primary label with some very long text that will wrap to multiple lines")
    }
}

@Preview
@Composable
private fun ListItemPreview() {
    GlimmerTheme { ListItemSample() }
}

@Preview
@Composable
private fun ListItemWithSupportingLabelPreview() {
    GlimmerTheme { ListItemWithSupportingLabelSample() }
}

@Preview
@Composable
private fun ListItemWithSupportingLabelAndLeadingIconPreview() {
    GlimmerTheme { ListItemWithSupportingLabelAndLeadingIconSample() }
}

@Preview
@Composable
private fun ClickableListItemPreview() {
    GlimmerTheme { ClickableListItemSample() }
}

@Preview
@Composable
private fun ClickableListItemWithSupportingLabelPreview() {
    GlimmerTheme { ClickableListItemWithSupportingLabelSample() }
}

@Preview
@Composable
private fun ClickableListItemWithSupportingLabelAndLeadingIconPreview() {
    GlimmerTheme { ClickableListItemWithSupportingLabelAndLeadingIconSample() }
}

@Preview
@Composable
private fun ListItemWithSupportingLabelAndIconsPreview() {
    GlimmerTheme { ListItemWithSupportingLabelAndIcons() }
}

@Preview
@Composable
private fun ListItemWithLeadingIconLongTextPreview() {
    GlimmerTheme { ListItemWithLeadingIconLongText() }
}

@Preview
@Composable
private fun ListItemWithSupportingLabelAndLeadingIconLongTextPreview() {
    GlimmerTheme { ListItemWithSupportingLabelAndLeadingIconLongText() }
}
