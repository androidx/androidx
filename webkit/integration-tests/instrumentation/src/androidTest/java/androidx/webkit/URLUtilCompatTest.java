/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.webkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

/**
 * Tests for {@link URLUtilCompat}.
 *
 * This test suite does not use "image/jpeg" as a mime type, as this can result in 2
 * different extensions depending on the OS platform, which leads to flakes.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class URLUtilCompatTest {

    @Test
    public void guessFileName_ctsTest() {
        // This test mirrors the test in
        // cts/tests/tests/webkit/src/android/webkit/cts/URLUtilTest.java for completeness.
        String url = "ftp://example.url/test";
        assertEquals("test.gif", URLUtilCompat.guessFileName(url, null, "image/gif"));
        assertEquals("test.bin",
                URLUtilCompat.guessFileName(url, null, "application/octet-stream"));
    }

    @Test
    public void guessFileName_extractPathSegmentFromUrl() {
        String url = "https://example.com/resources/image?size=large";
        assertEquals("image.gif", URLUtilCompat.guessFileName(url, null, "image/gif"));
    }

    @Test
    public void guessFileName_existingExtesionsNotReplacedIfMatchingMimeType() {
        String url = "https://example.com/resources/image.gif?size=large";
        assertEquals("image.gif", URLUtilCompat.guessFileName(url, null, "image/gif"));
    }

    @Test
    public void guessFileName_extensionAppendedIfNotMatchingMimetype() {
        String url = "https://example.com/resources/image.png?size=large";
        assertEquals("image.png.gif", URLUtilCompat.guessFileName(url, null, "image/gif"));
    }

    @Test
    public void guessFileName_htmlExtensionForTextHtml() {
        String url = "https://example.com/index";
        // On some versions of Android, the mime type is mapped to the .htm extension, while on
        // other it is mapped to the ".html" extension.
        // The function is correct as long as it returns one or the other.
        assertTrue(Arrays.asList("index.html", "index.htm").contains(
                URLUtilCompat.guessFileName(url, null, "text/html")));
    }

    @Test
    public void guessFileName_txtExtensionForUnknownTxtType() {
        String url = "https://example.com/index";
        assertEquals("index.txt", URLUtilCompat.guessFileName(url, null, "text/fantasy"));
    }

    @Test
    public void guessFileName_fallbackIfNoFilenamesAvailable() {
        String url = "https://example.com/";
        assertEquals("downloadfile.bin", URLUtilCompat.guessFileName(url, null, null));
    }

    @Test
    public void guessFileName_contentDispositionUsedIfAvailable() {
        String url = "https://example.com/wrong";
        String contentDisposition = "attachment; filename=Test.png";
        assertEquals("Test.png", URLUtilCompat.guessFileName(url, contentDisposition, "image/png"));
    }

    @Test
    public void guessFileName_contentDispositionNameGetsAppendedMimeType() {
        String url = "https://example.com/wrong";
        String contentDisposition = "attachment; filename=Test.png";
        assertEquals("Test.png.gif",
                URLUtilCompat.guessFileName(url, contentDisposition, "image/gif"));
    }

    @Test
    public void guessFileName_contentDispositionNameReplacesSlashWithUnderscore() {
        String url = "https://example.com/wrong";
        String contentDisposition = "attachment; filename=Test/Test.png";
        assertEquals("Test_Test.png", URLUtilCompat.guessFileName(url, contentDisposition, null));
    }

    @Test
    public void guessFileName_contentDispositionNameReplacesSlashWithUnderscore_WithEscape() {
        String url = "https://example.com/wrong";
        String contentDisposition = "attachment; filename=\"Test\\/Test.png\"";
        assertEquals("Test_Test.png", URLUtilCompat.guessFileName(url, contentDisposition, null));
    }

    @Test
    public void guessFileName_contentDispositionUsesExtNameIfAvailable() {
        String url = "https://example.com/wrong";
        String contentDisposition = "attachment; filename=\"Wrong.png\"; filename*=utf-8''Test.png";
        assertEquals("Test.png", URLUtilCompat.guessFileName(url, contentDisposition, null));
    }


    @Test
    public void parseContentDisposition_nullIfDispositionIsInline() {
        assertNull(URLUtilCompat.getFilenameFromContentDisposition("inline; filename=Test.png"));
    }


    @Test
    public void parseContentDisposition_ignoreUnknownDisposition() {
        assertEquals("Test.png",
                URLUtilCompat.getFilenameFromContentDisposition("unknowndisposition; filename=Test"
                        + ".png"));
    }

    @Test
    public void parseContentDisposition_caseInsensitiveAttrName() {
        assertEquals("Test.png",
                URLUtilCompat.getFilenameFromContentDisposition("attachment; fIlEnAmE=Test.png"));
    }

    @Test
    public void parseContentDisposition_unQuotedString() {
        assertEquals("Test.png",
                URLUtilCompat.getFilenameFromContentDisposition("attachment; filename=Test.png"));
    }


    @Test
    public void parseContentDisposition_doubleQuotedString() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Test.png\""));
    }

    @Test
    public void parseContentDisposition_doubleQuotedString_extraSpaces() {
        assertEquals("Te st.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment ; filename = \"Te st.png\""));
    }

    @Test
    public void parseContentDisposition_singleQuotedString_extraSpaces() {
        assertEquals("Te st.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment ; filename = 'Te st.png'"));
    }

    @Test
    public void parseContentDisposition_unQuotedString_extraSpaces() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment ; filename = Test.png  ;"));
    }

    @Test
    public void parseContentDisposition_doubleQuotedStringUppercaseAttachment() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "Attachment; filename=\"Test.png\""));
    }

    @Test
    public void parseContentDisposition_singleQuotedString() {
        assertEquals("Test.png",
                URLUtilCompat.getFilenameFromContentDisposition("attachment; filename='Test.png'"));
    }

    @Test
    public void parseContentDisposition_quotedStringExtraSemicolon() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Test.png\";"));
    }

    @Test
    public void parseContentDisposition_onlyExtUtf8NoSpecialChars() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename*=UTF-8''Test.png"));
    }

    @Test
    public void parseContentDisposition_bothParametersExtFilenameFirst_expectExtFilename() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename*=UTF-8''Test.png; filename=\"Wrong.png\""));
    }

    @Test
    public void parseContentDisposition_bothParametersStandardFirst_expectExtFilename() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Wrong.png\"; filename*=utf-8''Test.png"));
    }

    @Test
    public void parseContentDisposition_encodedWithLanguageParameter() {
        assertEquals("£ rates", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename*=iso-8859-1'en'%A3%20rates; filename=\"Wrong.png\""));
    }

    @Test
    public void parseContentDisposition_encodedWithUppercaseEncoding() {
        assertEquals("£ and € rates", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Wrong.png\" ; "
                        + "filename*=UTF-8''%c2%a3%20and%20%e2%82%ac%20rates"));
    }

    @Test
    public void parseContentDisposition_badEncodingIsIgnoredInFavorOfRegular() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Test.png\"; " + "filename*=UTF-8''%borked"));
    }


    @Test
    public void parseContentDisposition_unknownEncodingIsIgnoredInFavorOfRegular() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Test.png\"; " + "filename*=UTF-9''Wrong.png"));
    }

    @Test
    public void parseContentDisposition_semicolonInQuotedString_singleQuoted() {
        assertEquals("Test;.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename='Test;.png'"));
    }
    @Test
    public void parseContentDisposition_semicolonInQuotedString_doubleQuoted() {
        assertEquals("Test;.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Test;.png\""));
    }
    @Test
    public void parseContentDisposition_semicolonInQuotedString_singeQuotedPlusExt() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename='Wrong;.png'; filename*=utf-8''Test.png"));
    }
    @Test
    public void parseContentDisposition_semicolonInQuotedString_doubleQuotedPlusExt() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename='Wrong;.png'; filename*=utf-8''Test.png"));
    }

    @Test
    public void parseContentDisposition_equalsInQuotedString_singleQuoted() {
        assertEquals("Test=.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename='Test=.png'"));
    }

    @Test
    public void parseContentDisposition_equalsInQuotedString_doubleQuoted() {
        assertEquals("Test=.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Test=.png\""));
    }

    @Test
    public void parseContentDisposition_equalsInQuotedString_singleQuotedPlusExt() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename='Wrong=.png'; filename*=utf-8''Test.png"));
    }

    @Test
    public void parseContentDisposition_equalsInQuotedString_doubleQuotedPlusExt() {
        assertEquals("Test.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Wrong=.png\"; filename*=utf-8''Test.png"));
    }

    @Test
    public void parseContentDisposition_escapedQuote_singleQuoted() {
        assertEquals("Test'.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename='Test\\'.png'"));
    }

    @Test
    public void parseContentDisposition_escapedQuote_doubleQuoted() {
        assertEquals("Test\".png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Test\\\".png\""));
    }


    @Test
    public void parseContentDisposition_escapedTokens_singleQuoted() {
        assertEquals("Test\\*.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename='Test\\\\\\*.png'"));
    }
    @Test
    public void parseContentDisposition_escapedTokens_doubleQuoted() {
        assertEquals("Test\\*.png", URLUtilCompat.getFilenameFromContentDisposition(
                "attachment; filename=\"Test\\\\\\*.png\""));
    }
}
