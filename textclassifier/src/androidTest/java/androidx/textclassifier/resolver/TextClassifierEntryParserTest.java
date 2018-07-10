/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier.resolver;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertTrue;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

@SmallTest
public class TextClassifierEntryParserTest {
    private TextClassifierEntryParser mTextClassifierEntryParser;

    @Before
    public void setup() {
        mTextClassifierEntryParser =
                new TextClassifierEntryParser(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void testParse_twoEntries() throws IOException, XmlPullParserException {
        List<TextClassifierEntry> entries = mTextClassifierEntryParser.parse(
                androidx.textclassifier.test.R.xml.test_xml_1);
        assertThat(entries).hasSize(2);

        TextClassifierEntry firstEntry = entries.get(0);
        TextClassifierEntry secondEntry = entries.get(1);

        assertThat(firstEntry.packageName).isEqualTo("first.package");
        assertThat(firstEntry.certificate).isEqualTo("first.cert");

        assertThat(secondEntry.packageName).isEqualTo("second.package");
        assertThat(secondEntry.certificate).isEqualTo("second.cert");
    }

    @Test
    public void testParse_systemEntry() throws IOException, XmlPullParserException {
        List<TextClassifierEntry> entries = mTextClassifierEntryParser.parse(
                androidx.textclassifier.test.R.xml.test_xml_2);
        assertThat(entries).hasSize(3);

        TextClassifierEntry firstEntry = entries.get(0);
        TextClassifierEntry secondEntry = entries.get(1);
        TextClassifierEntry thirdEntry = entries.get(2);

        assertTrue(firstEntry.isOem());
        assertThat(secondEntry.packageName).isEqualTo("second.package");
        assertThat(secondEntry.certificate).isEqualTo("second.cert");
        assertTrue(thirdEntry.isAosp());
    }

    @Test
    public void testParse_invalidEntry() throws IOException, XmlPullParserException {
        List<TextClassifierEntry> entries = mTextClassifierEntryParser.parse(
                androidx.textclassifier.test.R.xml.test_xml_3);
        assertThat(entries).hasSize(2);

        TextClassifierEntry firstEntry = entries.get(0);
        TextClassifierEntry secondEntry = entries.get(1);

        assertThat(firstEntry.packageName).isEqualTo("first.package");
        assertThat(firstEntry.certificate).isEqualTo("first.cert");

        assertThat(secondEntry.packageName).isEqualTo("second.package");
        assertThat(secondEntry.certificate).isEqualTo("second.cert");
    }
}
