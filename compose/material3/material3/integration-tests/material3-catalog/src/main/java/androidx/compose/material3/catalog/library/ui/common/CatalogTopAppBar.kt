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

package androidx.compose.material3.catalog.library.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.catalog.library.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogTopAppBar(
    title: String,
    showBackNavigationIcon: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBackClick: () -> Unit = {},
    favorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onGuidelinesClick: () -> Unit = {},
    onDocsClick: () -> Unit = {},
    onSourceClick: () -> Unit = {},
    onIssueClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onLicensesClick: () -> Unit = {}
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            Box {
                Row {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector =
                                if (favorite) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            tint = if (favorite)
                                MaterialTheme.colorScheme.primary
                            else
                                LocalContentColor.current,
                            contentDescription = stringResource(id = R.string.favorite_button)
                        )
                    }
                    IconButton(onClick = onThemeClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_palette_24dp),
                            contentDescription = stringResource(id = R.string.change_theme_button)
                        )
                    }
                    IconButton(onClick = { moreMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.more_menu_button)
                        )
                    }
                }
                MoreMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false },
                    onGuidelinesClick = {
                        onGuidelinesClick()
                        moreMenuExpanded = false
                    },
                    onDocsClick = {
                        onDocsClick()
                        moreMenuExpanded = false
                    },
                    onSourceClick = {
                        onSourceClick()
                        moreMenuExpanded = false
                    },
                    onIssueClick = {
                        onIssueClick()
                        moreMenuExpanded = false
                    },
                    onTermsClick = {
                        onTermsClick()
                        moreMenuExpanded = false
                    },
                    onPrivacyClick = {
                        onPrivacyClick()
                        moreMenuExpanded = false
                    },
                    onLicensesClick = {
                        onLicensesClick()
                        moreMenuExpanded = false
                    }
                )
            }
        },
        navigationIcon = {
            if (showBackNavigationIcon) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.back_button)
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onGuidelinesClick: () -> Unit,
    onDocsClick: () -> Unit,
    onSourceClick: () -> Unit,
    onIssueClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLicensesClick: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.view_design_guidelines)) },
            onClick = onGuidelinesClick
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.view_developer_docs)) },
            onClick = onDocsClick
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.view_source_code)) },
            onClick = onSourceClick
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.report_an_issue)) },
            onClick = onIssueClick
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.terms_of_service)) },
            onClick = onTermsClick
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.privacy_policy)) },
            onClick = onPrivacyClick
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.open_source_licenses)) },
            onClick = onLicensesClick
        )
    }
}
