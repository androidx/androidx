/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.car.app;

import static androidx.car.app.SessionInfo.DEFAULT_SESSION_INFO;
import static androidx.car.app.SessionInfo.DISPLAY_TYPE_CLUSTER;

import static com.google.common.truth.Truth.assertThat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class SessionInfoIntentEncoderTest {
    private static final String EXTRA_SESSION_INFO = "androidx.car.app.extra.SESSION_INFO_BUNDLE";

    @Test
    public void encode_insertsExtra() {
        Intent intent = new Intent();

        SessionInfoIntentEncoder.encode(DEFAULT_SESSION_INFO, intent);

        assertThat(intent.hasExtra(EXTRA_SESSION_INFO)).isTrue();
    }

    @Test
    public void decode() {
        Intent intent = new Intent();
        SessionInfoIntentEncoder.encode(DEFAULT_SESSION_INFO, intent);

        SessionInfo info = SessionInfoIntentEncoder.decode(intent);

        assertThat(info).isEqualTo(DEFAULT_SESSION_INFO);
    }

    @SuppressLint("NewApi")
    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void encode_setsIdentifier_qAndAbove() {
        SessionInfo testSessionInfo =
                new SessionInfo(DISPLAY_TYPE_CLUSTER, "a-unique-session-id");
        Intent intent = new Intent();
        SessionInfoIntentEncoder.encode(testSessionInfo, intent);

        SessionInfo resultSessionInfo = SessionInfoIntentEncoder.decode(intent);

        assertThat(resultSessionInfo).isEqualTo(testSessionInfo);
        assertThat(intent.getIdentifier()).isEqualTo(testSessionInfo.toString());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void encode_setsData_qAndBelow() {
        SessionInfo testSessionInfo =
                new SessionInfo(DISPLAY_TYPE_CLUSTER, "a-unique-session-id");
        Intent intent = new Intent();
        SessionInfoIntentEncoder.encode(testSessionInfo, intent);

        SessionInfo resultSessionInfo = SessionInfoIntentEncoder.decode(intent);

        assertThat(resultSessionInfo).isEqualTo(testSessionInfo);
        assertThat(intent.getDataString()).isEqualTo(testSessionInfo.toString());
    }

    @Test
    public void containsSessionInfo_returnsFalse_whenNoExtras() {
        Intent intent = new Intent();

        assertThat(SessionInfoIntentEncoder.containsSessionInfo(intent)).isFalse();
    }

    @Test
    public void containsSessionInfo_returnsFalse_whenSessionInfoExtraNotSet() {
        Intent intent = new Intent();
        intent.putExtra("test extra", 1);

        assertThat(SessionInfoIntentEncoder.containsSessionInfo(intent)).isFalse();
    }

    @Test
    public void containsSessionInfo() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SESSION_INFO, "test");

        assertThat(SessionInfoIntentEncoder.containsSessionInfo(intent)).isTrue();
    }
}
