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

package androidx.navigation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class NavigatorTest {

    @Test
    public void onActive() {
        ActiveAwareNavigator navigator = new ActiveAwareNavigator();
        Navigator.OnNavigatorNavigatedListener listener =
                mock(Navigator.OnNavigatorNavigatedListener.class);
        assertThat("Navigators should start inactive",
                navigator.mIsActive, is(false));

        navigator.addOnNavigatorNavigatedListener(listener);
        assertThat("Navigators should be active after addOnNavigatorNavigatedListener",
                navigator.mIsActive, is(true));
    }

    @Test
    public void onInactive() {
        ActiveAwareNavigator navigator = new ActiveAwareNavigator();
        Navigator.OnNavigatorNavigatedListener listener =
                mock(Navigator.OnNavigatorNavigatedListener.class);
        assertThat("Navigators should start inactive",
                navigator.mIsActive, is(false));

        navigator.addOnNavigatorNavigatedListener(listener);
        assertThat("Navigators should be active after addOnNavigatorNavigatedListener",
                navigator.mIsActive, is(true));

        navigator.removeOnNavigatorNavigatedListener(listener);
        assertThat("Navigators should be inactive after removeOnNavigatorNavigatedListener",
                navigator.mIsActive, is(false));
    }

    private static class ActiveAwareNavigator extends EmptyNavigator {

        boolean mIsActive = false;

        @Override
        public void onActive() {
            mIsActive = true;
        }

        @Override
        public void onInactive() {
            mIsActive = false;
        }
    }
}
