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

package androidx.work.impl.utils;

import android.support.annotation.RestrictTo;

import androidx.work.impl.Extras;
import androidx.work.impl.WorkManagerImpl;

/**
 * A {@link Runnable} that can start work on the
 * {@link androidx.work.impl.Processor}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StartWorkRunnable implements Runnable {

    private static final String TAG = "StartWorkRunnable";

    private WorkManagerImpl mWorkManagerImpl;
    private String mWorkSpecId;
    private Extras.RuntimeExtras mRuntimeExtras;

    public StartWorkRunnable(
            WorkManagerImpl workManagerImpl,
            String workSpecId,
            Extras.RuntimeExtras runtimeExtras) {
        mWorkManagerImpl = workManagerImpl;
        mWorkSpecId = workSpecId;
        mRuntimeExtras = runtimeExtras;
    }

    @Override
    public void run() {
        mWorkManagerImpl.getProcessor().startWork(mWorkSpecId, mRuntimeExtras);
    }
}
