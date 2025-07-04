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
import static org.junit.Assert.fail;

import android.app.PendingIntent;
import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;


@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CustomContentActionTest {

    @Test
    public void testBuilder_validImageAction() {
        int id = 1;
        String label = "Pin Image";
        PendingIntent pendingIntent = TestUtil.makeMockPendingIntent();
        int targetType = CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE;

        CustomContentAction action = new CustomContentAction.Builder(id, label, pendingIntent,
                targetType).build();

        assertEquals(id, action.getId());
        assertEquals(label, action.getLabel());
        assertEquals(pendingIntent, action.getPendingIntent());
        assertEquals(targetType, action.getTargetType());
    }

    @Test
    public void testBuilder_validLinkAction() {
        int id = 2;
        String label = "Share Link";
        PendingIntent pendingIntent = TestUtil.makeMockPendingIntent();
        int targetType = CustomTabsIntent.CONTENT_TARGET_TYPE_LINK;

        CustomContentAction action = new CustomContentAction.Builder(id, label, pendingIntent,
                targetType).build();

        assertEquals(id, action.getId());
        assertEquals(label, action.getLabel());
        assertEquals(pendingIntent, action.getPendingIntent());
        assertEquals(targetType, action.getTargetType());
    }

    @Test
    public void testBuilder_emptyLabel_throwsIllegalArgumentException() {
        try {
            new CustomContentAction.Builder(1, "", TestUtil.makeMockPendingIntent(),
                    CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE);
            fail("Expected IllegalArgumentException for empty label");
        } catch (IllegalArgumentException e) {
            // Expected
            assertEquals("Label cannot be empty.", e.getMessage());
        }
    }

    @Test
    public void testBuilder_invalidId_throwsIllegalArgumentException() {
        try {
            new CustomContentAction.Builder(-1, "Test", TestUtil.makeMockPendingIntent(),
                    CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE);
            fail("Expected IllegalArgumentException for invalid id");
        } catch (IllegalArgumentException e) {
            // Expected
            assertEquals("Id cannot be set to negative numbers.", e.getMessage());
        }
    }

    @Test
    public void testBuilder_invalidTargetType_throwsIllegalArgumentException() {
        try {
            new CustomContentAction.Builder(1, "Test", TestUtil.makeMockPendingIntent(),
                    99); // 99 is invalid
            fail("Expected IllegalArgumentException for invalid target type");
        } catch (IllegalArgumentException e) {
            // Expected
            assertEquals("Invalid target type: 99", e.getMessage());
        }
    }

    @Test
    public void testToBundleAndFromBundle_serialization() {
        int id = 10;
        String label = "Test Action";
        PendingIntent pendingIntent = TestUtil.makeMockPendingIntent();
        int targetType = CustomTabsIntent.CONTENT_TARGET_TYPE_LINK;
        CustomContentAction originalAction = new CustomContentAction.Builder(id, label,
                pendingIntent, targetType).build();

        Bundle bundle = originalAction.toBundle();
        assertNotNull(bundle);

        CustomContentAction restoredAction = CustomContentAction.fromBundle(bundle);
        assertNotNull(restoredAction);
        assertEquals(originalAction.getId(), restoredAction.getId());
        assertEquals(originalAction.getLabel(), restoredAction.getLabel());
        assertEquals(originalAction.getPendingIntent(), restoredAction.getPendingIntent());
        assertEquals(originalAction.getTargetType(), restoredAction.getTargetType());
    }

    @Test
    public void testFromBundle_missingId_returnsNull() {
        Bundle bundle = new Bundle();
        bundle.putString(CustomContentAction.KEY_LABEL, "Label");
        bundle.putParcelable(CustomContentAction.KEY_PENDING_INTENT,
                TestUtil.makeMockPendingIntent());
        bundle.putInt(CustomContentAction.KEY_TARGET_TYPE,
                CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE);
        assertNull(CustomContentAction.fromBundle(bundle));
    }

    @Test
    public void testFromBundle_missingLabel_returnsNull() {
        Bundle bundle = new Bundle();
        bundle.putInt(CustomContentAction.KEY_ID, 1);
        bundle.putParcelable(CustomContentAction.KEY_PENDING_INTENT,
                TestUtil.makeMockPendingIntent());
        bundle.putInt(CustomContentAction.KEY_TARGET_TYPE,
                CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE);
        assertNull(CustomContentAction.fromBundle(bundle));
    }

    @Test
    public void testFromBundle_emptyLabel_returnsNull() {
        Bundle bundle = new Bundle();
        bundle.putInt(CustomContentAction.KEY_ID, 1);
        bundle.putString(CustomContentAction.KEY_LABEL, "");
        bundle.putParcelable(CustomContentAction.KEY_PENDING_INTENT,
                TestUtil.makeMockPendingIntent());
        bundle.putInt(CustomContentAction.KEY_TARGET_TYPE,
                CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE);
        assertNull(CustomContentAction.fromBundle(bundle));
    }

    @Test
    public void testFromBundle_missingPendingIntent_returnsNull() {
        Bundle bundle = new Bundle();
        bundle.putInt(CustomContentAction.KEY_ID, 1);
        bundle.putString(CustomContentAction.KEY_LABEL, "Label");
        bundle.putInt(CustomContentAction.KEY_TARGET_TYPE,
                CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE);
        assertNull(CustomContentAction.fromBundle(bundle));
    }

    @Test
    public void testFromBundle_missingTargetType_returnsNull() {
        Bundle bundle = new Bundle();
        bundle.putInt(CustomContentAction.KEY_ID, 1);
        bundle.putString(CustomContentAction.KEY_LABEL, "Label");
        bundle.putParcelable(CustomContentAction.KEY_PENDING_INTENT,
                TestUtil.makeMockPendingIntent());
        // KEY_TARGET_TYPE not set, defaults to 0 in getInt, which is invalid.
        assertNull(CustomContentAction.fromBundle(bundle));
    }

    @Test
    public void testFromBundle_invalidTargetTypeInBundle_returnsNull() {
        Bundle bundle = new Bundle();
        bundle.putInt(CustomContentAction.KEY_ID, 1);
        bundle.putString(CustomContentAction.KEY_LABEL, "Label");
        bundle.putParcelable(CustomContentAction.KEY_PENDING_INTENT,
                TestUtil.makeMockPendingIntent());
        bundle.putInt(CustomContentAction.KEY_TARGET_TYPE, 99); // Invalid value
        assertNull(CustomContentAction.fromBundle(bundle));
    }
}
