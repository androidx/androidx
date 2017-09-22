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

package android.arch.background.workmanager;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkerWrapperTests {
    private WorkDatabase mDatabase;
    private Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
        mDatabase = WorkDatabase.getInMemoryInstance(mContext);
    }

    @After
    public void tearDown() {
        //TODO(xbhatnag): Include any tear down needed here.
    }

    @Test
    public void success() {
        Work work = new Work.Builder(TestWorker.class).build();
        mDatabase.workSpecDao().insertWorkSpec(work.getWorkSpec());
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, work.getId(), mockListener).run();
        Mockito.verify(mockListener).onSuccess(work.getId());
    }

    @Test
    public void invalidWorkSpecId() {
        final String invalidWorkSpecId = "INVALID_ID";
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, invalidWorkSpecId, mockListener).run();
        Mockito.verify(mockListener).onPermanentError(invalidWorkSpecId);
    }

    @Test
    public void notEnqueuedWorkSpecStatus() {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().mStatus = Work.STATUS_RUNNING;
        mDatabase.workSpecDao().insertWorkSpec(work.getWorkSpec());
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, work.getId(), mockListener).run();
        Mockito.verify(mockListener).onNotEnqueued(work.getId());
    }

    @Test
    public void invalidWorkerClass() {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().mWorkerClassName = "INVALID_CLASS_NAME";
        mDatabase.workSpecDao().insertWorkSpec(work.getWorkSpec());
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, work.getId(), mockListener).run();
        Mockito.verify(mockListener).onPermanentError(work.getId());
    }
}
