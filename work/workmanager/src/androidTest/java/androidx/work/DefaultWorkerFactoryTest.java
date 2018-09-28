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

package androidx.work;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.worker.TestWorker;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DefaultWorkerFactoryTest extends DatabaseTest {

    private Context mContext;
    private DefaultWorkerFactory mDefaultWorkerFactory;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mDefaultWorkerFactory = new DefaultWorkerFactory();
    }

    @Test
    @SmallTest
    public void testCreateWorker_isCreatedSuccessfully() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);

        ListenableWorker worker = mDefaultWorkerFactory.createWorker(
                mContext.getApplicationContext(),
                TestWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        work.getTags(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        new SynchronousExecutor(),
                        mDefaultWorkerFactory));
        assertThat(worker, is(notNullValue()));
        assertThat(worker,
                is(CoreMatchers.<ListenableWorker>instanceOf(TestWorker.class)));
        assertThat(worker.getId(), is(work.getId()));
    }
}
