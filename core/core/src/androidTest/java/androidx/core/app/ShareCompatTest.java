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

import android.app.Activity;
import android.content.Intent;
import android.support.v4.BaseInstrumentationTestCase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@MediumTest
public class ShareCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    @Rule
    public ActivityTestRule<TestActivity> testRuleForReading =
            new ActivityTestRule<TestActivity>(TestActivity.class, false, false);

    @Rule
    public ActivityTestRule<TestActivity> testRuleForReadingInterop =
            new ActivityTestRule<TestActivity>(TestActivity.class, false, false);

    public ShareCompatTest() {
        super(TestActivity.class);
    }

    @Test
    public void testBuilder() {
        Activity activity = mActivityTestRule.getActivity();
        ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(activity);
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

    @Test
    public void testReader() {
        Activity activity = mActivityTestRule.getActivity();

        ShareCompat.IntentBuilder intentBuilder =
                ShareCompat.IntentBuilder.from(activity);
        Intent intent = intentBuilder.getIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activityForReading = testRuleForReading.launchActivity(intent);

        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(activityForReading);
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
                ShareCompat.IntentReader.from(activityForReadingInterop);
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
                ShareCompat.IntentReader.from(activityForReadingInterop);
        assertEquals(intentReader.getCallingPackage(), activity.getPackageName());
        assertEquals(intentReader.getCallingActivity(), activity.getComponentName());
    }
}
