/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static androidx.core.graphics.drawable.IconCompatTest.verifyBadgeBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.app.TestActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ShortcutInfoCompatTest {

    private Intent mAction;

    private Context mContext;
    private ShortcutInfoCompat.Builder mBuilder;

    @Before
    public void setup() {
        mContext = spy(new ContextWrapper(InstrumentationRegistry.getContext()));
        mAction = new Intent(Intent.ACTION_VIEW).setPackage(mContext.getPackageName());

        mBuilder = new ShortcutInfoCompat.Builder(mContext, "test-shortcut")
                .setIntent(mAction)
                .setShortLabel("Test shortcut")
                .setIcon(IconCompat.createWithResource(mContext, R.drawable.test_drawable_red));
    }

    @Test
    public void testAddToIntent_noBadge() {
        Intent intent = new Intent();
        mBuilder.setActivity(new ComponentName(mContext, TestActivity.class))
                .build()
                .addToIntent(intent);

        assertEquals(mAction, intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT));
        assertNotNull(intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE));
        assertNull(intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON));
    }

    @Test
    public void testAddToIntent_badgeActivity() {
        Intent intent = new Intent();
        mBuilder.setActivity(new ComponentName(mContext, TestActivity.class))
                .setAlwaysBadged()
                .build()
                .addToIntent(intent);

        assertEquals(mAction, intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT));
        assertNull(intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE));

        verifyBadgeBitmap(intent, ContextCompat.getColor(mContext, R.color.test_red),
                ContextCompat.getColor(mContext, R.color.test_blue));
    }

    @Test
    public void testAddToIntent_badgeApplication() {
        ApplicationInfo appInfo = spy(mContext.getApplicationInfo());
        doReturn(ContextCompat.getDrawable(mContext, R.drawable.test_drawable_green))
                .when(appInfo).loadIcon(any(PackageManager.class));
        doReturn(appInfo).when(mContext).getApplicationInfo();

        Intent intent = new Intent();
        mBuilder.setAlwaysBadged()
                .build()
                .addToIntent(intent);

        assertEquals(mAction, intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT));
        assertNull(intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE));

        verifyBadgeBitmap(intent, ContextCompat.getColor(mContext, R.color.test_red),
                ContextCompat.getColor(mContext, R.color.test_green));
    }
}
