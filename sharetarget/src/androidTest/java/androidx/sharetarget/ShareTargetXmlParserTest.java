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

package androidx.sharetarget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ShareTargetXmlParserTest {

    private static final ArrayList<ShareTargetCompat> sExpectedValues = new ArrayList<>();

    private Context mContext;

    @Before
    public void setup() {
        mContext = spy(new ContextWrapper(InstrumentationRegistry.getContext()));
        initExpectedValues();
    }

    private void initExpectedValues() {
        // These values must exactly match the content of shortcuts.xml resource
        sExpectedValues.add(new ShareTargetCompat(
                new ShareTargetCompat.TargetData[]{
                        new ShareTargetCompat.TargetData("http", "www.google.com", "1234",
                                "somePath", "somePathPattern", "somePathPrefix", "text/plain")},
                "com.test.googlex.directshare.TestActivity1",
                new String[]{"com.test.googlex.category.CATEGORY1",
                        "com.test.googlex.category.CATEGORY2"}));
        sExpectedValues.add(new ShareTargetCompat(
                new ShareTargetCompat.TargetData[]{
                        new ShareTargetCompat.TargetData(null, null, null, null, null, null,
                                "video/mp4"),
                        new ShareTargetCompat.TargetData("content", null, null, null, null, null,
                                "video/*")},
                "com.test.googlex.directshare.TestActivity5",
                new String[]{"com.test.googlex.category.CATEGORY5",
                        "com.test.googlex.category.CATEGORY6"}));
    }

    private void assertShareTargetEquals(ShareTargetCompat expected, ShareTargetCompat actual) {
        assertEquals(expected.mTargetData.length, actual.mTargetData.length);
        for (int i = 0; i < expected.mTargetData.length; i++) {
            assertEquals(expected.mTargetData[i].mScheme, actual.mTargetData[i].mScheme);
            assertEquals(expected.mTargetData[i].mHost, actual.mTargetData[i].mHost);
            assertEquals(expected.mTargetData[i].mPort, actual.mTargetData[i].mPort);
            assertEquals(expected.mTargetData[i].mPath, actual.mTargetData[i].mPath);
            assertEquals(expected.mTargetData[i].mPathPrefix, actual.mTargetData[i].mPathPrefix);
            assertEquals(expected.mTargetData[i].mPathPattern, actual.mTargetData[i].mPathPattern);
            assertEquals(expected.mTargetData[i].mMimeType, actual.mTargetData[i].mMimeType);
        }

        assertEquals(expected.mTargetClass, actual.mTargetClass);

        assertEquals(expected.mCategories.length, actual.mCategories.length);
        for (int i = 0; i < expected.mCategories.length; i++) {
            assertEquals(expected.mCategories[i], actual.mCategories[i]);
        }
    }

    /**
     * Tests if ShareTargetXmlParser is able to:
     * a) locate the xml resource and read it
     * b) ignore the legacy shortcut definitions if any
     * c) drop incomplete share-target definitions
     * d) read and return all valid share-targets from xml
     */
    @Test
    public void testGetShareTargets() {
        ArrayList<ShareTargetCompat> shareTargets = ShareTargetXmlParser.getShareTargets(mContext);

        assertNotNull(shareTargets);
        assertEquals(sExpectedValues.size(), shareTargets.size());
        for (int i = 0; i < sExpectedValues.size(); i++) {
            assertShareTargetEquals(sExpectedValues.get(i), shareTargets.get(i));
        }
    }
}
