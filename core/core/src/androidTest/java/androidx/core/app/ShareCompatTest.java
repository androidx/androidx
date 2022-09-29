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

package androidx.core.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.BaseInstrumentationTestCase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("deprecation")
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ShareCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    @Rule
    public ActivityTestRule<TestActivity> testRuleForReading =
            new ActivityTestRule<>(TestActivity.class, false, false);

    @Rule
    public ActivityTestRule<TestActivity> testRuleForReadingInterop =
            new ActivityTestRule<>(TestActivity.class, false, false);

    public ShareCompatTest() {
        super(TestActivity.class);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBuilder() {
        Activity activity = mActivityTestRule.getActivity();
        ShareCompat.IntentBuilder intentBuilder = new ShareCompat.IntentBuilder(activity);
        Intent intent = intentBuilder.getIntent();

        assertEquals(intent.getStringExtra(ShareCompat.EXTRA_CALLING_PACKAGE),
                activity.getPackageName());
        assertEquals(intent.getStringExtra(ShareCompat.EXTRA_CALLING_PACKAGE_INTEROP),
                activity.getPackageName());

        assertEquals(intent.getParcelableExtra(ShareCompat.EXTRA_CALLING_ACTIVITY),
                activity.getComponentName());
        assertEquals(intent.getParcelableExtra(ShareCompat.EXTRA_CALLING_ACTIVITY_INTEROP),
                activity.getComponentName());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBuilderWithoutActivity() {
        Context context = mActivityTestRule.getActivity().getApplicationContext();
        ShareCompat.IntentBuilder intentBuilder = new ShareCompat.IntentBuilder(context);
        Intent intent = intentBuilder.getIntent();

        assertEquals(intent.getStringExtra(ShareCompat.EXTRA_CALLING_PACKAGE),
                context.getPackageName());
        assertEquals(intent.getStringExtra(ShareCompat.EXTRA_CALLING_PACKAGE_INTEROP),
                context.getPackageName());

        assertNull(intent.getParcelableExtra(ShareCompat.EXTRA_CALLING_ACTIVITY));
        assertNull(intent.getParcelableExtra(ShareCompat.EXTRA_CALLING_ACTIVITY_INTEROP));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBuilderWithWrappedActivity() {
        Activity activity = mActivityTestRule.getActivity();
        Context context = new ContextWrapper(activity);
        ShareCompat.IntentBuilder intentBuilder = new ShareCompat.IntentBuilder(context);
        Intent intent = intentBuilder.getIntent();

        assertEquals(intent.getStringExtra(ShareCompat.EXTRA_CALLING_PACKAGE),
                activity.getPackageName());
        assertEquals(intent.getStringExtra(ShareCompat.EXTRA_CALLING_PACKAGE_INTEROP),
                activity.getPackageName());

        assertEquals(intent.getParcelableExtra(ShareCompat.EXTRA_CALLING_ACTIVITY),
                activity.getComponentName());
        assertEquals(intent.getParcelableExtra(ShareCompat.EXTRA_CALLING_ACTIVITY_INTEROP),
                activity.getComponentName());
    }

    @SuppressWarnings("deprecation")
    @Test
    @SdkSuppress(minSdkVersion = 16)
    public void testBuilderSingleStreamUri() {
        Activity activity = mActivityTestRule.getActivity();
        Uri uri = Uri.parse("content://fake/file");
        ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(activity);
        intentBuilder.addStream(uri);
        Intent intent = intentBuilder.getIntent();

        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM));
        assertEquals(uri, intent.getClipData().getItemAt(0).getUri());
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }

    @Test
    @SdkSuppress(minSdkVersion = 16)
    public void testBuilderMultipleStreamUris() {
        Activity activity = mActivityTestRule.getActivity();
        Uri uri1 = Uri.parse("content://fake/file1");
        Uri uri2 = Uri.parse("content://fake/file2");
        ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(activity);
        intentBuilder.addStream(uri1);
        intentBuilder.addStream(uri2);
        Intent intent = intentBuilder.getIntent();

        assertEquals(asList(uri1, uri2), intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM));
        assertEquals(uri1, intent.getClipData().getItemAt(0).getUri());
        assertEquals(uri2, intent.getClipData().getItemAt(1).getUri());
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }

    @Test
    public void testReader() {
        Activity activity = mActivityTestRule.getActivity();

        ShareCompat.IntentBuilder intentBuilder = new ShareCompat.IntentBuilder(activity);
        Intent intent = intentBuilder.getIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activityForReading = testRuleForReading.launchActivity(intent);

        ShareCompat.IntentReader intentReader = new ShareCompat.IntentReader(activityForReading);
        assertEquals(intentReader.getCallingPackage(), activity.getPackageName());
        assertEquals(intentReader.getCallingActivity(), activity.getComponentName());
    }

    @Test
    public void testReaderInteropOnlyOld() {
        Activity activity = mActivityTestRule.getActivity();

        Intent intent = new Intent().setAction(Intent.ACTION_SEND);
        intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE_INTEROP,
                activity.getPackageName());
        intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY_INTEROP,
                activity.getComponentName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activityForReadingInterop = testRuleForReadingInterop.launchActivity(intent);

        ShareCompat.IntentReader intentReader =
                new ShareCompat.IntentReader(activityForReadingInterop);
        assertEquals(intentReader.getCallingPackage(), activity.getPackageName());
        assertEquals(intentReader.getCallingActivity(), activity.getComponentName());
    }

    @Test
    public void testReaderInteropOnlyNew() {
        Activity activity = mActivityTestRule.getActivity();

        Intent intent = new Intent().setAction(Intent.ACTION_SEND);
        intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE,
                activity.getPackageName());
        intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY,
                activity.getComponentName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activityForReadingInterop = testRuleForReadingInterop.launchActivity(intent);

        ShareCompat.IntentReader intentReader =
                new ShareCompat.IntentReader(activityForReadingInterop);
        assertEquals(intentReader.getCallingPackage(), activity.getPackageName());
        assertEquals(intentReader.getCallingActivity(), activity.getComponentName());
    }
}
