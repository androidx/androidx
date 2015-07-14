/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.app;

import org.junit.Test;

public abstract class BaseBasicsTestCase<A extends BaseTestActivity>
        extends BaseInstrumentationTestCase<A> {

    protected BaseBasicsTestCase(Class<A> activityClass) {
        super(activityClass);
    }

    @Test
    public void testActionBarExists() {
        assertNotNull("ActionBar is not null", getActivity().getSupportActionBar());
    }

    @Test
     public void testDefaultActionBarTitle() {
        assertEquals(getActivity().getTitle(), getActivity().getSupportActionBar().getTitle());
    }

    @Test
    public void testSetActionBarTitle() throws Throwable {
        final String newTitle = "hello";
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().setTitle(newTitle);
                assertEquals("New title is set to ActionBar",
                        newTitle, getActivity().getSupportActionBar().getTitle());
            }
        });
    }
}
