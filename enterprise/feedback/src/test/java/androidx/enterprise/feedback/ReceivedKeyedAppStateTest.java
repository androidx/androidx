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

import static androidx.enterprise.feedback.KeyedAppState.SEVERITY_INFO;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_DATA;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_KEY;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_MESSAGE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_SEVERITY;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.fail;

import android.os.Bundle;

import androidx.enterprise.feedback.ReceivedKeyedAppState.ReceivedKeyedAppStateBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests {@link ReceivedKeyedAppState}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = 21)
public class ReceivedKeyedAppStateTest {

    private static final String KEY = "key";
    private static final String MESSAGE = "message";
    private static final int SEVERITY = SEVERITY_INFO;
    private static final String DATA = "data";
    private static final String PACKAGE_NAME = "com.package";
    private static final long TIMESTAMP = 12345;

    @Test
    public void fromBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(APP_STATE_KEY, KEY);
        bundle.putString(APP_STATE_MESSAGE, MESSAGE);
        bundle.putInt(APP_STATE_SEVERITY, SEVERITY);
        bundle.putString(APP_STATE_DATA, DATA);

        ReceivedKeyedAppState state = ReceivedKeyedAppState.fromBundle(bundle, PACKAGE_NAME,
                TIMESTAMP);

        assertThat(state.getKey()).isEqualTo(KEY);
        assertThat(state.getMessage()).isEqualTo(MESSAGE);
        assertThat(state.getSeverity()).isEqualTo(SEVERITY);
        assertThat(state.getData()).isEqualTo(DATA);
        assertThat(state.getPackageName()).isEqualTo(PACKAGE_NAME);
        assertThat(state.getTimestamp()).isEqualTo(TIMESTAMP);
    }

    @Test
    public void fromBundle_invalidBundle_throwsIllegalArgumentException() {
        Bundle bundle = new Bundle();
        bundle.putString(APP_STATE_KEY, KEY);
        bundle.putString(APP_STATE_MESSAGE, MESSAGE);
        bundle.putString(APP_STATE_DATA, DATA);

        try {
            ReceivedKeyedAppState.fromBundle(bundle, PACKAGE_NAME, TIMESTAMP);
            fail();
        } catch (IllegalArgumentException expected) { }
    }

    @Test
    public void keyIsRequired() {
        ReceivedKeyedAppStateBuilder builder =
                ReceivedKeyedAppState.builder()
                        .setPackageName(PACKAGE_NAME)
                        .setTimestamp(TIMESTAMP)
                        .setSeverity(SEVERITY_INFO)
                        .setMessage(MESSAGE)
                        .setData(DATA);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void severityIsRequired() {
        ReceivedKeyedAppStateBuilder builder =
                ReceivedKeyedAppState.builder()
                        .setPackageName(PACKAGE_NAME)
                        .setTimestamp(TIMESTAMP)
                        .setKey(KEY)
                        .setMessage(MESSAGE)
                        .setData(DATA);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void packageNameIsRequired() {
        ReceivedKeyedAppStateBuilder builder =
                ReceivedKeyedAppState.builder()
                        .setTimestamp(TIMESTAMP)
                        .setKey(KEY)
                        .setSeverity(SEVERITY)
                        .setMessage(MESSAGE)
                        .setData(DATA);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void timestampIsRequired() {
        ReceivedKeyedAppStateBuilder builder =
                ReceivedKeyedAppState.builder()
                        .setPackageName(PACKAGE_NAME)
                        .setKey(KEY)
                        .setSeverity(SEVERITY)
                        .setMessage(MESSAGE)
                        .setData(DATA);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }
}
