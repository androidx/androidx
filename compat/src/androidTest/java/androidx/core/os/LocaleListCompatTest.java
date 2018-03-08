/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.os;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

/**
 * Tests for {@link LocaleListCompat}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocaleListCompatTest {

    @Test
    public void testEmptyLocaleList() {
        LocaleListCompat ll = LocaleListCompat.create();
        assertNotNull(ll);
        assertTrue(ll.isEmpty());
        assertEquals(0, ll.size());
        assertNull(ll.get(0));
        assertNull(ll.get(1));
        assertNull(ll.get(10));

        ll = LocaleListCompat.create(new Locale[0]);
        assertNotNull(ll);
        assertTrue(ll.isEmpty());
        assertEquals(0, ll.size());
        assertNull(ll.get(0));
        assertNull(ll.get(1));
        assertNull(ll.get(10));
    }

    @Test
    public void testOneMemberLocaleList() {
        final LocaleListCompat ll = LocaleListCompat.create(Locale.US);
        assertNotNull(ll);
        assertFalse(ll.isEmpty());
        assertEquals(1, ll.size());
        assertEquals(Locale.US, ll.get(0));
        assertNull(ll.get(10));
    }

    @Test
    public void testTwoMemberLocaleList() {
        final Locale enPH = forLanguageTag("en-PH");
        final Locale[] la = {enPH, Locale.US};
        final LocaleListCompat ll = LocaleListCompat.create(la);
        assertNotNull(ll);
        assertFalse(ll.isEmpty());
        assertEquals(2, ll.size());
        assertEquals(enPH, ll.get(0));
        assertEquals(Locale.US, ll.get(1));
        assertNull(ll.get(10));
    }

    @Test
    public void testNullArgument() {
        try {
            LocaleListCompat.create((Locale) null);
            fail("Initializing a LocaleListCompat with a null argument should throw.");
        } catch (Throwable e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
        try {
            LocaleListCompat.create((Locale[]) null);
            fail("Initializing a LocaleListCompat with a null array should throw.");
        } catch (Throwable e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
    }

    @Test
    public void testNullArguments() {
        final Locale[] la = {Locale.US, null};
        try {
            LocaleListCompat.create(la);
            fail("Initializing a LocaleListCompat with an array containing null should throw.");
        } catch (Throwable e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
    }

    @Test
    public void testRepeatedArguments() {
        final Locale[] la = {Locale.US, Locale.US};
        try {
            LocaleListCompat.create(la);
            fail("Initializing a LocaleListCompat with an array containing duplicates should "
                    + "throw.");
        } catch (Throwable e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    @Test
    public void testIndexOf() {
        final LocaleListCompat empty = LocaleListCompat.create();
        assertEquals(-1, empty.indexOf(Locale.US));

        final LocaleListCompat oneMember = LocaleListCompat.create(Locale.US);
        assertEquals(0, oneMember.indexOf(Locale.US));
        assertEquals(-1, oneMember.indexOf(Locale.ENGLISH));

        final LocaleListCompat twoMember = LocaleListCompat.forLanguageTags("en,fr");
        assertEquals(0, twoMember.indexOf(forLanguageTag("en")));
        assertEquals(1, twoMember.indexOf(forLanguageTag("fr")));
        assertEquals(-1, twoMember.indexOf(forLanguageTag("en-US")));
    }

    @Test
    public void testToString() {
        LocaleListCompat ll = LocaleListCompat.create();
        assertEquals("[]", ll.toString());

        final Locale[] la1 = {Locale.US};
        ll = LocaleListCompat.create(la1);
        assertEquals("[" + Locale.US.toString() + "]", ll.toString());

        final Locale[] la2 = {Locale.US, Locale.FRENCH};
        ll = LocaleListCompat.create(la2);
        assertEquals("[" + Locale.US.toString() + "," + Locale.FRENCH.toString() + "]",
                ll.toString());
    }

    @Test
    public void testToLanguageTags() {
        LocaleListCompat ll = LocaleListCompat.create();
        assertEquals("", ll.toLanguageTags());

        final Locale[] la1 = {Locale.US};
        ll = LocaleListCompat.create(la1);
        assertEquals(toLanguageTag(Locale.US), ll.toLanguageTags());

        final Locale[] la2 = {Locale.US, Locale.FRENCH};
        ll = LocaleListCompat.create(la2);
        assertEquals(toLanguageTag(Locale.US) + "," + toLanguageTag(Locale.FRENCH),
                ll.toLanguageTags());
    }

    @Test
    public void testGetEmptyLocaleList() {
        LocaleListCompat empty = LocaleListCompat.getEmptyLocaleList();
        LocaleListCompat anotherEmpty = LocaleListCompat.getEmptyLocaleList();
        LocaleListCompat constructedEmpty = LocaleListCompat.create();

        assertEquals(constructedEmpty, empty);
        assertSame(empty, anotherEmpty);
    }

    @Test
    public void testForLanguageTags() {
        assertEquals(LocaleListCompat.getEmptyLocaleList(), LocaleListCompat.forLanguageTags(null));
        assertEquals(LocaleListCompat.getEmptyLocaleList(), LocaleListCompat.forLanguageTags(""));

        assertEquals(LocaleListCompat.create(forLanguageTag("en-US")),
                LocaleListCompat.forLanguageTags("en-US"));

        final Locale[] la = {forLanguageTag("en-PH"), forLanguageTag("en-US")};
        assertEquals(LocaleListCompat.create(la),
                LocaleListCompat.forLanguageTags("en-PH,en-US"));
    }

    @Test
    public void testGetDefault() {
        final LocaleListCompat ll = LocaleListCompat.getDefault();
        assertNotNull(ll);
        assertTrue(ll.size() >= 1);

        final Locale defaultLocale = Locale.getDefault();
        assertTrue(ll.indexOf(defaultLocale) != -1);
    }

    @Test
    public void testGetAdjustedDefault() {
        final LocaleListCompat ll = LocaleListCompat.getDefault();
        assertNotNull(ll);
        assertTrue(ll.size() >= 1);

        final Locale defaultLocale = Locale.getDefault();
        assertTrue(ll.indexOf(defaultLocale) == 0);
    }

    @Test
    public void testGetFirstMatch_noAssets() {
        String[] noAssets = {};
        assertNull(LocaleListCompat.getEmptyLocaleList().getFirstMatch(noAssets));
        assertEquals(
                forLanguageTag("fr-BE"),
                LocaleListCompat.forLanguageTags("fr-BE").getFirstMatch(noAssets));
        assertEquals(
                forLanguageTag("fr-BE"),
                LocaleListCompat.forLanguageTags("fr-BE,nl-BE").getFirstMatch(noAssets));
    }

    @Test
    public void testGetFirstMatch_oneAsset() {
        String[] oneDutchAsset = {"nl"};
        assertEquals(
                forLanguageTag("fr-BE"),
                LocaleListCompat.forLanguageTags("fr-BE").getFirstMatch(oneDutchAsset));
        assertEquals(
                forLanguageTag("nl-BE"),
                LocaleListCompat.forLanguageTags("nl-BE").getFirstMatch(oneDutchAsset));
        assertEquals(
                forLanguageTag("nl-BE"),
                LocaleListCompat.forLanguageTags("fr-BE,nl-BE").getFirstMatch(oneDutchAsset));
        assertEquals(
                forLanguageTag("nl-BE"),
                LocaleListCompat.forLanguageTags("nl-BE,fr-BE").getFirstMatch(oneDutchAsset));
    }

    @Test
    public void testGetFirstMatch_twoAssets() {
        String[] FrenchAndDutchAssets = {"fr", "nl"};
        assertEquals(
                forLanguageTag("fr-BE"),
                LocaleListCompat.forLanguageTags("fr-BE").getFirstMatch(FrenchAndDutchAssets));
        assertEquals(
                forLanguageTag("nl-BE"),
                LocaleListCompat.forLanguageTags("nl-BE").getFirstMatch(FrenchAndDutchAssets));
        assertEquals(
                forLanguageTag("fr-BE"),
                LocaleListCompat.forLanguageTags("fr-BE,nl-BE")
                        .getFirstMatch(FrenchAndDutchAssets));
        assertEquals(
                forLanguageTag("nl-BE"),
                LocaleListCompat.forLanguageTags("nl-BE,fr-BE")
                        .getFirstMatch(FrenchAndDutchAssets));
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testGetFirstMatch_oneChineseAsset() {
        String[] oneChineseAsset = {"zh-CN"};  // Assumed to mean zh-Hans-CN
        // The following Chinese examples would all match, so they will be chosen.
        assertEquals(
                forLanguageTag("zh"),
                LocaleListCompat.forLanguageTags("ko-KR,zh").getFirstMatch(oneChineseAsset));
        assertEquals(
                forLanguageTag("zh-CN"),
                LocaleListCompat.forLanguageTags("ko-KR,zh-CN").getFirstMatch(oneChineseAsset));
        assertEquals(
                forLanguageTag("zh-Hans"),
                LocaleListCompat.forLanguageTags("ko-KR,zh-Hans").getFirstMatch(oneChineseAsset));
        assertEquals(
                forLanguageTag("zh-Hans-CN"),
                LocaleListCompat.forLanguageTags("ko-KR,zh-Hans-CN")
                        .getFirstMatch(oneChineseAsset));
        assertEquals(
                forLanguageTag("zh-Hans-HK"),
                LocaleListCompat.forLanguageTags("ko-KR,zh-Hans-HK")
                        .getFirstMatch(oneChineseAsset));

        // The following Chinese examples wouldn't match, so the first locale will be chosen
        // instead.
        assertEquals(
                forLanguageTag("ko-KR"),
                LocaleListCompat.forLanguageTags("ko-KR,zh-TW").getFirstMatch(oneChineseAsset));
        assertEquals(
                forLanguageTag("ko-KR"),
                LocaleListCompat.forLanguageTags("ko-KR,zh-Hant").getFirstMatch(oneChineseAsset));
        assertEquals(
                forLanguageTag("ko-KR"),
                LocaleListCompat.forLanguageTags("ko-KR,zh-Hant-TW")
                        .getFirstMatch(oneChineseAsset));
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testGetFirstMatch_serbianCyrillic() {
        String[] oneSerbianAsset = {"sr"};  // Assumed to mean sr-Cyrl-RS
        // The following Serbian examples would all match, so they will be chosen.
        assertEquals(
                forLanguageTag("sr"),
                LocaleListCompat.forLanguageTags("hr-HR,sr").getFirstMatch(oneSerbianAsset));
        assertEquals(
                forLanguageTag("sr-RS"),
                LocaleListCompat.forLanguageTags("hr-HR,sr-RS").getFirstMatch(oneSerbianAsset));
        assertEquals(
                forLanguageTag("sr-Cyrl"),
                LocaleListCompat.forLanguageTags("hr-HR,sr-Cyrl").getFirstMatch(oneSerbianAsset));
        assertEquals(
                forLanguageTag("sr-Cyrl-RS"),
                LocaleListCompat.forLanguageTags("hr-HR,sr-Cyrl-RS")
                        .getFirstMatch(oneSerbianAsset));
        assertEquals(
                forLanguageTag("sr-Cyrl-ME"),
                LocaleListCompat.forLanguageTags("hr-HR,sr-Cyrl-ME")
                        .getFirstMatch(oneSerbianAsset));

        // The following Serbian examples wouldn't match, so the first locale will be chosen
        // instead.
        assertEquals(
                forLanguageTag("hr-HR"),
                LocaleListCompat.forLanguageTags("hr-HR,sr-ME").getFirstMatch(oneSerbianAsset));
        assertEquals(
                forLanguageTag("hr-HR"),
                LocaleListCompat.forLanguageTags("hr-HR,sr-Latn").getFirstMatch(oneSerbianAsset));
        assertEquals(
                forLanguageTag("hr-HR"),
                LocaleListCompat.forLanguageTags("hr-HR,sr-Latn-ME")
                        .getFirstMatch(oneSerbianAsset));
    }

    @Test
    public void testGetFirstMatch_LtrPseudoLocale() {
        String[] onePseudoLocale = {"en-XA"};
        // "en-XA" matches itself
        assertEquals(
                forLanguageTag("en-XA"),
                LocaleListCompat.forLanguageTags("sr,en-XA").getFirstMatch(onePseudoLocale));

        // "en-XA" doesn't match "en" or "en-US"
        assertEquals(
                forLanguageTag("sr"),
                LocaleListCompat.forLanguageTags("sr,en").getFirstMatch(onePseudoLocale));
        assertEquals(
                forLanguageTag("sr"),
                LocaleListCompat.forLanguageTags("sr,en-US").getFirstMatch(onePseudoLocale));
    }

    @Test
    public void testGetFirstMatch_RtlPseudoLocale() {
        String[] onePseudoLocale = {"ar-XB"};
        // "ar-XB" matches itself
        assertEquals(
                forLanguageTag("ar-XB"),
                LocaleListCompat.forLanguageTags("sr,ar-XB").getFirstMatch(onePseudoLocale));

        // "ar-XB" doesn't match "ar" or "ar-EG"
        assertEquals(
                forLanguageTag("sr"),
                LocaleListCompat.forLanguageTags("sr,ar").getFirstMatch(onePseudoLocale));
        assertEquals(
                forLanguageTag("sr"),
                LocaleListCompat.forLanguageTags("sr,ar-EG").getFirstMatch(onePseudoLocale));
    }

    @Test
    public void testGetFirstMatch_privateUseWithoutCountry() {
        String[] onePrivateLocale = {"qaa"};
        // "qaa" supports itself and "qaa-CA"
        assertEquals(
                forLanguageTag("qaa"),
                LocaleListCompat.forLanguageTags("sr,qaa").getFirstMatch(onePrivateLocale));
        assertEquals(
                forLanguageTag("qaa-CA"),
                LocaleListCompat.forLanguageTags("sr,qaa-CA").getFirstMatch(onePrivateLocale));
    }

    @Test
    public void testGetFirstMatch_privateUseWithCountry() {
        String[] onePrivateLocale = {"qaa-US"};
        // "qaa-US" supports itself
        assertEquals(
                forLanguageTag("qaa-US"),
                LocaleListCompat.forLanguageTags("sr,qaa-US").getFirstMatch(onePrivateLocale));

        // "qaa-US" doesn't support "qaa" or "qaa-CA"
        assertEquals(
                forLanguageTag("sr"),
                LocaleListCompat.forLanguageTags("sr,qaa-CA").getFirstMatch(onePrivateLocale));
        assertEquals(
                forLanguageTag("sr"),
                LocaleListCompat.forLanguageTags("sr,qaa").getFirstMatch(onePrivateLocale));
    }

    private Locale forLanguageTag(String str) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Locale.forLanguageTag(str);
        } else {
            return LocaleHelper.forLanguageTag(str);
        }
    }

    private String toLanguageTag(Locale locale) {
        if (Build.VERSION.SDK_INT >= 21) {
            return locale.toLanguageTag();
        } else {
            return LocaleHelper.toLanguageTag(locale);
        }
    }

}
