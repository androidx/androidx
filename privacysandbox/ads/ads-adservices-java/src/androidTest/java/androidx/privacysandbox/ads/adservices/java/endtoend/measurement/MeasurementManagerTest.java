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

package androidx.privacysandbox.ads.adservices.java.endtoend.measurement;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.net.Uri;

import androidx.privacysandbox.ads.adservices.java.VersionCompatUtil;
import androidx.privacysandbox.ads.adservices.java.endtoend.TestUtil;
import androidx.privacysandbox.ads.adservices.java.measurement.MeasurementManagerFutures;
import androidx.privacysandbox.ads.adservices.measurement.DeletionRequest;
import androidx.privacysandbox.ads.adservices.measurement.SourceRegistrationRequest;
import androidx.privacysandbox.ads.adservices.measurement.WebSourceParams;
import androidx.privacysandbox.ads.adservices.measurement.WebSourceRegistrationRequest;
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerParams;
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerRegistrationRequest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("NewApi")
@RunWith(JUnit4.class)
@SdkSuppress(minSdkVersion = 28) // API 28 required for device_config used by this test
// TODO: Consider refactoring so that we're not duplicating code.
public class MeasurementManagerTest {
    private static final String TAG = "MeasurementManagerTest";
    TestUtil mTestUtil = new TestUtil(InstrumentationRegistry.getInstrumentation(), TAG);

    /* Note: The source and trigger registration used here must match one of those in
       {@link PreEnrolledAdTechForTest}.
    */
    private static final Uri SOURCE_REGISTRATION_URI = Uri.parse("https://test.com/source");
    private static final Uri TRIGGER_REGISTRATION_URI = Uri.parse("https://test.com/trigger");
    private static final Uri DESTINATION = Uri.parse("http://trigger-origin.com");
    private static final Uri OS_DESTINATION = Uri.parse("android-app://com.os.destination");
    private static final Uri WEB_DESTINATION = Uri.parse("http://web-destination.com");
    private static final Uri ORIGIN_URI = Uri.parse("https://sample.example1.com");
    private static final Uri DOMAIN_URI = Uri.parse("https://example2.com");

    private MeasurementManagerFutures mMeasurementManager;

    @BeforeClass
    public static void presuite() {
        TestUtil testUtil = new TestUtil(InstrumentationRegistry.getInstrumentation(), TAG);
        testUtil.disableDeviceConfigSyncForTests(true);
        testUtil.enableVerboseLogging();
    }

    @AfterClass
    public static void postsuite() {
        TestUtil testUtil = new TestUtil(InstrumentationRegistry.getInstrumentation(), TAG);
        testUtil.disableDeviceConfigSyncForTests(false);
    }

    @Before
    public void setup() throws Exception {
        // To grant access to all pp api app
        mTestUtil.overrideAllowlists(true);
        // We need to turn the Consent Manager into debug mode
        mTestUtil.overrideConsentManagerDebugMode(true);
        mTestUtil.overrideMeasurementKillSwitches(true);
        mTestUtil.overrideAdIdKillSwitch(true);
        mTestUtil.overrideDisableMeasurementEnrollmentCheck("1");
        mMeasurementManager =
                MeasurementManagerFutures.from(ApplicationProvider.getApplicationContext());
        if (VersionCompatUtil.INSTANCE.isSWithMinExtServicesVersion(9)) {
            mTestUtil.enableBackCompatOnS();
        }

        // Put in a short sleep to make sure the updated config propagates
        // before starting the tests
        Thread.sleep(100);
    }

