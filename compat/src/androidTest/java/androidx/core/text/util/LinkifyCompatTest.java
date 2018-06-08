/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.text.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;

import androidx.core.util.PatternsCompat;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test {@link LinkifyCompat}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LinkifyCompatTest {
    private static final Pattern LINKIFY_TEST_PATTERN = Pattern.compile(
            "(test:)?[a-zA-Z0-9]+(\\.pattern)?");

    private MatchFilter mMatchFilterStartWithDot = new MatchFilter() {
        @Override
        public boolean acceptMatch(final CharSequence s, final int start, final int end) {
            if (start == 0) {
                return true;
            }

            if (s.charAt(start - 1) == '.') {
                return false;
            }

            return true;
        }
    };

    private TransformFilter mTransformFilterUpperChar = new TransformFilter() {
        @Override
        public String transformUrl(final Matcher match, String url) {
            StringBuilder buffer = new StringBuilder();
            String matchingRegion = match.group();

            for (int i = 0, size = matchingRegion.length(); i < size; i++) {
                char character = matchingRegion.charAt(i);

                if (character == '.' || Character.isLowerCase(character)
                        || Character.isDigit(character)) {
                    buffer.append(character);
                }
            }
            return buffer.toString();
        }
    };

    @Test
    public void testAddLinksToSpannable() {
        // Verify URLs including the ones that have new gTLDs, and the
        // ones that look like gTLDs (and so are accepted by linkify)
        // and the ones that should not be linkified due to non-compliant
        // gTLDs
        final String longGTLD =
                "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabc";
        SpannableString spannable = new SpannableString("name@gmail.com, "
                + "www.google.com, http://www.google.com/language_tools?hl=en, "
                + "a.bd, "   // a URL with accepted TLD so should be linkified
                + "d.e, f.1, g.12, "  // not valid, so should not be linkified
                + "http://h." + longGTLD + " "  // valid, should be linkified
                + "j." + longGTLD + "a"); // not a valid URL (gtld too long), no linkify

        assertTrue(LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS));
        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertEquals(4, spans.length);
        assertEquals("http://www.google.com", spans[0].getURL());
        assertEquals("http://www.google.com/language_tools?hl=en", spans[1].getURL());
        assertEquals("http://a.bd", spans[2].getURL());
        assertEquals("http://h." + longGTLD, spans[3].getURL());

        assertTrue(LinkifyCompat.addLinks(spannable, Linkify.EMAIL_ADDRESSES));
        spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("mailto:name@gmail.com", spans[0].getURL());

        try {
            LinkifyCompat.addLinks((Spannable) null, Linkify.WEB_URLS);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        assertFalse(LinkifyCompat.addLinks((Spannable) null, 0));
    }

    @Test
    public void testAddLinksToSpannableWithScheme() {
        String text = "google.pattern, test:AZ0101.pattern";

        SpannableString spannable = new SpannableString(text);
        LinkifyCompat.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:");
        URLSpan[] spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("test:google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());

        try {
            LinkifyCompat.addLinks((Spannable) null, LINKIFY_TEST_PATTERN, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        try {
            LinkifyCompat.addLinks(spannable, null, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        spannable = new SpannableString(text);
        LinkifyCompat.addLinks(spannable, LINKIFY_TEST_PATTERN, null);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());
    }

    @Test
    public void testAddLinks3() {
        String text = "FilterUpperCase.pattern, 12.345.pattern";

        SpannableString spannable = new SpannableString(text);
        LinkifyCompat.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:",
                mMatchFilterStartWithDot, mTransformFilterUpperChar);
        URLSpan[] spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());

        try {
            LinkifyCompat.addLinks((Spannable)null, LINKIFY_TEST_PATTERN, "Test:",
                    mMatchFilterStartWithDot, mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            LinkifyCompat.addLinks(spannable, null, "Test:", mMatchFilterStartWithDot,
                    mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        spannable = new SpannableString(text);
        LinkifyCompat.addLinks(spannable, LINKIFY_TEST_PATTERN, null, mMatchFilterStartWithDot,
                mTransformFilterUpperChar);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("ilterpperase.pattern", spans[0].getURL());
        assertEquals("12", spans[1].getURL());

        spannable = new SpannableString(text);
        LinkifyCompat.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:", null, mTransformFilterUpperChar);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(3, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());
        assertEquals("test:345.pattern", spans[2].getURL());

        spannable = new SpannableString(text);
        LinkifyCompat.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:", mMatchFilterStartWithDot, null);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("test:FilterUpperCase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());
    }

    @Test
    public void testAddLinksPhoneNumbers() {
        String numbersInvalid = "123456789 not a phone number";
        String numbersUKLocal = "tel:(0812)1234560 (0812)1234561";
        String numbersUSLocal = "tel:(812)1234562 (812)123.4563 "
                + " tel:(800)5551210 (800)555-1211 555-1212";
        String numbersIntl = "tel:+4408121234564 +44-0812-123-4565"
                + " tel:+18005551213 +1-800-555-1214";
        SpannableString spannable = new SpannableString(
                numbersInvalid
                        + " " + numbersUKLocal
                        + " " + numbersUSLocal
                        + " " + numbersIntl);

        // phonenumber linkify is locale-dependent
        if (Locale.US.equals(Locale.getDefault())) {
            assertTrue(LinkifyCompat.addLinks(spannable, Linkify.PHONE_NUMBERS));
            URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
            // We cannot assert the contents of the spans as support library falls back to the
            // framework libphonenumber which behaves differently for different API levels.
            assertNotEquals("There should be more than zero phone number spans.", 0, spans.length);
        }

        try {
            LinkifyCompat.addLinks((Spannable) null, Linkify.WEB_URLS);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        assertFalse(LinkifyCompat.addLinks((Spannable) null, 0));
    }

    @Test
    public void testAddLinks_spanOverlapPruning() {
        SpannableString spannable = new SpannableString("800-555-1211@gmail.com 800-555-1222.com"
                + " phone +1-800-555-1214");

        // phonenumber linkify is locale-dependent
        if (Locale.US.equals(Locale.getDefault())) {
            assertTrue(LinkifyCompat.addLinks(spannable, Linkify.ALL));
            URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
            assertEquals(3, spans.length);
            assertTrue(containsUrl(spans, "tel:+18005551214"));
            assertTrue(containsUrl(spans, "mailto:800-555-1211@gmail.com"));
            assertTrue(containsUrl(spans, "http://800-555-1222.com"));
        }
    }

    private boolean containsUrl(URLSpan[] spans, String expectedValue) {
        for (URLSpan span : spans) {
            if (span.getURL().equals(expectedValue)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testAddLinks_addsLinksWhenDefaultSchemeIsNull() {
        Spannable spannable = new SpannableString("any https://android.com any android.com any");
        LinkifyCompat.addLinks(spannable, PatternsCompat.AUTOLINK_WEB_URL, null, null, null);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertEquals("android.com and https://android.com should be linkified", 2, spans.length);
        assertEquals("https://android.com", spans[0].getURL());
        assertEquals("android.com", spans[1].getURL());
    }

    @Test
    public void testAddLinks_addsLinksWhenSchemesArrayIsNull() {
        Spannable spannable = new SpannableString("any https://android.com any android.com any");
        LinkifyCompat.addLinks(spannable, PatternsCompat.AUTOLINK_WEB_URL, "http://", null, null);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertEquals("android.com and https://android.com should be linkified", 2, spans.length);
        // expected behavior, passing null schemes array means: prepend defaultScheme to all links.
        assertEquals("http://https://android.com", spans[0].getURL());
        assertEquals("http://android.com", spans[1].getURL());
    }

    @Test
    public void testAddLinks_prependsDefaultSchemeToBeginingOfLink() {
        Spannable spannable = new SpannableString("any android.com any");
        LinkifyCompat.addLinks(spannable, PatternsCompat.AUTOLINK_WEB_URL, "http://",
                new String[] { "http://", "https://"}, null, null);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertEquals("android.com should be linkified", 1, spans.length);
        assertEquals("http://android.com", spans[0].getURL());
    }

    @Test
    public void testAddLinks_doesNotPrependSchemeIfSchemeExists() {
        Spannable spannable = new SpannableString("any https://android.com any");
        LinkifyCompat.addLinks(spannable, PatternsCompat.AUTOLINK_WEB_URL, "http://",
                new String[] { "http://", "https://"}, null, null);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertEquals("android.com should be linkified", 1, spans.length);
        assertEquals("https://android.com", spans[0].getURL());
    }

    // WEB_URLS Related Tests

    @Test
    public void testAddLinks_doesNotAddLinksForUrlWithoutProtocolAndWithoutKnownTld() {
        Spannable spannable = new SpannableString("hey man.its me");
        boolean linksAdded = LinkifyCompat.addLinks(spannable, Linkify.ALL);
        assertFalse("Should not add link with unknown TLD", linksAdded);
    }

    @Test
    public void testAddLinks_shouldNotAddEmailAddressAsUrl() {
        String url = "name@gmail.com";
        verifyAddLinksWithWebUrlFails("Should not recognize email address as URL", url);
    }

    @Test
    public void testAddLinks_acceptsUrlsWithCommasInRequestParameterValues() {
        String url = "https://android.com/path?ll=37.4221,-122.0836&z=17&pll=37.4221,-122.0836";
        verifyAddLinksWithWebUrlSucceeds("Should accept commas", url);
    }

    @Test
    public void testAddLinks_addsLinksForUrlWithProtocolWithoutTld() {
        String url = "http://android/#notld///a/n/d/r/o/i/d&p1=1&p2=2";
        verifyAddLinksWithWebUrlSucceeds("Should accept URL starting with protocol but does not"
                + " have TLD", url);
    }

    @Test
    public void testAddLinks_matchesProtocolCaseInsensitive() {
        String url = "hTtP://android.com";
        verifyAddLinksWithWebUrlSucceeds("Protocol matching should be case insensitive", url);
    }

    @Test
    public void testAddLinks_matchesValidUrlWithSchemeAndHostname() {
        String url = "http://www.android.com";
        verifyAddLinksWithWebUrlSucceeds("Should match valid URL with scheme and hostname", url);
    }

    @Test
    public void testAddLinks_matchesValidUrlWithSchemeHostnameAndNewTld() {
        String url = "http://www.android.me";
        verifyAddLinksWithWebUrlSucceeds("Should match valid URL with scheme hostname and new TLD",
                url);
    }

    @Test
    public void testAddLinks_matchesValidUrlWithHostnameAndNewTld() {
        String url = "android.camera";
        verifyAddLinksWithWebUrlSucceeds("Should match valid URL with hostname and new TLD", url);
    }

    @Test
    public void testAddLinks_matchesPunycodeUrl() {
        String url = "http://xn--fsqu00a.xn--unup4y";
        verifyAddLinksWithWebUrlSucceeds("Should match Punycode URL", url);
    }

    @Test
    public void testAddLinks_matchesPunycodeUrlWithoutProtocol() {
        String url = "xn--fsqu00a.xn--unup4y";
        verifyAddLinksWithWebUrlSucceeds("Should match Punycode URL without protocol", url);
    }

    @Test
    public void testAddLinks_doesNotMatchPunycodeTldThatStartsWithDash() {
        String url = "xn--fsqu00a.-xn--unup4y";
        verifyAddLinksWithWebUrlFails("Should not match Punycode TLD that starts with dash", url);
    }

    @Test
    public void testAddLinks_partiallyMatchesPunycodeTldThatEndsWithDash() {
        String url = "http://xn--fsqu00a.xn--unup4y-";
        verifyAddLinksWithWebUrlPartiallyMatches("Should partially match Punycode TLD that ends "
                + "with dash", "http://xn--fsqu00a.xn--unup4y", url);
    }

    @Test
    public void testAddLinks_matchesUrlWithUnicodeDomainName() {
        String url = "http://\uD604\uAE08\uC601\uC218\uC99D.kr";
        verifyAddLinksWithWebUrlSucceeds("Should match URL with Unicode domain name", url);
    }

    @Test
    public void testAddLinks_matchesUrlWithUnicodeDomainNameWithoutProtocol() {
        String url = "\uD604\uAE08\uC601\uC218\uC99D.kr";
        verifyAddLinksWithWebUrlSucceeds("Should match URL without protocol and with Unicode "
                + "domain name", url);
    }

    @Test
    public void testAddLinks_matchesUrlWithUnicodeDomainNameAndTld() {
        String url = "\uB3C4\uBA54\uC778.\uD55C\uAD6D";
        verifyAddLinksWithWebUrlSucceeds("Should match URL with Unicode domain name and TLD", url);
    }

    @Test
    public void testAddLinks_matchesUrlWithUnicodePath() {
        String url = "http://android.com/\u2019/a";
        verifyAddLinksWithWebUrlSucceeds("Should match URL with Unicode path", url);
    }

    @Test
    public void testAddLinks_matchesValidUrlWithPort() {
        String url = "http://www.example.com:8080";
        verifyAddLinksWithWebUrlSucceeds("Should match URL with port", url);
    }

    @Test
    public void testAddLinks_matchesUrlWithPortAndQuery() {
        String url = "http://www.example.com:8080/?foo=bar";
        verifyAddLinksWithWebUrlSucceeds("Should match URL with port and query", url);
    }

    @Test
    public void testAddLinks_matchesUrlWithTilde() {
        String url = "http://www.example.com:8080/~user/?foo=bar";
        verifyAddLinksWithWebUrlSucceeds("Should match URL with tilde", url);
    }

    @Test
    public void testAddLinks_matchesUrlStartingWithHttpAndDoesNotHaveTld() {
        String url = "http://android/#notld///a/n/d/r/o/i/d&p1=1&p2=2";
        verifyAddLinksWithWebUrlSucceeds("Should match URL without a TLD and starting with http",
                url);
    }

    @Test
    public void testAddLinks_doesNotMatchUrlsWithoutProtocolAndWithUnknownTld() {
        String url = "thank.you";
        verifyAddLinksWithWebUrlFails("Should not match URL that does not start with a protocol "
                + "and does not contain a known TLD", url);
    }

    @Test
    public void testAddLinks_matchesValidUrlWithEmoji() {
        String url = "Thank\u263A.com";
        verifyAddLinksWithWebUrlSucceeds("Should match URL with emoji", url);
    }

    @Test
    public void testAddLinks_doesNotMatchUrlsWithEmojiWithoutProtocolAndWithoutKnownTld() {
        String url = "Thank\u263A.you";
        verifyAddLinksWithWebUrlFails("Should not match URLs containing emoji and with unknown "
                + "TLD", url);
    }

    @Test
    public void testAddLinks_matchesDomainNameWithSurrogatePairs() {
        String url = "android\uD83C\uDF38.com";
        verifyAddLinksWithWebUrlSucceeds("Should match domain name with Unicode surrogate pairs",
                url);
    }

    @Test
    public void testAddLinks_matchesTldWithSurrogatePairs() {
        String url = "http://android.\uD83C\uDF38com";
        verifyAddLinksWithWebUrlSucceeds("Should match TLD with Unicode surrogate pairs", url);
    }

    @Test
    public void testAddLinks_doesNotMatchUrlWithExcludedSurrogate() {
        String url = "android\uD83F\uDFFE.com";
        verifyAddLinksWithWebUrlFails("Should not match URL with excluded Unicode surrogate"
                + " pair",  url);
    }

    @Test
    public void testAddLinks_matchesPathWithSurrogatePairs() {
        String url = "http://android.com/path-with-\uD83C\uDF38?v=\uD83C\uDF38f";
        verifyAddLinksWithWebUrlSucceeds("Should match path and query with Unicode surrogate pairs",
                url);
    }

    @Test
    public void testAddLinks__doesNotMatchUnicodeSpaces() {
        String part1 = "http://and";
        String part2 = "roid.com";
        String[] emptySpaces = new String[]{
                "\u00A0", // no-break space
                "\u2000", // en quad
                "\u2001", // em quad
                "\u2002", // en space
                "\u2003", // em space
                "\u2004", // three-per-em space
                "\u2005", // four-per-em space
                "\u2006", // six-per-em space
                "\u2007", // figure space
                "\u2008", // punctuation space
                "\u2009", // thin space
                "\u200A", // hair space
                "\u2028", // line separator
                "\u2029", // paragraph separator
                "\u202F", // narrow no-break space
                "\u3000"  // ideographic space
        };

        for (String emptySpace : emptySpaces) {
            String url = part1 + emptySpace + part2;
            verifyAddLinksWithWebUrlPartiallyMatches("Should not include empty space with code: "
                    + emptySpace.codePointAt(0), part1, url);
        }
    }

    @Test
    public void testAddLinks_matchesDomainNameWithDash() {
        String url = "http://a-nd.r-oid.com";
        verifyAddLinksWithWebUrlSucceeds("Should match domain name with '-'", url);

        url = "a-nd.r-oid.com";
        verifyAddLinksWithWebUrlSucceeds("Should match domain name with '-'", url);
    }

    @Test
    public void testAddLinks_matchesDomainNameWithUnderscore() {
        String url = "http://a_nd.r_oid.com";
        verifyAddLinksWithWebUrlSucceeds("Should match domain name with '_'", url);

        url = "a_nd.r_oid.com";
        verifyAddLinksWithWebUrlSucceeds("Should match domain name with '_'", url);
    }

    @Test
    public void testAddLinks_matchesPathAndQueryWithDollarSign() {
        String url = "http://android.com/path$?v=$val";
        verifyAddLinksWithWebUrlSucceeds("Should match path and query with '$'", url);

        url = "android.com/path$?v=$val";
        verifyAddLinksWithWebUrlSucceeds("Should match path and query with '$'", url);
    }

    @Test
    public void testAddLinks_matchesEmptyPathWithQueryParams() {
        String url = "http://android.com?q=v";
        verifyAddLinksWithWebUrlSucceeds("Should match empty path with query params", url);

        url = "android.com?q=v";
        verifyAddLinksWithWebUrlSucceeds("Should match empty path with query params", url);

        url = "http://android.com/?q=v";
        verifyAddLinksWithWebUrlSucceeds("Should match empty path with query params", url);

        url = "android.com/?q=v";
        verifyAddLinksWithWebUrlSucceeds("Should match empty path with query params", url);
    }

    // EMAIL_ADDRESSES Related Tests

    @Test
    public void testAddLinks_email_matchesShortValidEmail() {
        String email = "a@a.co";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);

        email = "ab@a.co";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesRegularEmail() {
        String email = "email@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesEmailWithMultipleSubdomains() {
        String email = "email@e.somelongdomainnameforandroid.abc.uk";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartWithDot() {
        String email = "e.mail@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartWithPlus() {
        String email = "e+mail@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartWithUnderscore() {
        String email = "e_mail@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartWithDash() {
        String email = "e-mail@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartWithApostrophe() {
        String email = "e'mail@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartWithDigits() {
        String email = "123@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesUnicodeLocalPart() {
        String email = "\uD604\uAE08\uC601\uC218\uC99D@android.kr";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartWithEmoji() {
        String email = "smiley\u263A@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartWithSurrogatePairs() {
        String email = "a\uD83C\uDF38a@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesDomainWithDash() {
        String email = "email@an-droid.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesUnicodeDomain() {
        String email = "email@\uD604\uAE08\uC601\uC218\uC99D.kr";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesUnicodeLocalPartAndDomain() {
        String email = "\uD604\uAE08\uC601\uC218\uC99D@\uD604\uAE08\uC601\uC218\uC99D.kr";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesDomainWithEmoji() {
        String email = "smiley@\u263Aandroid.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesDomainWithSurrogatePairs() {
        String email = "email@\uD83C\uDF38android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartAndDomainWithSurrogatePairs() {
        String email = "a\uD83C\uDF38a@\uD83C\uDF38android.com";
        verifyAddLinksWithEmailSucceeds("Should match email: " + email, email);
    }

    @Test
    public void testAddLinks_partiallyMatchesEmailEndingWithDot() {
        String email = "email@android.co.uk.";
        verifyAddLinksWithEmailPartiallyMatches("Should partially match email ending with dot",
                "mailto:email@android.co.uk", email);
    }

    @Test
    public void testAddLinks_email_partiallyMatchesLocalPartStartingWithDot() {
        String email = ".email@android.com";
        verifyAddLinksWithEmailPartiallyMatches("Should partially match email starting "
                + "with dot", "mailto:email@android.com", email);
    }

    @Test
    public void testAddLinks_email_doesNotMatchStringWithoutAtSign() {
        String email = "android.com";
        verifyAddLinksWithEmailFails("Should not match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_doesNotMatchPlainString() {
        String email = "email";
        verifyAddLinksWithEmailFails("Should not match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_doesNotMatchEmailWithoutTld() {
        String email = "email@android";
        verifyAddLinksWithEmailFails("Should not match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_doesNotMatchLocalPartEndingWithDot() {
        String email = "email.@android.com";
        verifyAddLinksWithEmailFails("Should not match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_doesNotMatchDomainStartingWithDash() {
        String email = "email@-android.com";
        verifyAddLinksWithEmailFails("Should not match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_doesNotMatchDomainWithConsecutiveDots() {
        String email = "email@android..com";
        verifyAddLinksWithEmailFails("Should not match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_doesNotMatchEmailWithIp() {
        String email = "email@127.0.0.1";
        verifyAddLinksWithEmailFails("Should not match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_doesNotMatchEmailWithInvalidTld() {
        String email = "email@android.c";
        verifyAddLinksWithEmailFails("Should not match email: " + email, email);
    }

    @Test
    public void testAddLinks_email_matchesLocalPartUpTo64Chars() {
        String localPart = "";
        for (int i = 0; i < 64; i++) {
            localPart += "a";
        }
        String email = localPart + "@android.com";
        verifyAddLinksWithEmailSucceeds("Should match email local part of length: "
                + localPart.length(), email);

        email = localPart + "a@android.com";
        verifyAddLinksWithEmailFails("Should not match email local part of length:"
                + localPart.length(), email);
    }

    @Test
    public void testAddLinks_email_matchesSubdomainUpTo63Chars() {
        String subdomain = "";
        for (int i = 0; i < 63; i++) {
            subdomain += "a";
        }
        String email = "email@" + subdomain + ".com";

        verifyAddLinksWithEmailSucceeds("Should match email subdomain of length: "
                + subdomain.length(), email);

        subdomain += "a";
        email = "email@" + subdomain + ".com";

        verifyAddLinksWithEmailFails("Should not match email subdomain of length:"
                + subdomain.length(), email);
    }

    @Test
    public void testAddLinks_email_matchesDomainUpTo255Chars() {
        String domain = "";
        while (domain.length() <= 250) {
            domain += "d.";
        }
        domain += "com";
        assertEquals(255, domain.length());
        String email = "a@" + domain;
        verifyAddLinksWithEmailSucceeds("Should match email domain of length: "
                + domain.length(), email);

        email = email + "m";
        verifyAddLinksWithEmailFails("Should not match email domain of length:"
                + domain.length(), email);
    }

    // ADDRESS RELATED TESTS

    @Test
    public void testFindAddress_withZipcode() {
        final String address = "455 LARKSPUR DRIVE CALIFORNIA SPRINGS CALIFORNIA 92826";
        verifyAddLinksWithMapAddressSucceeds("Should match map address: " + address, address);
    }

    @Test
    public void testFindAddress_invalidAddress() {
        final String address = "This is not an address: no town, no state, no zip.";
        verifyAddLinksWithMapAddressFails("Should not match map address: " + address, address);
    }

    // Utility functions
    private static void verifyAddLinksWithWebUrlSucceeds(String msg, String url) {
        verifyAddLinksSucceeds(msg, url, Linkify.WEB_URLS);
    }

    private static void verifyAddLinksWithWebUrlFails(String msg, String url) {
        verifyAddLinksFails(msg, url, Linkify.WEB_URLS);
    }

    private static void verifyAddLinksWithWebUrlPartiallyMatches(String msg, String expected,
            String url) {
        verifyAddLinksPartiallyMatches(msg, expected, url, Linkify.WEB_URLS);
    }

    private static void verifyAddLinksWithEmailSucceeds(String msg, String url) {
        verifyAddLinksSucceeds(msg, url, Linkify.EMAIL_ADDRESSES);
    }

    private static void verifyAddLinksWithEmailFails(String msg, String url) {
        verifyAddLinksFails(msg, url, Linkify.EMAIL_ADDRESSES);
    }

    private static void verifyAddLinksWithEmailPartiallyMatches(String msg, String expected,
            String url) {
        verifyAddLinksPartiallyMatches(msg, expected, url, Linkify.EMAIL_ADDRESSES);
    }

    private static void verifyAddLinksWithMapAddressSucceeds(String msg, String url) {
        verifyAddLinksSucceeds(msg, url, Linkify.MAP_ADDRESSES);
    }

    private static void verifyAddLinksWithMapAddressFails(String msg, String url) {
        verifyAddLinksFails(msg, url, Linkify.MAP_ADDRESSES);
    }

    private static void verifyAddLinksSucceeds(String msg, String string, int type) {
        String str = "start " + string + " end";
        Spannable spannable = new SpannableString(str);

        boolean linksAdded = LinkifyCompat.addLinks(spannable, type);
        URLSpan[] spans = spannable.getSpans(0, str.length(), URLSpan.class);

        assertTrue(msg, linksAdded);
        assertEquals("Span should start from the beginning of: " + string,
                "start ".length(), spannable.getSpanStart(spans[0]));
        assertEquals("Span should end at the end of: " + string,
                str.length() - " end".length(), spannable.getSpanEnd(spans[0]));
    }

    private static void verifyAddLinksFails(String msg, String string, int type) {
        Spannable spannable = new SpannableString("start " + string + " end");
        boolean linksAdded = LinkifyCompat.addLinks(spannable, type);
        assertFalse(msg, linksAdded);
    }

    private static void verifyAddLinksPartiallyMatches(String msg, String expected,
            String string, int type) {
        Spannable spannable = new SpannableString("start " + string + " end");
        boolean linksAdded = LinkifyCompat.addLinks(spannable, type);
        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertTrue(msg, linksAdded);
        assertEquals(msg, expected, spans[0].getURL().toString());
    }
}
