/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.java.endtoend.appsetid;

import static com.google.common.truth.Truth.assertThat;

import androidx.privacysandbox.ads.adservices.appsetid.AppSetId;
import androidx.privacysandbox.ads.adservices.java.VersionCompatUtil;
import androidx.privacysandbox.ads.adservices.java.appsetid.AppSetIdManagerFutures;
import androidx.privacysandbox.ads.adservices.java.endtoend.TestUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SdkSuppress(minSdkVersion = 28) // API 28 required for device_config used by this test
// TODO: Consider refactoring so that we're not duplicating code.
public class AppSetIdManagerTest {
    private static final String TAG = "AppSetIdManagerTest";
    TestUtil mTestUtil = new TestUtil(InstrumentationRegistry.getInstrumentation(), TAG);

    @Before
    public void setup() throws Exception {
        mTestUtil.overrideAppSetIdKillSwitch(true);
        mTestUtil.overrideKillSwitches(true);
        mTestUtil.overrideAllowlists(true);

        // Put in a short sleep to make sure the updated config propagates
        // before starting the tests
        Thread.sleep(100);
    }

    @After
    public void teardown() {
        mTestUtil.overrideAppSetIdKillSwitch(false);
        mTestUtil.overrideKillSwitches(false);
        mTestUtil.overrideAllowlists(false);
    }

    @Test
    public void testAppSetId() throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion=*/ 4,
                        /* minExtServicesVersion=*/ 9));

        AppSetIdManagerFutures appSetIdManager =
                AppSetIdManagerFutures.from(ApplicationProvider.getApplicationContext());
        AppSetId appSetId = appSetIdManager.getAppSetIdAsync().get();
        assertThat(appSetId.getId()).isNotEmpty();
        assertThat(appSetId.getScope()).isNotNull();
    }
}
