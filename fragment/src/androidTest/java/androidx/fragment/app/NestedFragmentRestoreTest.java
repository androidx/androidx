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


package androidx.fragment.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.app.test.FragmentTestActivity.ChildFragment;
import androidx.fragment.app.test.FragmentTestActivity.ChildFragment.OnAttachListener;
import androidx.fragment.app.test.FragmentTestActivity.ParentFragment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class NestedFragmentRestoreTest {

    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule = new ActivityTestRule<>(
            FragmentTestActivity.class);

    public NestedFragmentRestoreTest() {
    }

    @Test
    @SmallTest
    public void recreateActivity() throws Throwable {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FragmentTestActivity.ParentFragment parent = new ParentFragment();
                parent.setRetainChildInstance(true);

                activity.getSupportFragmentManager().beginTransaction()
                        .add(parent, "parent")
                        .commitNow();
            }
        });

        FragmentManager fm = activity.getSupportFragmentManager();
        ParentFragment parent = (ParentFragment) fm.findFragmentByTag("parent");
        ChildFragment child = parent.getChildFragment();

        final Context[] attachedTo = new Context[1];
        final CountDownLatch latch = new CountDownLatch(1);
        child.setOnAttachListener(new OnAttachListener() {
            @Override
            public void onAttach(Context activity, ChildFragment fragment) {
                attachedTo[0] = activity;
                latch.countDown();
            }
        });

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });

        assertTrue("timeout waiting for recreate", latch.await(10, TimeUnit.SECONDS));

        assertNotNull("attached as part of recreate", attachedTo[0]);
        assertNotSame("attached to new context", activity, attachedTo[0]);
        assertNotSame("attached to new parent fragment", parent, child);
    }
}