    @After
    public void tearDown() throws Exception {
        mTestUtil.overrideAllowlists(false);
        mTestUtil.overrideConsentManagerDebugMode(false);
        mTestUtil.resetOverrideDisableMeasurementEnrollmentCheck();
        mTestUtil.overrideMeasurementKillSwitches(false);
        mTestUtil.overrideAdIdKillSwitch(false);
        mTestUtil.overrideDisableMeasurementEnrollmentCheck("0");
        if (VersionCompatUtil.INSTANCE.isSWithMinExtServicesVersion(9)) {
            mTestUtil.disableBackCompatOnS();
        }

        // Cool-off rate limiter
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    public void testRegisterSource_NoServerSetup_NoErrors() throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion= */ 5,
                        /* minExtServicesVersionS= */ 9));

        assertThat(
                        mMeasurementManager
                                .registerSourceAsync(
                                        SOURCE_REGISTRATION_URI, /* inputEvent= */ null)
                                .get())
                .isNotNull();
    }

    @Test
    public void testRegisterAppSources_NoServerSetup_NoErrors() throws Exception {
        // Skip the test if the right SDK extension is not present
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion= */ 5,
                        /* minExtServicesVersionS= */ 9));

        SourceRegistrationRequest request =
                new SourceRegistrationRequest.Builder(
                                Collections.singletonList(SOURCE_REGISTRATION_URI))
                        .build();
        assertThat(mMeasurementManager.registerSourceAsync(request).get()).isNotNull();
    }

    @Test
    public void testRegisterTrigger_NoServerSetup_NoErrors() throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion= */ 5,
                        /* minExtServicesVersionS= */ 9));

        assertThat(mMeasurementManager.registerTriggerAsync(TRIGGER_REGISTRATION_URI).get())
                .isNotNull();
    }

    @Test
    public void registerWebSource_NoErrors() throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion= */ 5,
                        /* minExtServicesVersionS= */ 9));

        WebSourceParams webSourceParams = new WebSourceParams(SOURCE_REGISTRATION_URI, false);

        WebSourceRegistrationRequest webSourceRegistrationRequest =
                new WebSourceRegistrationRequest(
                        Collections.singletonList(webSourceParams),
                        SOURCE_REGISTRATION_URI,
                        /* inputEvent= */ null,
                        OS_DESTINATION,
                        WEB_DESTINATION,
                        /* verifiedDestination= */ null);

        assertThat(mMeasurementManager.registerWebSourceAsync(webSourceRegistrationRequest).get())
                .isNotNull();
    }

    @Test
    public void registerWebTrigger_NoErrors() throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion= */ 5,
                        /* minExtServicesVersionS= */ 9));

        WebTriggerParams webTriggerParams = new WebTriggerParams(TRIGGER_REGISTRATION_URI, false);
        WebTriggerRegistrationRequest webTriggerRegistrationRequest =
                new WebTriggerRegistrationRequest(
                        Collections.singletonList(webTriggerParams), DESTINATION);

        assertThat(mMeasurementManager.registerWebTriggerAsync(webTriggerRegistrationRequest).get())
                .isNotNull();
    }

    @Test
    public void testDeleteRegistrations_withRequest_withNoRange_withCallback_NoErrors()
            throws Exception {
        // Skip the test if SDK extension 5 is not present.
        // This test should not run for back compat because it depends on adServices running in
        // system server
        Assume.assumeTrue(VersionCompatUtil.INSTANCE.isTPlusWithMinAdServicesVersion(5));

        DeletionRequest deletionRequest =
                new DeletionRequest.Builder(
                                DeletionRequest.DELETION_MODE_ALL,
                                DeletionRequest.MATCH_BEHAVIOR_DELETE)
                        .setDomainUris(Collections.singletonList(DOMAIN_URI))
                        .setOriginUris(Collections.singletonList(ORIGIN_URI))
                        .build();
        assertThat(mMeasurementManager.deleteRegistrationsAsync(deletionRequest).get()).isNotNull();
    }

    @Test
    public void testDeleteRegistrations_withRequest_withEmptyLists_withRange_withCallback_NoErrors()
            throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion= */ 5,
                        /* minExtServicesVersionS= */ 9));

        DeletionRequest deletionRequest =
                new DeletionRequest.Builder(
                                DeletionRequest.DELETION_MODE_ALL,
                                DeletionRequest.MATCH_BEHAVIOR_DELETE)
                        .setDomainUris(Collections.singletonList(DOMAIN_URI))
                        .setOriginUris(Collections.singletonList(ORIGIN_URI))
                        .setStart(Instant.ofEpochMilli(0))
                        .setEnd(Instant.now())
                        .build();
        assertThat(mMeasurementManager.deleteRegistrationsAsync(deletionRequest).get()).isNotNull();
    }

    @Test
    public void testDeleteRegistrations_withRequest_withInvalidArguments_withCallback_hasError()
            throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion= */ 5,
                        /* minExtServicesVersionS= */ 9));

        DeletionRequest deletionRequest =
                new DeletionRequest.Builder(
                                DeletionRequest.DELETION_MODE_ALL,
                                DeletionRequest.MATCH_BEHAVIOR_DELETE)
                        .setDomainUris(Collections.singletonList(DOMAIN_URI))
                        .setOriginUris(Collections.singletonList(ORIGIN_URI))
                        .setStart(Instant.now().plusMillis(1000))
                        .setEnd(Instant.now())
                        .build();
        Exception exception =
                assertThrows(
                        ExecutionException.class,
                        () -> mMeasurementManager.deleteRegistrationsAsync(deletionRequest).get());
        assertThat(exception).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testMeasurementApiStatus_returnResultStatus() throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion= */ 5,
                        /* minExtServicesVersionS= */ 9));

        int result = mMeasurementManager.getMeasurementApiStatusAsync().get();
        assertThat(result).isEqualTo(1);
    }
}
