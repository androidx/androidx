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

import static androidx.car.app.SessionInfo.DISPLAY_TYPE_CLUSTER;
import static androidx.car.app.SessionInfo.DISPLAY_TYPE_MAIN;

import static com.google.common.truth.Truth.assertThat;

import androidx.car.app.model.Template;
import androidx.car.app.versioning.CarAppApiLevels;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class SessionInfoTest {
    protected static final String TEST_SESSION_ID = "test session id";

    @Test
    public void dataClassTest() {
        SessionInfo result = new SessionInfo(DISPLAY_TYPE_MAIN, TEST_SESSION_ID);

        assertThat(result.getSessionId()).isEqualTo(TEST_SESSION_ID);
        assertThat(result.getDisplayType()).isEqualTo(DISPLAY_TYPE_MAIN);
    }

    @Test
    public void getSupportedTemplates_displayTypeMain() {
        SessionInfo sessionInfo = new SessionInfo(DISPLAY_TYPE_MAIN, TEST_SESSION_ID);

        Set<Class<? extends Template>> result =
                sessionInfo.getSupportedTemplates(CarAppApiLevels.LEVEL_5);

        assertThat(result).isNull();
    }

    @Test
    public void getSupportedTemplates_displayTypeCluster() {
        SessionInfo sessionInfo =
                new SessionInfo(DISPLAY_TYPE_CLUSTER, TEST_SESSION_ID);

        Set<Class<? extends Template>> result =
                sessionInfo.getSupportedTemplates(CarAppApiLevels.LEVEL_5);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void getSupportedTemplates_displayTypeCluster_apiLessThan5() {
        SessionInfo sessionInfo =
                new SessionInfo(DISPLAY_TYPE_CLUSTER, TEST_SESSION_ID);

        Set<Class<? extends Template>> result =
                sessionInfo.getSupportedTemplates(CarAppApiLevels.LEVEL_4);

        assertThat(result).isEmpty();
    }

    @Test
    public void equals_comparedAgainstNull_isNotEqual() {
        SessionInfo clusterSessionInfo = new SessionInfo(DISPLAY_TYPE_CLUSTER,
                TEST_SESSION_ID);

        assertThat(clusterSessionInfo).isNotEqualTo(null);
    }

    @Test
    public void equals_sameIdDifferentDisplay_returnsFalse() {
        SessionInfo clusterSessionInfo = new SessionInfo(DISPLAY_TYPE_CLUSTER,
                TEST_SESSION_ID);
        SessionInfo mainDisplaySessionInfo = new SessionInfo(DISPLAY_TYPE_MAIN,
                TEST_SESSION_ID);
        assertThat(clusterSessionInfo).isNotEqualTo(mainDisplaySessionInfo);
    }

    @Test
    public void equals_sameDisplayDifferentId_returnsFalse() {
        SessionInfo clusterSessionInfo = new SessionInfo(DISPLAY_TYPE_MAIN,
                TEST_SESSION_ID);
        SessionInfo mainDisplaySessionInfo = new SessionInfo(DISPLAY_TYPE_MAIN,
                TEST_SESSION_ID + TEST_SESSION_ID);
        assertThat(clusterSessionInfo).isNotEqualTo(mainDisplaySessionInfo);
    }

    @Test
    public void toString_returnsCorrectFormat() {
        SessionInfo displayTypeMainSessionId = new SessionInfo(DISPLAY_TYPE_MAIN, TEST_SESSION_ID);
        SessionInfo anotherDiplayTypeMainSessionId = new SessionInfo(DISPLAY_TYPE_MAIN,
                TEST_SESSION_ID);
        String expected = DISPLAY_TYPE_MAIN + "/" + TEST_SESSION_ID;

        assertThat(displayTypeMainSessionId.toString()).isEqualTo(expected);
    }

    @Test
    public void toString_differentObjects_areEqual() {
        SessionInfo displayTypeMainSessionId = new SessionInfo(DISPLAY_TYPE_MAIN, TEST_SESSION_ID);
        SessionInfo anotherDisplayTypeMainSessionId = new SessionInfo(DISPLAY_TYPE_MAIN,
                TEST_SESSION_ID);

        assertThat(displayTypeMainSessionId.toString()).isEqualTo(
                anotherDisplayTypeMainSessionId.toString());
    }
}
