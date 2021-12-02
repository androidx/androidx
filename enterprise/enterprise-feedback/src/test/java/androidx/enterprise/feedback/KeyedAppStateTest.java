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

import static androidx.enterprise.feedback.KeyedAppState.MAX_DATA_LENGTH;
import static androidx.enterprise.feedback.KeyedAppState.MAX_KEY_LENGTH;
import static androidx.enterprise.feedback.KeyedAppState.MAX_MESSAGE_LENGTH;
import static androidx.enterprise.feedback.KeyedAppState.SEVERITY_INFO;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_DATA;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_KEY;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_MESSAGE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_SEVERITY;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.fail;

import android.os.Bundle;

import androidx.enterprise.feedback.KeyedAppState.KeyedAppStateBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests {@link KeyedAppState}. */
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = 21)
public class KeyedAppStateTest {

    private static final String KEY = "key";
    private static final String MESSAGE = "message";
    private static final int SEVERITY = SEVERITY_INFO;
    private static final String DATA = "data";
    private static final int INVALID_SEVERITY = 100;

    @Test
    public void toStateBundle() {
        KeyedAppState keyedAppState =
                KeyedAppState.builder()
                        .setKey(KEY)
                        .setMessage(MESSAGE)
                        .setSeverity(SEVERITY)
                        .setData(DATA)
                        .build();

        Bundle bundle = keyedAppState.toStateBundle();

        assertThat(bundle.getString(APP_STATE_KEY)).isEqualTo(KEY);
        assertThat(bundle.getString(APP_STATE_MESSAGE)).isEqualTo(MESSAGE);
        assertThat(bundle.getInt(APP_STATE_SEVERITY)).isEqualTo(SEVERITY);
        assertThat(bundle.getString(APP_STATE_DATA)).isEqualTo(DATA);
    }

    @Test
    public void isValid() {
        Bundle bundle = new Bundle();
        bundle.putString(APP_STATE_KEY, KEY);
        bundle.putInt(APP_STATE_SEVERITY, SEVERITY);

        assertThat(KeyedAppState.isValid(bundle)).isTrue();
    }

    @Test
    public void isValid_missingKey_isFalse() {
        Bundle bundle = buildTestBundle();
        bundle.remove(APP_STATE_KEY);

        assertThat(KeyedAppState.isValid(bundle)).isFalse();
    }

    @Test
    public void isValid_missingSeverity_isFalse() {
        Bundle bundle = buildTestBundle();
        bundle.remove(APP_STATE_SEVERITY);

        assertThat(KeyedAppState.isValid(bundle)).isFalse();
    }

    @Test
    public void isValid_invalidSeverity_isFalse() {
        Bundle bundle = buildTestBundle();
        bundle.putInt(APP_STATE_SEVERITY, INVALID_SEVERITY);

        assertThat(KeyedAppState.isValid(bundle)).isFalse();
    }

    @Test
    public void isValid_maxKeyLength_isTrue() {
        Bundle bundle = buildTestBundle();
        bundle.putString(APP_STATE_KEY, buildStringOfLength(MAX_KEY_LENGTH));

        assertThat(KeyedAppState.isValid(bundle)).isTrue();
    }

    @Test
    public void isValid_tooHighKeyLength_isFalse() {
        Bundle bundle = buildTestBundle();
        bundle.putString(APP_STATE_KEY, buildStringOfLength(MAX_KEY_LENGTH + 1));

        assertThat(KeyedAppState.isValid(bundle)).isFalse();
    }

    @Test
    public void isValid_maxMessageLength_isTrue() {
        Bundle bundle = buildTestBundle();
        bundle.putString(APP_STATE_MESSAGE, buildStringOfLength(MAX_MESSAGE_LENGTH));

        assertThat(KeyedAppState.isValid(bundle)).isTrue();
    }

    @Test
    public void isValid_tooHighMessageLength_isFalse() {
        Bundle bundle = buildTestBundle();
        bundle.putString(APP_STATE_MESSAGE, buildStringOfLength(MAX_MESSAGE_LENGTH + 1));

        assertThat(KeyedAppState.isValid(bundle)).isFalse();
    }

    @Test
    public void isValid_maxDataLength_isTrue() {
        Bundle bundle = buildTestBundle();
        bundle.putString(APP_STATE_DATA, buildStringOfLength(MAX_DATA_LENGTH));

        assertThat(KeyedAppState.isValid(bundle)).isTrue();
    }

