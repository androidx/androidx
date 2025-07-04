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

package androidx.browser.customtabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ContentActionSelectedDataTest {

    @Test
    public void testFromIntent_nullIntent_returnsNull() {
        assertNull(ContentActionSelectedData.fromIntent(null));
    }

    @Test
    public void testFromIntent_validIntent_returnsInstance() {
        Intent intent = new Intent();
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertSame(intent, data.getIntent());
    }

    @Test
    public void testGetPageUrl() {
        Intent intent = new Intent();
        Uri expectedUri = Uri.parse("https://example.com/page");
        intent.setData(expectedUri);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedUri, data.getPageUrl());
    }

    @Test
    public void testGetTriggeredActionId_withExtra() {
        Intent intent = new Intent();
        int expectedActionId = 123;
        intent.putExtra(CustomTabsIntent.EXTRA_TRIGGERED_CUSTOM_CONTENT_ACTION_ID,
                expectedActionId);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedActionId, data.getTriggeredActionId());
    }

    @Test
    public void testGetTriggeredActionId_withoutExtra_returnsDefault() {
        Intent intent = new Intent();
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(-1, data.getTriggeredActionId()); // Default is -1
    }


    @Test
    public void testGetClickedContentTargetType_withImageExtra() {
        Intent intent = new Intent();
        int expectedType = CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE;
        intent.putExtra(CustomTabsIntent.EXTRA_CLICKED_CONTENT_TARGET_TYPE, expectedType);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedType, data.getClickedContentTargetType());
    }

    @Test
    public void testGetClickedContentTargetType_withLinkExtra() {
        Intent intent = new Intent();
        int expectedType = CustomTabsIntent.CONTENT_TARGET_TYPE_LINK;
        intent.putExtra(CustomTabsIntent.EXTRA_CLICKED_CONTENT_TARGET_TYPE, expectedType);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedType, data.getClickedContentTargetType());
    }

    @Test
    public void testGetClickedContentTargetType_withoutExtra_returnsDefault() {
        Intent intent = new Intent();
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(0, data.getClickedContentTargetType()); // Default is 0
    }

    @Test
    public void testGetImageUrl_withExtra() {
        Intent intent = new Intent();
        String expectedUrl = "https://example.com/image.png";
        intent.putExtra(CustomTabsIntent.EXTRA_CONTEXT_IMAGE_URL, expectedUrl);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedUrl, data.getImageUrl());
    }

    @Test
    public void testGetImageUrl_withoutExtra_returnsNull() {
        Intent intent = new Intent();
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertNull(data.getImageUrl());
    }

    @Test
    public void testGetImageDataUri_withExtra() {
        Intent intent = new Intent();
        Uri expectedUri = Uri.parse("content://com.example.provider/image/123");
        intent.putExtra(CustomTabsIntent.EXTRA_CONTEXT_IMAGE_DATA_URI, expectedUri);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedUri, data.getImageDataUri());
    }

    @Test
    public void testGetImageDataUri_withoutExtra_returnsNull() {
        Intent intent = new Intent();
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertNull(data.getImageDataUri());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.S_V2) // Test specific to pre-Tiramisu getParcelable
    public void testGetImageDataUri_withExtra_preTiramisu() {
        Intent intent = new Intent();
        Uri expectedUri = Uri.parse("content://com.example.provider/image/pre_t_test");
        intent.putExtra(CustomTabsIntent.EXTRA_CONTEXT_IMAGE_DATA_URI, expectedUri);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(null, data.getImageDataUri());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.TIRAMISU)
    public void testGetImageDataUri_withExtra_tiramisuAndAbove() {
        Intent intent = new Intent();
        Uri expectedUri = Uri.parse("content://com.example.provider/image/t_test");
        intent.putExtra(CustomTabsIntent.EXTRA_CONTEXT_IMAGE_DATA_URI, expectedUri);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedUri, data.getImageDataUri());
    }

    @Test
    public void testGetLinkUrl_withExtra() {
        Intent intent = new Intent();
        String expectedUrl = "https://example.com/another-page";
        intent.putExtra(CustomTabsIntent.EXTRA_CONTEXT_LINK_URL, expectedUrl);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedUrl, data.getLinkUrl());
    }

    @Test
    public void testGetLinkUrl_withoutExtra_returnsNull() {
        Intent intent = new Intent();
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertNull(data.getLinkUrl());
    }

    @Test
    public void testGetLinkText_withExtra() {
        Intent intent = new Intent();
        String expectedText = "Click here for more info";
        intent.putExtra(CustomTabsIntent.EXTRA_CONTEXT_LINK_TEXT, expectedText);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedText, data.getLinkText());
    }

    @Test
    public void testGetLinkText_withoutExtra_returnsNull() {
        Intent intent = new Intent();
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertNull(data.getLinkText());
    }

    @Test
    public void testGetAltText_withExtra() {
        Intent intent = new Intent();
        String expectedAltText = "A beautiful sunset";
        intent.putExtra(CustomTabsIntent.EXTRA_CONTEXT_IMAGE_ALT_TEXT, expectedAltText);

        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertEquals(expectedAltText, data.getImageAltText());
    }

    @Test
    public void testGetAltText_withoutExtra_returnsNull() {
        Intent intent = new Intent();
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(intent);
        assertNotNull(data);
        assertNull(data.getImageAltText());
    }

    @Test
    public void testGetIntent_returnsOriginal() {
        Intent originalIntent = new Intent();
        originalIntent.putExtra("test_key", "test_value");
        ContentActionSelectedData data = ContentActionSelectedData.fromIntent(originalIntent);
        assertNotNull(data);
        assertSame(originalIntent, data.getIntent());
        assertEquals("test_value", data.getIntent().getStringExtra("test_key"));
    }
}
