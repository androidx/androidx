/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.support.v17.leanback.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import android.support.v17.leanback.widget.GuidanceStylist.Guidance;
import android.support.v17.leanback.widget.GuidedAction;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.espresso.action.ViewActions;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * @hide from javadoc
 */
@RunWith(AndroidJUnit4.class)
public class GuidedStepFragmentTest extends GuidedStepFragmentTestBase {

    @Test
    public void nextAndBack() throws Throwable {
        GuidedStepTestFragment.Provider first = mockProvider("first");
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                List actions = (List) invocation.getArguments()[0];
                actions.add(new GuidedAction.Builder().id(1000).title("OK").build());
                return null;
            }
        }).when(first).onCreateActions(any(List.class), any(Bundle.class));
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                GuidedAction action = (GuidedAction) invocation.getArguments()[0];
                GuidedStepTestFragment.Provider obj = (GuidedStepTestFragment.Provider)
                        invocation.getMock();
                if (action.getId() == 1000) {
                    GuidedStepFragment.add(obj.getFragmentManager(),
                            new GuidedStepTestFragment("second"));
                }
                return null;
            }
        }).when(first).onGuidedActionClicked(any(GuidedAction.class));

        GuidedStepTestFragment.Provider second = mockProvider("second");

        GuidedStepFragmentTestActivity activity = launchTestActivity("first");
        verify(first, times(1)).onCreate(any(Bundle.class));
        verify(first, times(1)).onCreateGuidance(any(Bundle.class));
        verify(first, times(1)).onCreateActions(any(List.class), any(Bundle.class));
        verify(first, times(1)).onCreateButtonActions(any(List.class), any(Bundle.class));
        verify(first, times(1)).onCreateView(any(LayoutInflater.class), any(ViewGroup.class),
                any(Bundle.class), any(View.class));
        verify(first, times(1)).onViewStateRestored(any(Bundle.class));
        verify(first, times(1)).onStart();
        verify(first, times(1)).onResume();

        sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        verify(first, times(1)).onGuidedActionClicked(any(GuidedAction.class));

        waitEnterTransitionFinish(second);
        verify(first, times(1)).onPause();
        verify(first, times(1)).onStop();
        verify(first, times(1)).onDestroyView();
        verify(second, times(1)).onCreate(any(Bundle.class));
        verify(second, times(1)).onCreateGuidance(any(Bundle.class));
        verify(second, times(1)).onCreateActions(any(List.class), any(Bundle.class));
        verify(second, times(1)).onCreateButtonActions(any(List.class), any(Bundle.class));
        verify(second, times(1)).onCreateView(any(LayoutInflater.class), any(ViewGroup.class),
                any(Bundle.class), any(View.class));
        verify(second, times(1)).onViewStateRestored(any(Bundle.class));
        verify(second, times(1)).onStart();
        verify(second, times(1)).onResume();

        sendKey(KeyEvent.KEYCODE_BACK);

        waitEnterTransitionFinish(first);
        verify(second, times(1)).onPause();
        verify(second, times(1)).onStop();
        verify(second, times(1)).onDestroyView();
        verify(second, times(1)).onDestroy();
        verify(first, times(1)).onCreateActions(any(List.class), any(Bundle.class));
        verify(first, times(2)).onCreateView(any(LayoutInflater.class), any(ViewGroup.class),
                any(Bundle.class), any(View.class));
        verify(first, times(2)).onViewStateRestored(any(Bundle.class));
        verify(first, times(2)).onStart();
        verify(first, times(2)).onResume();

        sendKey(KeyEvent.KEYCODE_BACK);
        waitActivityDestroy(activity);
        verify(first, times(1)).onDestroy();
        assertTrue(activity.isDestroyed());
    }

    @Test
    public void restoreFragments() throws Throwable {
        GuidedStepTestFragment.Provider first = mockProvider("first");
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                List actions = (List) invocation.getArguments()[0];
                actions.add(new GuidedAction.Builder().id(1000).title("OK").build());
                actions.add(new GuidedAction.Builder().id(1001).editable(true).title("text")
                        .build());
                actions.add(new GuidedAction.Builder().id(1002).editable(true).title("text")
                        .autoSaveRestoreEnabled(false).build());
                return null;
            }
        }).when(first).onCreateActions(any(List.class), any(Bundle.class));
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                GuidedAction action = (GuidedAction) invocation.getArguments()[0];
                GuidedStepTestFragment.Provider obj = (GuidedStepTestFragment.Provider)
                        invocation.getMock();
                if (action.getId() == 1000) {
                    GuidedStepFragment.add(obj.getFragmentManager(),
                            new GuidedStepTestFragment("second"));
                }
                return null;
            }
        }).when(first).onGuidedActionClicked(any(GuidedAction.class));

        GuidedStepTestFragment.Provider second = mockProvider("second");

        final GuidedStepFragmentTestActivity activity = launchTestActivity("first");
        first.getFragment().findActionById(1001).setTitle("modified text");
        first.getFragment().findActionById(1002).setTitle("modified text");
        sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        waitEnterTransitionFinish(second);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });
        waitEnterTransitionFinish(second);
        verify(first, times(2)).onCreate(any(Bundle.class));
        verify(first, times(1)).onCreateView(any(LayoutInflater.class), any(ViewGroup.class),
                any(Bundle.class), any(View.class));
        verify(first, times(2)).onCreateActions(any(List.class), any(Bundle.class));
        verify(first, times(1)).onDestroy();
        verify(second, times(2)).onCreate(any(Bundle.class));
        verify(second, times(2)).onCreateView(any(LayoutInflater.class), any(ViewGroup.class),
                any(Bundle.class), any(View.class));
        verify(second, times(1)).onDestroy();
        assertEquals("modified text", first.getFragment().findActionById(1001).getTitle());
        assertEquals("text", first.getFragment().findActionById(1002).getTitle());

        sendKey(KeyEvent.KEYCODE_BACK);
        waitEnterTransitionFinish(first);
        verify(second, times(2)).onPause();
        verify(second, times(2)).onStop();
        verify(second, times(2)).onDestroyView();
        verify(second, times(2)).onDestroy();
        verify(first, times(2)).onCreateView(any(LayoutInflater.class), any(ViewGroup.class),
                any(Bundle.class), any(View.class));
    }
}
