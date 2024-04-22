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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.ui.theme.ThemePicker
import androidx.compose.material3.catalog.library.util.GuidelinesUrl
import androidx.compose.material3.catalog.library.util.IssueUrl
import androidx.compose.material3.catalog.library.util.LicensesUrl
import androidx.compose.material3.catalog.library.util.PrivacyUrl
import androidx.compose.material3.catalog.library.util.ReleasesUrl
import androidx.compose.material3.catalog.library.util.SourceUrl
import androidx.compose.material3.catalog.library.util.TermsUrl
import androidx.compose.material3.catalog.library.util.openUrl
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScaffold(
    topBarTitle: String,
    showBackNavigationIcon: Boolean = false,
    theme: Theme,
    guidelinesUrl: String = GuidelinesUrl,
    docsUrl: String = ReleasesUrl,
    sourceUrl: String = SourceUrl,
    issueUrl: String = IssueUrl,
    termsUrl: String = TermsUrl,
    privacyUrl: String = PrivacyUrl,
    licensesUrl: String = LicensesUrl,
    onThemeChange: (theme: Theme) -> Unit,
    onBackClick: () -> Unit = {},
    favorite: Boolean,
    onFavoriteClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val sheetState = rememberModalBottomSheetState()
    var openThemePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CatalogTopAppBar(
                title = topBarTitle,
                showBackNavigationIcon = showBackNavigationIcon,
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick,
                favorite = favorite,
                onFavoriteClick = onFavoriteClick,
                onThemeClick = { openThemePicker = true },
                onGuidelinesClick = { context.openUrl(guidelinesUrl) },
                onDocsClick = { context.openUrl(docsUrl) },
                onSourceClick = { context.openUrl(sourceUrl) },
                onIssueClick = { context.openUrl(issueUrl) },
                onTermsClick = { context.openUrl(termsUrl) },
                onPrivacyClick = { context.openUrl(privacyUrl) },
                onLicensesClick = { context.openUrl(licensesUrl) }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        content = content
    )

    if (openThemePicker) {
        ModalBottomSheet(
            onDismissRequest = { openThemePicker = false },
            sheetState = sheetState,
            content = {
                ThemePicker(
                    theme = theme,
                    onThemeChange = onThemeChange,
                )
            },
        )
    }
}
