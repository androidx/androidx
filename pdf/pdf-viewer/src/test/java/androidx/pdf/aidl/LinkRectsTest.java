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

package androidx.pdf.aidl;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;

import android.graphics.Rect;

import androidx.pdf.models.LinkRects;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
public class LinkRectsTest {

    private LinkRects mLinkRects = createLinkRects(5, new Integer[]{0, 2, 3},
            new String[]{"http://first.com", "http://second.org", "http://third.net"});

    @Test
    public void testGet() {
        assertThat(mLinkRects.size()).isEqualTo(3);

        assertThat(mLinkRects.get(0).size()).isEqualTo(2);
        assertThat(mLinkRects.get(1).size()).isEqualTo(1);
        assertThat(mLinkRects.get(2).size()).isEqualTo(2);

        try {
            mLinkRects.get(3);
        } catch (Exception e) {
            // As expected.
        }
    }

    @Test
    public void testGetUrl_returnsUrlCorrespondingToLink() {
        assertThat(mLinkRects.getUrl(0)).isEqualTo("http://first.com");
        assertThat(mLinkRects.getUrl(1)).isEqualTo("http://second.org");
        assertThat(mLinkRects.getUrl(2)).isEqualTo("http://third.net");
    }

    @Test
    public void testGetUrlAtPoint() {
        assertThat(mLinkRects.getUrlAtPoint(100, 100)).isEqualTo("http://first.com");
        assertThat(mLinkRects.getUrlAtPoint(200, 201)).isEqualTo("http://first.com");
        assertThat(mLinkRects.getUrlAtPoint(301, 302)).isEqualTo("http://second.org");
        assertThat(mLinkRects.getUrlAtPoint(403, 400)).isEqualTo("http://third.net");
        assertThat(mLinkRects.getUrlAtPoint(502, 501)).isEqualTo("http://third.net");

        assertThat(mLinkRects.getUrlAtPoint(600, 600)).isNull();
        assertThat(mLinkRects.getUrlAtPoint(100, 200)).isNull();
        assertThat(mLinkRects.getUrlAtPoint(510, 500)).isNull();
    }

    @Test
    public void testClassFields_flagsFieldModification() {
        List<String> fields = new ArrayList<>();
        fields.add("NO_LINKS");
        fields.add("CREATOR");
        fields.add("mRects");
        fields.add("mLinkToRect");
        fields.add("mUrls");

        List<String> declaredFields = new ArrayList<>();
        for (Field field : LinkRects.class.getDeclaredFields()) {
            declaredFields.add(field.getName());
        }

        assertTrue(fields.containsAll(declaredFields));
    }

    private static LinkRects createLinkRects(int numRects, Integer[] linkToRect, String[] urls) {
        List<Rect> rects = new ArrayList<Rect>();
        for (int i = 1; i <= numRects; i++) {
            rects.add(new Rect(i * 100, i * 100, i * 101, i * 101));
        }
        return new LinkRects(rects, Arrays.asList(linkToRect), Arrays.asList(urls));
    }
}
