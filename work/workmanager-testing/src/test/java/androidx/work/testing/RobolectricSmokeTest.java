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

package androidx.work.testing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.testing.workers.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

@Config(manifest = Config.NONE, maxSdk = 30) // Robolectric uses wrong maxSdk by default
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RobolectricSmokeTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext);
    }

    @Test(timeout = 10000)
    public void testWorker_shouldSucceedSynchronously()
            throws InterruptedException, ExecutionException {
        WorkRequest request = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        // TestWorkManagerImpl is a subtype of WorkManagerImpl.
        WorkManagerImpl workManagerImpl = WorkManagerImpl.getInstance(mContext);
        workManagerImpl.enqueue(Collections.singletonList(request)).getResult().get();
        WorkInfo status = workManagerImpl.getWorkInfoById(request.getId()).get();
        assertThat(status.getState().isFinished(), is(true));
    }
}
