/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.browser.browseractions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for {@link BrowserActionsIntent}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class BrowserActionsIntentTest {
    private static final String TEST_URL = "http://www.example.com";
    private static final String CUSTOM_ITEM_TITLE = "Share url";
    private Uri mUri = Uri.parse(TEST_URL);
    private Context mContext = InstrumentationRegistry.getTargetContext();

    /**
     * Test whether default {@link BrowserActionsIntent} is populated correctly.
     */
    @Test
    public void testDefaultBrowserActionsIntent() {
        BrowserActionsIntent browserActionsIntent =
                new BrowserActionsIntent.Builder(mContext, mUri).build();
        Intent intent = browserActionsIntent.getIntent();
        assertNotNull(intent);

        assertEquals(BrowserActionsIntent.ACTION_BROWSER_ACTIONS_OPEN, intent.getAction());
        assertEquals(mUri, intent.getData());
        assertTrue(intent.hasExtra(BrowserActionsIntent.EXTRA_TYPE));
        assertEquals(BrowserActionsIntent.URL_TYPE_NONE,
                intent.getIntExtra(BrowserActionsIntent.EXTRA_TYPE, 0));
        assertTrue(intent.hasExtra(BrowserActionsIntent.EXTRA_APP_ID));
        assertEquals(mContext.getPackageName(), BrowserActionsIntent.getCreatorPackageName(intent));
        assertFalse(intent.hasExtra(BrowserActionsIntent.EXTRA_SELECTED_ACTION_PENDING_INTENT));
    }

    @Test
    /**
     * Test whether custom items are set correctly.
     */
    public void testCustomItem() {
        PendingIntent action1 = createCustomItemAction(TEST_URL);
        BrowserActionItem customItemWithoutIcon = new BrowserActionItem(CUSTOM_ITEM_TITLE, action1);
        PendingIntent action2 = createCustomItemAction(TEST_URL);
        BrowserActionItem customItemWithIcon =
                new BrowserActionItem(CUSTOM_ITEM_TITLE, action2, android.R.drawable.ic_menu_share);
        ArrayList<BrowserActionItem> customItems = new ArrayList<>();
        customItems.add(customItemWithIcon);
        customItems.add(customItemWithoutIcon);

        BrowserActionsIntent browserActionsIntent = new BrowserActionsIntent.Builder(mContext, mUri)
                .setCustomItems(customItems)
                .build();
        Intent intent = browserActionsIntent.getIntent();
        assertTrue(intent.hasExtra(BrowserActionsIntent.EXTRA_MENU_ITEMS));
        ArrayList<Bundle> bundles =
                intent.getParcelableArrayListExtra(BrowserActionsIntent.EXTRA_MENU_ITEMS);
        assertNotNull(bundles);
        List<BrowserActionItem> items = BrowserActionsIntent.parseBrowserActionItems(bundles);
        assertEquals(2, items.size());
        BrowserActionItem items1 = items.get(0);
        assertEquals(CUSTOM_ITEM_TITLE, items1.getTitle());
        assertEquals(android.R.drawable.ic_menu_share, items1.getIconId());
        assertEquals(action1, items1.getAction());
        BrowserActionItem items2 = items.get(1);
        assertEquals(CUSTOM_ITEM_TITLE, items2.getTitle());
        assertEquals(0, items2.getIconId());
        assertEquals(action2, items2.getAction());
    }

    static PendingIntent createCustomItemAction(String url) {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent customIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        return PendingIntent.getActivity(context, 0, customIntent, 0);
    }
}
