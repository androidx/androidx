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

package androidx.privacysandbox.ads.adservices.java.endtoend.adid;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.os.Build;

import androidx.privacysandbox.ads.adservices.adid.AdId;
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo;
import androidx.privacysandbox.ads.adservices.java.adid.AdIdManagerFutures;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class AdIdManagerTest {
    private Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    @Before
    public void setup() throws Exception {
        overrideKillSwitches(true);
    }

    @After
    public void teardown() {
        overrideKillSwitches(false);
    }

    @Test
    public void testAdId() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        AdIdManagerFutures adIdManager =
                AdIdManagerFutures.from(ApplicationProvider.getApplicationContext());
        AdId adId = adIdManager.getAdIdAsync().get();
        assertThat(adId.getAdId()).isNotEmpty();
        assertThat(adId.isLimitAdTrackingEnabled()).isNotNull();
    }

    // Run shell command.
    private void runShellCommand(String command) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mInstrumentation.getUiAutomation().executeShellCommand(command);
            }
        }
    }

    private void overrideKillSwitches(boolean override) {
        if (override) {
            runShellCommand("setprop debug.adservices.adid_kill_switch " + false);
        } else {
            runShellCommand("setprop debug.adservices.adid_kill_switch " + null);
        }
    }
}
