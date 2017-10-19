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

package android.arch.background.workmanager.firebase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import android.arch.background.workmanager.model.WorkSpec;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FirebaseJobConverterTest {
    private FirebaseJobConverter mConverter;

    @Before
    public void setUp() {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(
                new GooglePlayDriver(InstrumentationRegistry.getTargetContext()));
        mConverter = new FirebaseJobConverter(dispatcher);
    }

    @Test
    @SmallTest
    public void testConvert_basicWorkSpec() {
        final String expectedWorkSpecId = "026e3422-9cd1-11e7-abc4-cec278b6b50a";
        WorkSpec workSpec = new WorkSpec(expectedWorkSpecId);
        Job job = mConverter.convert(workSpec);
        assertThat(job.getTag(), is(expectedWorkSpecId));
        assertThat(job.getLifetime(), is(Lifetime.FOREVER));
        assertThat(job.getService(), is(FirebaseJobService.class.getName()));
    }
}
