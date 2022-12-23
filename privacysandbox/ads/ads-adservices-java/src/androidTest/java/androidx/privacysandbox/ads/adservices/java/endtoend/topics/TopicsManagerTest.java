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

package androidx.privacysandbox.ads.adservices.java.endtoend.topics;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo;
import androidx.privacysandbox.ads.adservices.java.topics.TopicsManagerFutures;
import androidx.privacysandbox.ads.adservices.topics.GetTopicsRequest;
import androidx.privacysandbox.ads.adservices.topics.GetTopicsResponse;
import androidx.privacysandbox.ads.adservices.topics.Topic;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
// TODO: Consider refactoring so that we're not duplicating code.
public class TopicsManagerTest {
    private Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private static final String TAG = "TopicsManagerTest";
    // The JobId of the Epoch Computation.
    private static final int EPOCH_JOB_ID = 2;

    // Override the Epoch Job Period to this value to speed up the epoch computation.
    private static final long TEST_EPOCH_JOB_PERIOD_MS = 3000;

    // Default Epoch Period.
    private static final long TOPICS_EPOCH_JOB_PERIOD_MS = 7 * 86_400_000; // 7 days.

    // Use 0 percent for random topic in the test so that we can verify the returned topic.
    private static final int TEST_TOPICS_PERCENTAGE_FOR_RANDOM_TOPIC = 0;
    private static final int TOPICS_PERCENTAGE_FOR_RANDOM_TOPIC = 5;

    // Used to get the package name. Copied over from com.android.adservices.AdServicesCommon
    private static final String TOPICS_SERVICE_NAME = "android.adservices.TOPICS_SERVICE";
    private static final String ADSERVICES_PACKAGE_NAME = getAdServicesPackageName();

    @Before
    public void setup() throws Exception {
        overrideKillSwitches(true);
        // We need to skip 3 epochs so that if there is any usage from other test runs, it will
        // not be used for epoch retrieval.
        Thread.sleep(3 * TEST_EPOCH_JOB_PERIOD_MS);

        overrideEpochPeriod(TEST_EPOCH_JOB_PERIOD_MS);
        // We need to turn off random topic so that we can verify the returned topic.
        overridePercentageForRandomTopic(TEST_TOPICS_PERCENTAGE_FOR_RANDOM_TOPIC);
        // TODO: Remove this override.
        enableEnrollmentCheck(true);
    }

    @After
    public void teardown() {
        overrideKillSwitches(false);
        overrideEpochPeriod(TOPICS_EPOCH_JOB_PERIOD_MS);
        overridePercentageForRandomTopic(TOPICS_PERCENTAGE_FOR_RANDOM_TOPIC);
        enableEnrollmentCheck(false);
    }

    @Test
    public void testTopicsManager_runClassifier() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        TopicsManagerFutures topicsManager =
                TopicsManagerFutures.from(ApplicationProvider.getApplicationContext());
        GetTopicsRequest request = new GetTopicsRequest.Builder()
                .setSdkName("sdk1")
                .setShouldRecordObservation(true)
                .build();
        GetTopicsResponse response = topicsManager.getTopicsAsync(request).get();

        // At beginning, Sdk1 receives no topic.
        assertThat(response.getTopics().isEmpty());

        // Now force the Epoch Computation Job. This should be done in the same epoch for
        // callersCanLearnMap to have the entry for processing.
        forceEpochComputationJob();

        // Wait to the next epoch. We will not need to do this after we implement the fix in
        // go/rb-topics-epoch-scheduling
        Thread.sleep(TEST_EPOCH_JOB_PERIOD_MS);

        // Since the sdk1 called the Topics API in the previous Epoch, it should receive some topic.
        response = topicsManager.getTopicsAsync(request).get();
        assertThat(response.getTopics()).isNotEmpty();

        // Top 5 classifications for empty string with v2 model are [10230, 10253, 10227, 10250,
        // 10257]. This is computed by running the model on the device for empty string.
        // These 5 classification topics will become top 5 topics of the epoch since there is
        // no other apps calling Topics API.
        // The app will be assigned one random topic from one of these 5 topics.
        assertThat(response.getTopics()).hasSize(1);

        Topic topic = response.getTopics().get(0);

        // topic is one of the 5 classification topics of the Test App.
        assertThat(topic.getTopicId()).isIn(Arrays.asList(10230, 10253, 10227, 10250, 10257));

        assertThat(topic.getModelVersion()).isAtLeast(1L);
        assertThat(topic.getTaxonomyVersion()).isAtLeast(1L);

        // Sdk 2 did not call getTopics API. So it should not receive any topic.
        GetTopicsResponse response2 = topicsManager.getTopicsAsync(
                new GetTopicsRequest.Builder()
                        .setSdkName("sdk2")
                        .build()).get();
        assertThat(response2.getTopics()).isEmpty();
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
            runShellCommand("setprop debug.adservices.global_kill_switch " + false);
            runShellCommand("setprop debug.adservices.topics_kill_switch " + false);
        } else {
            runShellCommand("setprop debug.adservices.global_kill_switch " + null);
            runShellCommand("setprop debug.adservices.topics_kill_switch " + null);
        }
    }

    private void enableEnrollmentCheck(boolean enable) {
        runShellCommand(
                "setprop debug.adservices.disable_topics_enrollment_check " + enable);
    }

    // Override the Epoch Period to shorten the Epoch Length in the test.
    private void overrideEpochPeriod(long overrideEpochPeriod) {
        runShellCommand(
                "setprop debug.adservices.topics_epoch_job_period_ms " + overrideEpochPeriod);
    }

    // Override the Percentage For Random Topic in the test.
    private void overridePercentageForRandomTopic(long overridePercentage) {
        runShellCommand(
                "setprop debug.adservices.topics_percentage_for_random_topics "
                        + overridePercentage);
    }

    /** Forces JobScheduler to run the Epoch Computation job */
    private void forceEpochComputationJob() {
        runShellCommand(
                "cmd jobscheduler run -f" + " " + ADSERVICES_PACKAGE_NAME + " " + EPOCH_JOB_ID);
    }

    @SuppressWarnings("deprecation")
    // Used to get the package name. Copied over from com.android.adservices.AndroidServiceBinder
    private static String getAdServicesPackageName() {
        final Intent intent = new Intent(TOPICS_SERVICE_NAME);
        final List<ResolveInfo> resolveInfos = ApplicationProvider.getApplicationContext()
                        .getPackageManager()
                        .queryIntentServices(intent, PackageManager.MATCH_SYSTEM_ONLY);

        if (resolveInfos == null || resolveInfos.isEmpty()) {
            Log.e(
                    TAG,
                    "Failed to find resolveInfo for adServices service. Intent action: "
                            + TOPICS_SERVICE_NAME);
            return null;
        }

        if (resolveInfos.size() > 1) {
            Log.e(
                    TAG,
                    String.format(
                            "Found multiple services (%1$s) for the same intent action (%2$s)",
                            TOPICS_SERVICE_NAME, resolveInfos));
            return null;
        }

        final ServiceInfo serviceInfo = resolveInfos.get(0).serviceInfo;
        if (serviceInfo == null) {
            Log.e(TAG, "Failed to find serviceInfo for adServices service");
            return null;
        }

        return serviceInfo.packageName;
    }
}
