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

import android.support.test.annotation.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;

public class BasicsTestCaseWithToolbar extends BaseBasicsTestCase<ToolbarActionBarActivity> {
    public BasicsTestCaseWithToolbar() {
        super(ToolbarActionBarActivity.class);
    }

    @Test
    @SmallTest
    @UiThreadTest
    public void testSupportActionModeAppCompatCallbacks() {
        // Since we're using Toolbar, any action modes will be created from the window
        testSupportActionModeAppCompatCallbacks(true);
    }
}
