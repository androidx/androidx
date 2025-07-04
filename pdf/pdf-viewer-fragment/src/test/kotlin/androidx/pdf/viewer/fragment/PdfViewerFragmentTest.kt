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

package androidx.pdf.viewer.fragment

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.content.ExternalLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class TestPdfViewerFragment : PdfViewerFragment() {

    // Expose the protected onLinkClicked method for testing purpose
    fun testOnLinkClicked(externalLink: ExternalLink): Boolean {
        return onLinkClicked(externalLink)
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class CustomTestPdfViewerFragment : PdfViewerFragment() {

    // Expose the protected onLinkClicked method for testing purpose
    fun testOnLinkClicked(externalLink: ExternalLink): Boolean {
        return onLinkClicked(externalLink)
    }

    // Override the onLinkClicked method to provide custom behavior for testing
    override fun onLinkClicked(externalLink: ExternalLink): Boolean {
        return externalLink.uri.toString().contains("example.com")
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@RunWith(RobolectricTestRunner::class)
class PdfViewerFragmentTest {

    @Test
    fun test_setDocumentUri_withInvalidUri_throwsIllegalArgumentException() {
        val fragment = PdfViewerFragment()
        val customUriString = "customscheme://some/path/to/resource?param1=value1&param2=value2"
        val customUri: Uri = Uri.parse(customUriString)

        assertThrows(IllegalArgumentException::class.java) { fragment.documentUri = customUri }
    }

    @Test
    fun test_onLinkClicked_withDefaultBehavior() {
        val fragment = TestPdfViewerFragment()
        val externalLink = ExternalLink(Uri.parse("http://example.com"))
        val result = fragment.testOnLinkClicked(externalLink)
        assertEquals("Link should trigger default behavior", false, result)
    }

    @Test
    fun test_onLinkClicked_withCustomImplementation() {
        val fragment = CustomTestPdfViewerFragment()
        val externalLink = ExternalLink(Uri.parse("http://example.com"))
        val result = fragment.testOnLinkClicked(externalLink)
        assertEquals("Link should be handled by custom logic", true, result)
    }

    @Test
    fun test_onLinkClicked_withCustomImplementation_Fails() {
        val fragment = CustomTestPdfViewerFragment()
        val externalLink = ExternalLink(Uri.parse("http://example.org"))
        val result = fragment.testOnLinkClicked(externalLink)
        assertEquals("Link should fall back to default behavior", false, result)
    }
}
