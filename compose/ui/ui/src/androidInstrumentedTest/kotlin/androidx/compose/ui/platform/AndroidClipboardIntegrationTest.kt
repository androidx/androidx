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

package androidx.compose.ui.platform

import android.content.ClipData
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import junit.framework.TestCase.assertEquals
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class AndroidClipboardIntegrationTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun setText_affects_getClipEntry_and_vice_versa() = runTest {
        val clipboard: Clipboard = AndroidClipboard(rule.activity)

        clipboard.setClipEntry(null)
        assertFalse(clipboard.getClipEntry().hasText())
        assertEquals(null, clipboard.getClipEntry())

        clipboard.setClipEntry(testClipEntry("test"))
        assertTrue(clipboard.getClipEntry().hasText())
        assertEquals(1, clipboard.getClipEntry()?.clipData?.itemCount)
        assertEquals("test", clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text)

        clipboard.setClipEntry(null)
        assertFalse(clipboard.getClipEntry().hasText())
        assertEquals(null, clipboard.getClipEntry())

        clipboard.nativeClipboard.setPrimaryClip(testClipEntry("test2").clipData)
        assertTrue(clipboard.getClipEntry().hasText())
        assertEquals("test2", clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text)

        // nativeClipboard should be correct too
        assertEquals(
            rule.activity.getSystemService(Context.CLIPBOARD_SERVICE),
            clipboard.nativeClipboard,
        )
    }
}

internal fun testClipEntry(text: String): ClipEntry {
    val clipEntry =
        ClipData.newPlainText("plain text", AnnotatedString(text).convertToCharSequence())
            .toClipEntry()
    return clipEntry
}

internal fun ClipEntry?.hasText(): Boolean {
    if (this == null) return false
    return this.clipData.description.hasMimeType("text/*")
}
