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

package androidx.lifecycle;

import android.app.PictureInPictureParams;
import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
@SmallTest
public class MissingClassTest {
    public static class ObserverWithMissingClasses {
        @SuppressWarnings("unused")
        public void newApiMethod(PictureInPictureParams params) {}

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        public void onResume() {}
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingApi() {
        new ReflectiveGenericLifecycleObserver(new ObserverWithMissingClasses());
    }
}
