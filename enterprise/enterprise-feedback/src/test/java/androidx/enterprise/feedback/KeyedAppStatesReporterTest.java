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

package androidx.enterprise.feedback;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.fail;

import static java.util.Collections.singleton;

import android.content.ContextWrapper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests {@link KeyedAppStatesReporter}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = 21)
public class KeyedAppStatesReporterTest {

    private final ContextWrapper mContext = ApplicationProvider.getApplicationContext();

    private final KeyedAppState mState =
            KeyedAppState.builder().setKey("key").setSeverity(KeyedAppState.SEVERITY_INFO).build();

    @Test
    public void create_nullContext_throwsNullPointerException() {
        try {
            KeyedAppStatesReporter.create(null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void create_createsDefaultKeyedAppStatesReporter() {
        KeyedAppStatesReporter reporter = KeyedAppStatesReporter.create(mContext);

        assertThat(reporter).isInstanceOf(DefaultKeyedAppStatesReporter.class);
    }

    @Test
    public void createWithExecutor_nullContext_throwsNullPointerException() {
        TestExecutor testExecutor = new TestExecutor();
        try {
            KeyedAppStatesReporter.create(null, testExecutor);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void createWithExecutor_nullExecutor_throwsNullPointerException() {
        try {
            KeyedAppStatesReporter.create(mContext, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void createWithExecutor_createsDefaultKeyedAppStatesReporter() {
        TestExecutor testExecutor = new TestExecutor();
        KeyedAppStatesReporter reporter =
                KeyedAppStatesReporter.create(mContext, testExecutor);

        assertThat(reporter).isInstanceOf(DefaultKeyedAppStatesReporter.class);
    }

    @Test
    public void setStates_createWithExecutor_usesExecutor() {
        TestExecutor testExecutor = new TestExecutor();
        KeyedAppStatesReporter reporter =
                KeyedAppStatesReporter.create(mContext, testExecutor);

        reporter.setStates(singleton(mState), /* callback= */ null);

        assertThat(testExecutor.lastExecuted()).isNotNull();
    }
}
