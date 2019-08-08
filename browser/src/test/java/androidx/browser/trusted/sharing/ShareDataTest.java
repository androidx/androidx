/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.trusted.sharing;

import static org.junit.Assert.assertEquals;

import android.net.Uri;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests for {@link ShareData}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@SmallTest
public class ShareDataTest {

    private static final List<Uri> URIS = Arrays.asList(Uri.parse("http://foo"),
            Uri.parse("http://bar"));

    @ParameterizedRobolectricTestRunner.Parameters(name = "{1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{new ShareData("foo", "bar", URIS), "All included"},
                new Object[]{new ShareData("foo", "bar", null), "No URIs"},
                new Object[]{new ShareData(null, "bar", URIS), "No title"},
                new Object[]{new ShareData("foo", null, URIS), "No text"});
    }

    private final ShareData mShareData;

    public ShareDataTest(ShareData shareData, String testName) {
        mShareData = shareData;
    }

    @Test
    public void bundlingAndUnbundlingYieldsOriginalObject() {
        assertShareDataEquals(mShareData, ShareData.fromBundle(mShareData.toBundle()));
    }

    private void assertShareDataEquals(ShareData expected, ShareData actual) {
        assertEquals(expected.title, actual.title);
        assertEquals(expected.text, actual.text);
        assertEquals(expected.uris, actual.uris);
    }
}
