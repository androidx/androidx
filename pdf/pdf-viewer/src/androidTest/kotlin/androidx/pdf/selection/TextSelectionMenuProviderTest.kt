/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.pdf.selection

import android.os.Build
import androidx.pdf.selection.model.TextSelection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class TextSelectionMenuProviderTest {
    internal lateinit var textSelectionMenuProvider: TextSelectionMenuProvider

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        textSelectionMenuProvider = TextSelectionMenuProvider(context)
    }

    @After fun tearDown() {}

    @Test
    fun getMenuItems_withEmail_returnsEmailMenu() = runTest {
        val emailText = "androidpdf@gmail.com"
        val textSelection = TextSelection(emailText, emptyList())
        val menuItems = textSelectionMenuProvider.getMenuItems(textSelection)
        assertThat(menuItems).isNotNull()
        assertThat(menuItems.size).isGreaterThan(2) // Email, Copy, Select All.
        val smartMenuItem = menuItems[0] as SmartSelectionMenuComponent
        assertThat(smartMenuItem).isNotNull()
        assertThat(smartMenuItem.label).isAnyOf("Email", "Add")
    }

    @Test
    fun getMenuItems_withPhoneNumber_returnsCallMenu() = runTest {
        val phoneNumber = "8044566807"
        val textSelection = TextSelection(phoneNumber, emptyList())
        val menuItems = textSelectionMenuProvider.getMenuItems(textSelection)
        assertThat(menuItems).isNotNull()
        assertThat(menuItems.size).isGreaterThan(2) // Call, Add, Message, Copy, Select All.
        val smartMenuItem = menuItems[0] as SmartSelectionMenuComponent
        assertThat(smartMenuItem).isNotNull()
        assertThat(smartMenuItem.label).isEqualTo("Call")
    }

    @Test
    fun getMenuItems_withURL_returnsOpenMenu() = runTest {
        val url = "https://www.google.com"
        val textSelection = TextSelection(url, emptyList())
        val menuItems = textSelectionMenuProvider.getMenuItems(textSelection)
        assertThat(menuItems).isNotNull()
        assertThat(menuItems.size).isGreaterThan(2) // Open, Copy, Select All.
        val smartMenuItem = menuItems[0] as SmartSelectionMenuComponent
        assertThat(smartMenuItem).isNotNull()
        assertThat(smartMenuItem.label).isEqualTo("Open")
    }

    @Test
    fun getMenuItems_withLongText_returnsDefaultMenu() = runTest {
        val longText = "A".repeat(501)
        val textSelection = TextSelection(longText, emptyList())
        val menuItems = textSelectionMenuProvider.getMenuItems(textSelection)
        assertThat(menuItems).isNotNull()
        assertThat(menuItems).hasSize(2) // Only Copy and Select All.
        val defaultMenuItem = menuItems[0] as DefaultSelectionMenuComponent
        assertThat(defaultMenuItem).isNotNull()
        assertThat(defaultMenuItem.label).isEqualTo("Copy")
        val defaultMenuItem1 = menuItems[1] as DefaultSelectionMenuComponent
        assertThat(defaultMenuItem1).isNotNull()
        assertThat(defaultMenuItem1.label).isEqualTo("Select all")
    }

    @Test
    fun getMenuItems_returnsAtleastDefaultMenu() = runTest {
        val emailText = "abcd"
        val textSelection = TextSelection(emailText, emptyList())
        val menuItems = textSelectionMenuProvider.getMenuItems(textSelection)
        assertThat(menuItems).isNotNull()
        val size = menuItems.size
        assertThat(size).isAtLeast(2) // Copy and Select All are must.
        val defaultMenuItem = menuItems[size - 2] as DefaultSelectionMenuComponent
        assertThat(defaultMenuItem).isNotNull()
        assertThat(defaultMenuItem.label).isEqualTo("Copy")
        val defaultMenuItem1 = menuItems[size - 1] as DefaultSelectionMenuComponent
        assertThat(defaultMenuItem1).isNotNull()
        assertThat(defaultMenuItem1.label).isEqualTo("Select all")
    }
}
