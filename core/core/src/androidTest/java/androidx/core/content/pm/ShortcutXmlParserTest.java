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

package androidx.core.content.pm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.util.Xml;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ShortcutXmlParserTest {

    private static final String SHORTCUTS_XML = "<shortcuts xmlns:android=\"http://schemas"
            + ".android.com/apk/res/android\">\n"
            + "    <shortcut\n"
            + "            android:shortcutId=\"my_shortcut\"\n"
            + "            android:enabled=\"true\"\n"
            + "            android:icon=\"@mipmap/logo_avatar\"\n"
            + "            android:shortcutShortLabel=\"@string/dummy_shortcut_short_label\"\n"
            + "            android:shortcutLongLabel=\"@string/dummy_shortcut_long_label\"\n"
            + "            android:shortcutDisabledMessage=\"@string"
            + "/dummy_shortcut_disabled_message\">\n"
            + "        <intent\n"
            + "                android:action=\"android.intent.action.VIEW\"\n"
            + "                android:targetPackage=\"androidx.sharetarget.testapp\"\n"
            + "                android:targetClass=\"androidx.sharetarget.testapp"
            + ".TextConsumerActivity\" />\n"
            + "        <categories android:name=\"android.shortcut.conversation\" />\n"
            + "    </shortcut>\n"
            + "\n"
            + "    <share-target android:targetClass=\"androidx.sharetarget.testapp"
            + ".TextConsumerActivity\">\n"
            + "        <data android:mimeType=\"text/plain\"/>\n"
            + "        <category\n"
            + "                android:name=\"androidx.sharetarget.testapp.category"
            + ".TEXT_SHARE_TARGET\"/>\n"
            + "    </share-target>\n"
            + "\n"
            + "    <shortcut\n"
            + "            android:shortcutId=\"another_shortcut\"\n"
            + "            android:enabled=\"true\"\n"
            + "            android:icon=\"@mipmap/logo_avatar\"\n"
            + "            android:shortcutShortLabel=\"@string/dummy_shortcut_short_label2\"\n"
            + "            android:shortcutLongLabel=\"@string/dummy_shortcut_long_label2\"\n"
            + "            android:shortcutDisabledMessage=\"@string"
            + "/dummy_shortcut_disabled_message2\">\n"
            + "        <intent\n"
            + "                android:action=\"android.intent.action.VIEW\"\n"
            + "                android:targetPackage=\"androidx.sharetarget.testapp\"\n"
            + "                android:targetClass=\"androidx.sharetarget.testapp"
            + ".TextConsumerActivity\" />\n"
            + "        <categories android:name=\"android.shortcut.conversation\" />\n"
            + "    </shortcut>\n"
            + "\n"
            + "    <share-target android:targetClass=\"androidx.sharetarget.testapp"
            + ".OtherTextConsumerActivity\">\n"
            + "        <data android:mimeType=\"text/plain\"/>\n"
            + "        <category android:name=\"androidx.sharetarget.testapp.category"
            + ".OTHER_TEXT_SHARE_TARGET\"/>\n"
            + "    </share-target>\n"
            + "</shortcuts>\n";

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testParseShortcutIds() throws XmlPullParserException, IOException {
        final XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(SHORTCUTS_XML.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8.name());
        final List<String> shortcutIds = ShortcutXmlParser.parseShortcutIds(parser);
        assertNotNull(shortcutIds);
        assertEquals(2, shortcutIds.size());
        assertTrue(shortcutIds.contains("my_shortcut"));
        assertTrue(shortcutIds.contains("another_shortcut"));
    }
}