    @Test
    public void isValid_tooHighDataLength_isFalse() {
        Bundle bundle = buildTestBundle();
        bundle.putString(APP_STATE_DATA, buildStringOfLength(MAX_DATA_LENGTH + 1));

        assertThat(KeyedAppState.isValid(bundle)).isFalse();
    }

    @Test
    public void fromBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(APP_STATE_KEY, KEY);
        bundle.putString(APP_STATE_MESSAGE, MESSAGE);
        bundle.putInt(APP_STATE_SEVERITY, SEVERITY);
        bundle.putString(APP_STATE_DATA, DATA);

        KeyedAppState keyedAppState = KeyedAppState.fromBundle(bundle);

        assertThat(keyedAppState.getKey()).isEqualTo(KEY);
        assertThat(keyedAppState.getMessage()).isEqualTo(MESSAGE);
        assertThat(keyedAppState.getSeverity()).isEqualTo(SEVERITY);
        assertThat(keyedAppState.getData()).isEqualTo(DATA);
    }

    @Test
    public void fromBundle_invalidBundle_throwsIllegalArgumentException() {
        Bundle bundle = buildTestBundle();
        bundle.remove(APP_STATE_SEVERITY);

        try {
            KeyedAppState.fromBundle(bundle);
            fail();
        } catch (IllegalArgumentException expected) { }
    }

    @Test
    public void severityDefaultsToInfo() {
        KeyedAppState keyedAppState = KeyedAppState.builder().setKey(KEY).build();

        assertThat(keyedAppState.getSeverity()).isEqualTo(SEVERITY_INFO);
    }

    @Test
    public void messageDefaultsToNull() {
        KeyedAppState keyedAppState = KeyedAppState.builder().setKey(KEY).build();

        assertThat(keyedAppState.getMessage()).isNull();
    }

    @Test
    public void dataDefaultsToNull() {
        KeyedAppState keyedAppState = KeyedAppState.builder().setKey(KEY).build();

        assertThat(keyedAppState.getData()).isNull();
    }

    @Test
    public void buildWithMaxKeyLength_builds() {
        createDefaultKeyedAppStateBuilder().setKey(buildStringOfLength(MAX_KEY_LENGTH)).build();
    }

    @Test
    public void buildWithTooHighKeyLength_throwsIllegalStateException() {
        KeyedAppStateBuilder builder =
                createDefaultKeyedAppStateBuilder().setKey(buildStringOfLength(MAX_KEY_LENGTH + 1));

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void buildWithMaxMessageLength_builds() {
        createDefaultKeyedAppStateBuilder().setMessage(
                buildStringOfLength(MAX_MESSAGE_LENGTH)).build();
    }

    @Test
    public void buildWithTooHighMessageLength_throwsIllegalStateException() {
        KeyedAppStateBuilder builder =
                createDefaultKeyedAppStateBuilder().setMessage(
                        buildStringOfLength(MAX_MESSAGE_LENGTH + 1));

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void buildWithMaxDataLength_builds() {
        createDefaultKeyedAppStateBuilder().setData(buildStringOfLength(MAX_DATA_LENGTH)).build();
    }

    @Test
    public void buildWithTooHighDataLength_throwsIllegalStateException() {
        KeyedAppStateBuilder builder =
                createDefaultKeyedAppStateBuilder().setData(
                        buildStringOfLength(MAX_DATA_LENGTH + 1));

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void buildWithInvalidSeverity_throwsIllegalStateException() {
        KeyedAppStateBuilder builder =
                createDefaultKeyedAppStateBuilder().setSeverity(INVALID_SEVERITY);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void keyIsRequired() {
        KeyedAppStateBuilder builder =
                KeyedAppState.builder().setSeverity(SEVERITY_INFO).setMessage(MESSAGE).setData(
                        DATA);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) { }
    }

    private static KeyedAppStateBuilder createDefaultKeyedAppStateBuilder() {
        return KeyedAppState.builder().setKey(KEY);
    }

    private static Bundle buildTestBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(APP_STATE_KEY, KEY);
        bundle.putString(APP_STATE_MESSAGE, MESSAGE);
        bundle.putInt(APP_STATE_SEVERITY, SEVERITY);
        bundle.putString(APP_STATE_DATA, DATA);

        return bundle;
    }

    private static String buildStringOfLength(int length) {
        return new String(new char[length]);
    }
}
