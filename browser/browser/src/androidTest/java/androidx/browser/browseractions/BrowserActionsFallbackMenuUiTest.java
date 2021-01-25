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

package androidx.browser.browseractions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.browser.R;
import androidx.browser.customtabs.EnableComponentsTestRule;
import androidx.browser.customtabs.TestActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
/** Unit tests for {@link BrowserActionsFallbackMenuUi}. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BrowserActionsFallbackMenuUiTest {
    private static final String TEST_URL = "http://www.example.com";
    private static final String CUSTOM_ITEM_TITLE_1 = "Open url";
    private static final String CUSTOM_ITEM_TITLE_2 = "Share url";
    @Rule
    public final androidx.test.rule.ActivityTestRule<TestActivity> mActivityTestRule =
            new androidx.test.rule.ActivityTestRule<>(TestActivity.class);

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestActivity.class
    );

    private Context mContext;
    private List<BrowserActionItem> mMenuItems;
    private List<String> mMenuItemTitles;

    @Before
    public void setup() {
        mContext = mActivityTestRule.getActivity();
        mMenuItems = createMenuItems();
        mMenuItemTitles = new ArrayList<>();
        mMenuItemTitles.add(mContext.getString(R.string.fallback_menu_item_open_in_browser));
        mMenuItemTitles.add(mContext.getString(R.string.fallback_menu_item_copy_link));
        mMenuItemTitles.add(mContext.getString(R.string.fallback_menu_item_share_link));
        mMenuItemTitles.add(CUSTOM_ITEM_TITLE_1);
        mMenuItemTitles.add(CUSTOM_ITEM_TITLE_2);
    }

    private List<BrowserActionItem> createMenuItems() {
        List<BrowserActionItem> menuItems = new ArrayList<>();
        BrowserActionItem menuItem1 = new BrowserActionItem(
                CUSTOM_ITEM_TITLE_1, BrowserActionsIntentTest.createCustomItemAction(TEST_URL));
        BrowserActionItem menuItem2 = new BrowserActionItem(
                CUSTOM_ITEM_TITLE_2, BrowserActionsIntentTest.createCustomItemAction(TEST_URL));
        menuItems.add(menuItem1);
        menuItems.add(menuItem2);
        return menuItems;
    }

    /**
     * Test whether {@link BrowserActionsFallbackMenuDialog} is opened if not provider is available.
     */
    @Test
    public void testBrowserActionsFallbackDialogOpened() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final BrowserActionsIntent.BrowserActionsFallDialogListener listener =
                new BrowserActionsIntent.BrowserActionsFallDialogListener() {
                    @Override
                    public void onDialogShown() {
                        signal.countDown();
                    }
                };
        BrowserActionsIntent.setDialogShownListenter(listener);
        mActivityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BrowserActionsIntent browserActionsIntent =
                        new BrowserActionsIntent.Builder(mContext, Uri.parse(TEST_URL)).build();
                Intent intent = browserActionsIntent.getIntent();
                BrowserActionsIntent.launchIntent(mContext, intent, null);
            }
        });
        signal.await(5L, TimeUnit.SECONDS);
    }

    /**
     * Test whether {@link BrowserActionsFallbackMenuUi} is inflated correctly.
     */
    @Test
    @Ignore("Test is flaky and we're removing Browser Actions.")
    public void testBrowserActionsFallbackMenuShownCorrectly() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final BrowserActionsFallbackMenuUi.BrowserActionsFallMenuUiListener listener =
                new BrowserActionsFallbackMenuUi.BrowserActionsFallMenuUiListener() {
                    @Override
                    public void onMenuShown(View contentView) {
                        signal.countDown();
                        assertDialogInflatedCorrectly(contentView);
                    }
                };
        mActivityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BrowserActionsFallbackMenuUi menuUi =
                        new BrowserActionsFallbackMenuUi(mContext, Uri.parse(TEST_URL), mMenuItems);
                menuUi.setMenuUiListener(listener);
                menuUi.displayMenu();
            }
        });
        signal.await(5L, TimeUnit.SECONDS);
    }

    private void assertDialogInflatedCorrectly(View contentView) {
        assertNotNull(contentView);
        TextView urlTextView =
                (TextView) contentView.findViewById(R.id.browser_actions_header_text);
        assertNotNull(urlTextView);
        assertEquals(TEST_URL, urlTextView.getText());
        final ListView menuListView =
                (ListView) contentView.findViewById(R.id.browser_actions_menu_items);
        assertNotNull(menuListView);
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mMenuItemTitles.size() == menuListView.getCount();
            }
        });
        for (int i = 0; i < mMenuItemTitles.size(); i++) {
            TextView menuItemTitleView = (TextView) menuListView.getChildAt(i).findViewById(
                    R.id.browser_actions_menu_item_text);
            assertEquals(mMenuItemTitles.get(i), menuItemTitleView.getText());
        }
    }
}