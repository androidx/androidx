/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.core.i18n;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.ImmutableMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class SimpleMessageFormatTest {
    private Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test @SmallTest
    public void testBasic() {
        final String message = "Going to {place} and back";
        final String expected = "Going to Germany and back";
        final Map<String, Object> arguments = ImmutableMap.of("place", "Germany");
        Assert.assertEquals("one simple argument, no locale", expected,
                MessageFormat.format(appContext, message, arguments));
        Assert.assertEquals("one simple argument", expected,
                MessageFormat.format(appContext, Locale.US, message, arguments));
    }

    @Test @SmallTest
    public void testSelect() {
        final String msg = "{gender,select,female{her book}male{his book}other{their book}}";
        Assert.assertEquals("select female", "her book",
                MessageFormat.format(appContext, Locale.US, msg,
                        ImmutableMap.of("gender", "female")));
        Assert.assertEquals("select male", "his book",
                MessageFormat.format(appContext, Locale.US, msg,
                        ImmutableMap.of("gender", "male")));
        Assert.assertEquals("select neutral", "their book",
                MessageFormat.format(appContext, Locale.US, msg,
                        ImmutableMap.of("gender", "unknown")));
    }

    @Test @SmallTest
    public void testPlural() {
        // Using Serbian, see
        // http://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html
        Locale sr = new Locale("sr");
        String msg = "{num,plural,offset:1" +
                "  =1    {only {name}}" +
                "  =2    {{name} and one other}" +
                "  one   {{name} and #-one others}" +
                "  few   {{name} and #-few others}" +
                "  other {{name} and #... others}" +
                "}";
        Assert.assertEquals("plural 1", "only Peter",
                MessageFormat.format(appContext, sr, msg,
                        ImmutableMap.of("num", 1, "name", "Peter")));
        Assert.assertEquals("plural 2", "Paul and one other",
                MessageFormat.format(appContext, sr, msg,
                        ImmutableMap.of("num", 2, "name", "Paul")));
        Assert.assertEquals("plural 22", "Mary and 21-one others",
                MessageFormat.format(appContext, sr, msg,
                        ImmutableMap.of("num", 22, "name", "Mary")));
        Assert.assertEquals("plural 33", "John and 32-few others",
                MessageFormat.format(appContext, sr, msg,
                        ImmutableMap.of("num", 33, "name", "John")));
        Assert.assertEquals("plural 6", "Yoko and 5... others",
                MessageFormat.format(appContext, sr, msg,
                        ImmutableMap.of("num", 6, "name", "Yoko")));
    }

    @Test @SmallTest
    public void testSelectAndPlural() {
        Locale ja = Locale.JAPANESE;  // always "other"
        String msg = "{gender,select," +
                "  female {{num,plural, =1 {her book}   other {her # books}}}" +
                "  male   {{num,plural, =1 {his book}   other {his # books}}}" +
                "  other  {{num,plural, =1 {their book} other {their # books}}}" +
                "}";
        Assert.assertEquals("female 1", "her book",
                MessageFormat.format(appContext, ja, msg,
                        ImmutableMap.of("gender", "female", "num", 1)));
        Assert.assertEquals("male 2", "his 2 books",
                MessageFormat.format(appContext, ja, msg,
                        ImmutableMap.of("gender", "male", "num", 2)));
        Assert.assertEquals("unknown 3000", "their 3,000 books",
                MessageFormat.format(appContext, ja, msg,
                        ImmutableMap.of("gender", "?", "num", 3000)));
    }

    @Test @SmallTest
    public void testSelectOrdinal() {
        Locale en = Locale.ENGLISH;
        String msg = "{num,selectordinal," +
                "  =1    {Gold medal}" +
                "  =2    {Silver medal}" +
                "  =3    {Bronze medal}" +
                "  one   {#st place}" +
                "  two   {#nd place}" +
                "  few   {#rd place}" +
                "  other {#th place}" +
                "}";
        Assert.assertEquals("1", "Gold medal",
                MessageFormat.format(appContext, en, msg, ImmutableMap.of("num", 1)));
        Assert.assertEquals("2", "Silver medal",
                MessageFormat.format(appContext, en, msg, ImmutableMap.of("num", 2)));
        Assert.assertEquals("3", "Bronze medal",
                MessageFormat.format(appContext, en, msg, ImmutableMap.of("num", 3)));
        Assert.assertEquals("91", "91st place",
                MessageFormat.format(appContext, en, msg, ImmutableMap.of("num", 91)));
        Assert.assertEquals("22", "22nd place",
                MessageFormat.format(appContext, en, msg, ImmutableMap.of("num", 22)));
        Assert.assertEquals("33", "33rd place",
                MessageFormat.format(appContext, en, msg, ImmutableMap.of("num", 33)));
        Assert.assertEquals("11", "11th place",
                MessageFormat.format(appContext, en, msg, ImmutableMap.of("num", 11)));
    }

    @Test @SmallTest
    public void testSelectOrdinalDefaultLocale() {
        Assert.assertEquals(Locale.US, Locale.getDefault());
        String msg = "{num,selectordinal," +
                "  one   {#st floor}" +
                "  two   {#nd floor}" +
                "  few   {#rd floor}" +
                "  other {#th floor}" +
                "}";
        Assert.assertEquals("91", "91st floor",
                MessageFormat.format(appContext, msg, ImmutableMap.of("num", 91)));
        Assert.assertEquals("22", "22nd floor",
                MessageFormat.format(appContext, msg, ImmutableMap.of("num", 22)));
        Assert.assertEquals("33", "33rd floor",
                MessageFormat.format(appContext, msg, ImmutableMap.of("num", 33)));
        Assert.assertEquals("11", "11th floor",
                MessageFormat.format(appContext, msg, ImmutableMap.of("num", 11)));
    }
}
