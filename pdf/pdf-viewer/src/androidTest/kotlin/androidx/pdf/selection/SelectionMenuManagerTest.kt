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

package androidx.pdf.selection

import android.net.Uri
import android.os.Build
import androidx.pdf.selection.model.GoToLinkSelection
import androidx.pdf.selection.model.HyperLinkSelection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class SelectionMenuManagerTest {
    internal lateinit var selectionMenuManager: SelectionMenuManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        selectionMenuManager = SelectionMenuManager(context)
    }

    @Test
    fun getSelectionMenuItems_withHyperLinkSelection_returnsOpenMenu() = runTest {
        val url = "https://www.google.com"
        val link: Uri = Uri.parse(url)
        val hyperLinkSelection = HyperLinkSelection(link, "Google", emptyList())
        val menuItems = selectionMenuManager.getSelectionMenuItems(hyperLinkSelection)
        assertThat(menuItems).isNotNull()
        assertThat(menuItems.size).isGreaterThan(3) // Open, Copy link, Copy, Select all.
        val smartMenuItem = menuItems[0] as SmartSelectionMenuComponent
        assertThat(smartMenuItem).isNotNull()
        assertThat(smartMenuItem.label).isEqualTo("Open")
        val copyUrlMenuItem = menuItems[1] as DefaultSelectionMenuComponent
        assertThat(copyUrlMenuItem).isNotNull()
        assertThat(copyUrlMenuItem.label).isEqualTo("Copy link")
        val copyMenuItem = menuItems[2] as DefaultSelectionMenuComponent
        assertThat(copyMenuItem).isNotNull()
        assertThat(copyMenuItem.label).isEqualTo("Copy")
        val selectAllMenuItem = menuItems[3] as DefaultSelectionMenuComponent
        assertThat(selectAllMenuItem).isNotNull()
        assertThat(selectAllMenuItem.label).isEqualTo("Select all")
    }

    @Test
    fun getSelectionMenuItems_withGoToLinkSelection_returnsJumptoMenu() = runTest {
        val destination = GoToLinkSelection.Destination(1, 0f, 0f, 1.0f)
        val goToLinkSelection = GoToLinkSelection(destination, "Page 2", emptyList())
        val menuItems = selectionMenuManager.getSelectionMenuItems(goToLinkSelection)
        assertThat(menuItems).isNotNull()
        assertThat(menuItems.size).isEqualTo(3) // Jump to, Copy, Select All.
        val goToMenuItem = menuItems[0] as DefaultSelectionMenuComponent
        assertThat(goToMenuItem).isNotNull()
        assertThat(goToMenuItem.label).isEqualTo("Jump to")
        val copyMenuItem = menuItems[1] as DefaultSelectionMenuComponent
        assertThat(copyMenuItem).isNotNull()
        assertThat(copyMenuItem.label).isEqualTo("Copy")
        val selectAllMenuItem = menuItems[2] as DefaultSelectionMenuComponent
        assertThat(selectAllMenuItem).isNotNull()
        assertThat(selectAllMenuItem.label).isEqualTo("Select all")
    }

    @Test
    fun getMenuItems_withEmailLink_doesNotReturnCopyLinkMenu() = runTest {
        val emailLink = Uri.parse("https://mailto:android-pdf@google.com")
        val email = "android-pdf@google.com"
        val emailLinkSelection = HyperLinkSelection(emailLink, email, emptyList())
        val menuItems = selectionMenuManager.getSelectionMenuItems(emailLinkSelection)
        assertThat(menuItems).isNotNull()
        assertThat(menuItems.size).isGreaterThan(2) // Email, Copy, Select All.
        val lastThirdMenuItem = menuItems[menuItems.size - 3] as SmartSelectionMenuComponent
        assertThat(lastThirdMenuItem).isNotNull()
        assertThat(lastThirdMenuItem.label).isNotEqualTo("Copy link")
    }

    @Test
    fun filterLink_withEmailLink_returnsJustEmail() = runTest {
        val emailLinkString = "https://mailto:android-pdf@google.com"
        val emailString = "android-pdf@google.com"
        val filteredLinkString = HyperLinkSelectionMenuProvider.filterLink(emailLinkString)
        assertThat(filteredLinkString).isNotNull()
        assertThat(filteredLinkString).isEqualTo(emailString)
    }

    @Test
    fun filterLink_withNonEmailLink_returnsLinkUnchanged() = runTest {
        val linkString = "https://www.google.com"
        val filteredLinkString = HyperLinkSelectionMenuProvider.filterLink(linkString)
        assertThat(filteredLinkString).isNotNull()
        assertThat(filteredLinkString).isEqualTo(linkString)
    }
}
