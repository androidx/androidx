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

package androidx.camera.integration.core.idlingresource;

import androidx.test.espresso.IdlingResource;

/** Idling resource which will block until the timeout occurs. */
public class ElapsedTimeIdlingResource implements IdlingResource {

    private long mStartTime;
    private long mWaitTime;
    private IdlingResource.ResourceCallback mResourceCallback;

    public ElapsedTimeIdlingResource(long waitTime) {
        mStartTime = System.currentTimeMillis();
        mWaitTime = waitTime;
    }

    @Override
    public String getName() {
        return "ElapsedTimeIdlingResource:" + mWaitTime;
    }

    @Override
    public boolean isIdleNow() {
        if ((System.currentTimeMillis() - mStartTime) >= mWaitTime) {
            mResourceCallback.onTransitionToIdle();
            return true;
        }
        return false;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }
}
