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

package androidx.navigation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.annotation.IdRes;
import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class NavActionTest {
    @IdRes
    private static final int DESTINATION_ID = 1;

    @Test
    public void createAction() {
        NavAction action = new NavAction(DESTINATION_ID);

        assertThat(action.getDestinationId(), is(DESTINATION_ID));
    }

    @Test
    public void createActionWithNullNavOptions() {
        NavAction action = new NavAction(DESTINATION_ID, null);

        assertThat(action.getDestinationId(), is(DESTINATION_ID));
        assertThat(action.getNavOptions(), nullValue());
    }

    @Test
    public void setNavOptions() {
        NavAction action = new NavAction(DESTINATION_ID);
        NavOptions navOptions = new NavOptions.Builder().build();
        action.setNavOptions(navOptions);

        assertThat(action.getNavOptions(), is(navOptions));
    }
}
