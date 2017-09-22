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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkerTests {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void context() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        Worker worker = Worker.fromWorkSpec(mContext, work.getWorkSpec());

        assertNotNull(worker);
        assertEquals(mContext.getApplicationContext(), worker.getAppContext());
    }

    @Test
    public void arguments() throws InterruptedException {
        Arguments arguments = new Arguments();
        String key = "KEY";
        String expectedValue = "VALUE";
        arguments.putString(key, expectedValue);

        Work work = new Work.Builder(TestWorker.class).withArguments(arguments).build();
        Worker worker = Worker.fromWorkSpec(mContext, work.getWorkSpec());

        assertNotNull(worker);
        assertNotNull(worker.getArguments());
        assertEquals(expectedValue, worker.getArguments().getString(key, null));

        work = new Work.Builder(TestWorker.class).build();
        worker = Worker.fromWorkSpec(mContext, work.getWorkSpec());

        assertNotNull(worker);
        assertNotNull(worker.getArguments());
        assertTrue(worker.getArguments().isEmpty());
    }
}
