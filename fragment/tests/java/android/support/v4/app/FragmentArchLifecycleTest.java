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

package android.support.v4.app;

import static org.junit.Assert.assertEquals;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.Bundle;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.EmptyFragmentTestActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FragmentArchLifecycleTest {

    @Rule
    public ActivityTestRule<EmptyFragmentTestActivity> mActivityRule =
            new ActivityTestRule<>(EmptyFragmentTestActivity.class);

    @Test
    @UiThreadTest
    public void testFragmentAdditionDuringOnStop() {
        final EmptyFragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final Fragment first = new Fragment();
        final Fragment second = new Fragment();
        fm.beginTransaction().add(first, "first").commitNow();
        first.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop() {
                fm.beginTransaction().add(second, "second").commitNow();
                first.getLifecycle().removeObserver(this);
            }
        });
        activity.onSaveInstanceState(new Bundle());
        assertEquals(Lifecycle.State.CREATED, first.getLifecycle().getCurrentState());
        assertEquals(Lifecycle.State.CREATED, second.getLifecycle().getCurrentState());
        assertEquals(Lifecycle.State.CREATED, activity.getLifecycle().getCurrentState());
    }
}
