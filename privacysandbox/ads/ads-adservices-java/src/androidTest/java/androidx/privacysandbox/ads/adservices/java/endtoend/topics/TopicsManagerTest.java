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

import androidx.privacysandbox.ads.adservices.java.VersionCompatUtil;
import androidx.privacysandbox.ads.adservices.java.endtoend.TestUtil;
import androidx.privacysandbox.ads.adservices.java.topics.TopicsManagerFutures;
import androidx.privacysandbox.ads.adservices.topics.GetTopicsRequest;
import androidx.privacysandbox.ads.adservices.topics.GetTopicsResponse;
import androidx.privacysandbox.ads.adservices.topics.Topic;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
@SdkSuppress(minSdkVersion = 28) // API 28 required for device_config used by this test
// TODO: Consider refactoring so that we're not duplicating code.
public class TopicsManagerTest {
    private static final String TAG = "TopicsManagerTest";
    TestUtil mTestUtil = new TestUtil(InstrumentationRegistry.getInstrumentation(), TAG);

    // Override the Epoch Job Period to this value to speed up the epoch computation.
    private static final long TEST_EPOCH_JOB_PERIOD_MS = 3000;

    // Default Epoch Period.
    private static final long TOPICS_EPOCH_JOB_PERIOD_MS = 7 * 86_400_000; // 7 days.

    // Use 0 percent for random topic in the test so that we can verify the returned topic.
    private static final int TEST_TOPICS_PERCENTAGE_FOR_RANDOM_TOPIC = 0;
    private static final int TOPICS_PERCENTAGE_FOR_RANDOM_TOPIC = 5;

    @Before
    public void setup() throws Exception {
        mTestUtil.overrideKillSwitches(true);
        // We need to skip 3 epochs so that if there is any usage from other test runs, it will
        // not be used for epoch retrieval.
        Thread.sleep(3 * TEST_EPOCH_JOB_PERIOD_MS);

        mTestUtil.overrideEpochPeriod(TEST_EPOCH_JOB_PERIOD_MS);
        // We need to turn off random topic so that we can verify the returned topic.
        mTestUtil.overridePercentageForRandomTopic(TEST_TOPICS_PERCENTAGE_FOR_RANDOM_TOPIC);
        mTestUtil.overrideConsentManagerDebugMode(true);
        mTestUtil.overrideAllowlists(true);
        // TODO: Remove this override.
        mTestUtil.enableEnrollmentCheck(true);
        // Force to use bundled files to make test result deterministic.
        mTestUtil.shouldForceUseBundledFiles(true);
        // Enable verbose logging.
        mTestUtil.enableVerboseLogging();

        if (VersionCompatUtil.INSTANCE.isSWithMinExtServicesVersion(9)) {
            mTestUtil.enableBackCompat();
        }
    }

    @After
    public void teardown() {
        mTestUtil.overrideKillSwitches(false);
        mTestUtil.overrideEpochPeriod(TOPICS_EPOCH_JOB_PERIOD_MS);
        mTestUtil.overridePercentageForRandomTopic(TOPICS_PERCENTAGE_FOR_RANDOM_TOPIC);
        mTestUtil.overrideConsentManagerDebugMode(false);
        mTestUtil.overrideAllowlists(false);
        mTestUtil.enableEnrollmentCheck(false);
        mTestUtil.shouldForceUseBundledFiles(false);
        if (VersionCompatUtil.INSTANCE.isSWithMinExtServicesVersion(9)) {
            mTestUtil.disableBackCompat();
        }
    }

    @Ignore // b/278931615
    @Test
    public void testTopicsManager_runClassifier() throws Exception {
        // Skip the test if the right SDK extension is not present.
        Assume.assumeTrue(
                VersionCompatUtil.INSTANCE.isTestableVersion(
                        /* minAdServicesVersion=*/ 4,
                        /* minExtServicesVersion=*/ 9));

        TopicsManagerFutures topicsManager =
                TopicsManagerFutures.from(ApplicationProvider.getApplicationContext());
        GetTopicsRequest request = new GetTopicsRequest.Builder()
                .setAdsSdkName("sdk1")
                .setShouldRecordObservation(true)
                .build();
        GetTopicsResponse response = topicsManager.getTopicsAsync(request).get();

        // At beginning, Sdk1 receives no topic.
        assertThat(response.getTopics()).isEmpty();

        // Now force the Epoch Computation Job. This should be done in the same epoch for
        // callersCanLearnMap to have the entry for processing.
        mTestUtil.forceEpochComputationJob();

        // Wait to the next epoch. We will not need to do this after we implement the fix in
        // go/rb-topics-epoch-scheduling
        Thread.sleep(TEST_EPOCH_JOB_PERIOD_MS);

        // Since the sdk1 called the Topics API in the previous Epoch, it should receive some topic.
        response = topicsManager.getTopicsAsync(request).get();
        assertThat(response.getTopics()).isNotEmpty();

        // Top 5 classifications for empty string with v2 model are [10147, 10253, 10175, 10254,
        // 10333]. This is computed by running the model on the device for empty string.
        // These 5 classification topics will become top 5 topics of the epoch since there is
        // no other apps calling Topics API.
        // The app will be assigned one random topic from one of these 5 topics.
        assertThat(response.getTopics()).hasSize(1);

        Topic topic = response.getTopics().get(0);

        // topic is one of the 5 classification topics of the Test App.
        assertThat(topic.getTopicId()).isIn(Arrays.asList(10147, 10253, 10175, 10254, 10333));

        assertThat(topic.getModelVersion()).isAtLeast(1L);
        assertThat(topic.getTaxonomyVersion()).isAtLeast(1L);

        // Sdk 2 did not call getTopics API. So it should not receive any topic.
        GetTopicsResponse response2 = topicsManager.getTopicsAsync(
                new GetTopicsRequest.Builder()
                        .setAdsSdkName("sdk2")
                        .build()).get();
        assertThat(response2.getTopics()).isEmpty();
    }
}
