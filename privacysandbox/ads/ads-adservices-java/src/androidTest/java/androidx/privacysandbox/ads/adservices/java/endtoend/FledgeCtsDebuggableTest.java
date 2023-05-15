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

package androidx.privacysandbox.ads.adservices.java.endtoend;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionManager;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome;
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest;
import androidx.privacysandbox.ads.adservices.common.AdData;
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals;
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier;
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience;
import androidx.privacysandbox.ads.adservices.customaudience.JoinCustomAudienceRequest;
import androidx.privacysandbox.ads.adservices.customaudience.TrustedBiddingData;
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo;
import androidx.privacysandbox.ads.adservices.java.adselection.AdSelectionManagerFutures;
import androidx.privacysandbox.ads.adservices.java.customaudience.CustomAudienceManagerFutures;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import kotlin.Unit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SdkSuppress(minSdkVersion = 26)
public class FledgeCtsDebuggableTest {
    protected static final Context sContext = ApplicationProvider.getApplicationContext();
    private static final String TAG = "FledgeCtsDebuggableTest";

    // Configuration constants
    private static final int SDK_MAX_REQUEST_PERMITS_PER_SECOND = 1;
    // sleep time to prevent hitting rate limit, with a small tolerance to prevent edge
    // case of max calls per second falling exactly within one second
    private static final long DEFAULT_API_RATE_LIMIT_SLEEP_MS =
            (long) (1000 / SDK_MAX_REQUEST_PERMITS_PER_SECOND) + 10L;
    private static final String DISABLE_MEASUREMENT_ENROLLMENT_CHECK = "1";

    // Time allowed by current test setup for APIs to respond
    private static final int API_RESPONSE_TIMEOUT_SECONDS = 5;

    // This is used to check actual API timeout conditions; note that the default overall timeout
    // for ad selection is 10 seconds
    private static final int API_RESPONSE_LONGER_TIMEOUT_SECONDS = 12;

    private static final AdTechIdentifier SELLER = new AdTechIdentifier("performance-fledge"
            + "-static-5jyy5ulagq-uc.a.run.app");

    private static final AdTechIdentifier BUYER_1 = new AdTechIdentifier("performance-fledge"
            + "-static-5jyy5ulagq-uc.a.run.app");
    private static final AdTechIdentifier BUYER_2 = new AdTechIdentifier("performance-fledge"
            + "-static-2-5jyy5ulagq-uc.a.run.app");

    private static final AdSelectionSignals AD_SELECTION_SIGNALS =
            new AdSelectionSignals("{\"ad_selection_signals\":1}");

    private static final AdSelectionSignals SELLER_SIGNALS =
            new AdSelectionSignals("{\"test_seller_signals\":1}");

    private static final Map<AdTechIdentifier, AdSelectionSignals> PER_BUYER_SIGNALS =
            new HashMap<>();
    static {
        PER_BUYER_SIGNALS.put(BUYER_1,
                new AdSelectionSignals("{\"buyer_signals\":1}"));
        PER_BUYER_SIGNALS.put(BUYER_2,
                new AdSelectionSignals("{\"buyer_signals\":2}"));
    }

    private static final String VALID_TRUSTED_BIDDING_URI_PATH = "/trusted/biddingsignals/simple";

    private static final ImmutableList<String> VALID_TRUSTED_BIDDING_KEYS =
            ImmutableList.of("key1", "key2");

    private static final AdSelectionSignals VALID_USER_BIDDING_SIGNALS =
            new AdSelectionSignals("{'valid': 'yep', 'opaque': 'definitely'}");

    private static final long FLEDGE_CUSTOM_AUDIENCE_DEFAULT_EXPIRE_IN_MS =
            60L * 24L * 60L * 60L * 1000L; // 60 days
    private static final long FLEDGE_CUSTOM_AUDIENCE_MAX_ACTIVATION_DELAY_IN_MS =
            60L * 24L * 60L * 60L * 1000L; // 60 days

    private static final long DAY_IN_SECONDS = 60 * 60 * 24;

    private static final String VALID_NAME = "testCustomAudienceName";

    private static final String AD_URI_PREFIX = "/adverts/123/";

    private static final String SELLER_DECISION_LOGIC_URI_PATH = "/seller/decision/simple_logic";
    private static final String BUYER_BIDDING_LOGIC_URI_PATH = "/buyer/bidding/simple_logic";
    private static final String SELLER_TRUSTED_SIGNAL_URI_PATH = "/trusted/scoringsignals/simple";
    // To mock malformed logic, use paths that return an empty response
    private static final String SELLER_MALFORMED_DECISION_LOGIC_URI_PATH = "/reporting/seller";
    private static final String BUYER_MALFORMED_BIDDING_LOGIC_URI_PATH = "/reporting/buyer";

    private static final AdSelectionConfig DEFAULT_AD_SELECTION_CONFIG = new AdSelectionConfig(
            SELLER,
            Uri.parse(
                    String.format(
                            "https://%s%s",
                            SELLER,
                            SELLER_DECISION_LOGIC_URI_PATH)),
            Arrays.asList(BUYER_1, BUYER_2),
            AD_SELECTION_SIGNALS,
            SELLER_SIGNALS,
            PER_BUYER_SIGNALS,
            Uri.parse(
                    String.format(
                            "https://%s%s",
                            SELLER,
                            SELLER_TRUSTED_SIGNAL_URI_PATH)));

    private AdSelectionClient mAdSelectionClient;
    private CustomAudienceClient mCustomAudienceClient;

    @BeforeClass
    public static void configure() {
        TestUtil testUtil = new TestUtil(InstrumentationRegistry.getInstrumentation(), TAG);

        testUtil.overrideAdIdKillSwitch(true);
        testUtil.overrideAppSetIdKillSwitch(true);
        testUtil.overrideKillSwitches(true);
        testUtil.overrideAllowlists(true);
        testUtil.overrideConsentManagerDebugMode(true);
        testUtil.overrideMeasurementKillSwitches(true);
        testUtil.overrideDisableMeasurementEnrollmentCheck(DISABLE_MEASUREMENT_ENROLLMENT_CHECK);
        testUtil.enableEnrollmentCheck(true);
        testUtil.overrideFledgeSelectAdsKillSwitch(true);
        testUtil.overrideFledgeCustomAudienceServiceKillSwitch(true);
        testUtil.overrideSdkRequestPermitsPerSecond(SDK_MAX_REQUEST_PERMITS_PER_SECOND);
        testUtil.disableDeviceConfigSyncForTests(true);
        testUtil.disableFledgeEnrollmentCheck(true);
        testUtil.enableAdServiceSystemService(true);
        testUtil.enforceFledgeJsIsolateMaxHeapSize(false);
    }

    @AfterClass
    public static void resetConfiguration() {
        TestUtil testUtil = new TestUtil(InstrumentationRegistry.getInstrumentation(), TAG);

        testUtil.overrideAdIdKillSwitch(false);
        testUtil.overrideAppSetIdKillSwitch(false);
        testUtil.overrideKillSwitches(false);
        testUtil.overrideAllowlists(false);
        testUtil.overrideConsentManagerDebugMode(false);
        testUtil.overrideMeasurementKillSwitches(false);
        testUtil.resetOverrideDisableMeasurementEnrollmentCheck();
        testUtil.enableEnrollmentCheck(false);
        testUtil.overrideFledgeSelectAdsKillSwitch(true);
        testUtil.overrideFledgeCustomAudienceServiceKillSwitch(true);
        testUtil.overrideSdkRequestPermitsPerSecond(1);
        testUtil.disableDeviceConfigSyncForTests(false);
        testUtil.disableFledgeEnrollmentCheck(false);
        testUtil.enableAdServiceSystemService(false);
        testUtil.enforceFledgeJsIsolateMaxHeapSize(true);
    }

    @Before
    public void setup() throws Exception {
        mAdSelectionClient =
                new AdSelectionClient(sContext);
        mCustomAudienceClient =
                new CustomAudienceClient(sContext);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);
    }

    @Test
    public void testFledgeAuctionSelectionFlow_overall_Success() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        CustomAudience customAudience2 = createCustomAudience(BUYER_2, bidsForBuyer2);
        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Running ad selection and asserting that the outcome is returned in < 10 seconds
        AdSelectionOutcome outcome =
                mAdSelectionClient
                        .selectAds(DEFAULT_AD_SELECTION_CONFIG)
                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that ad3 from BUYER_2 is rendered, since it had the highest bid and score
        Assert.assertEquals(
                getUri(BUYER_2, AD_URI_PREFIX + "/ad3"), outcome.getRenderUri());

        ReportImpressionRequest reportImpressionRequest =
                new ReportImpressionRequest(outcome.getAdSelectionId(),
                        DEFAULT_AD_SELECTION_CONFIG);

        // Performing reporting, and asserting that no exception is thrown
        mAdSelectionClient
                .reportImpression(reportImpressionRequest)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testAdSelection_etldViolation_failure() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        CustomAudience customAudience2 = createCustomAudience(BUYER_2, bidsForBuyer2);

        AdSelectionConfig adSelectionConfigWithEtldViolations =
                new AdSelectionConfig(
                        SELLER,
                        Uri.parse(
                                String.format(
                                        "https://%s%s",
                                        SELLER + "etld_noise",
                                        SELLER_DECISION_LOGIC_URI_PATH)),
                        Arrays.asList(BUYER_1, BUYER_2),
                        AD_SELECTION_SIGNALS,
                        SELLER_SIGNALS,
                        PER_BUYER_SIGNALS,
                        Uri.parse(
                                String.format(
                                        "https://%s%s",
                                        SELLER + "etld_noise",
                                        SELLER_TRUSTED_SIGNAL_URI_PATH)));

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Running ad selection and asserting that exception is thrown when decision and signals
        // URIs are not etld+1 compliant
        Exception selectAdsException =
                assertThrows(
                        ExecutionException.class,
                        () ->
                                mAdSelectionClient
                                        .selectAds(adSelectionConfigWithEtldViolations)
                                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertThat(selectAdsException.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testReportImpression_etldViolation_failure() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        CustomAudience customAudience2 = createCustomAudience(BUYER_2, bidsForBuyer2);

        AdSelectionConfig adSelectionConfigWithEtldViolations =
                new AdSelectionConfig(
                        SELLER,
                        Uri.parse(
                                String.format(
                                        "https://%s%s",
                                        SELLER + "etld_noise",
                                        SELLER_DECISION_LOGIC_URI_PATH)),
                        Arrays.asList(BUYER_1, BUYER_2),
                        AD_SELECTION_SIGNALS,
                        SELLER_SIGNALS,
                        PER_BUYER_SIGNALS,
                        Uri.parse(
                                String.format(
                                        "https://%s%s",
                                        SELLER + "etld_noise",
                                        SELLER_TRUSTED_SIGNAL_URI_PATH)));

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Running ad selection and asserting that the outcome is returned in < 10 seconds
        AdSelectionOutcome outcome =
                mAdSelectionClient
                        .selectAds(DEFAULT_AD_SELECTION_CONFIG)
                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that the ad3 from buyer 2 is rendered, since it had the highest bid and score
        Assert.assertEquals(
                getUri(BUYER_2, AD_URI_PREFIX + "/ad3"), outcome.getRenderUri());

        ReportImpressionRequest reportImpressionRequest =
                new ReportImpressionRequest(
                        outcome.getAdSelectionId(), adSelectionConfigWithEtldViolations);

        // Running report Impression and asserting that exception is thrown when decision and
        // signals URIs are not etld+1 compliant
        Exception selectAdsException =
                assertThrows(
                        ExecutionException.class,
                        () ->
                                mAdSelectionClient
                                        .reportImpression(reportImpressionRequest)
                                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertThat(selectAdsException.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testAdSelection_skipAdsMalformedBiddingLogic_success() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        CustomAudience customAudience2 = createCustomAudience(
                BUYER_2,
                bidsForBuyer2,
                getValidActivationTime(),
                getValidExpirationTime(),
                BUYER_MALFORMED_BIDDING_LOGIC_URI_PATH);


        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Running ad selection and asserting that the outcome is returned in < 10 seconds
        AdSelectionOutcome outcome =
                mAdSelectionClient
                        .selectAds(DEFAULT_AD_SELECTION_CONFIG)
                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that the ad3 from buyer 2 is skipped despite having the highest bid, since it has
        // malformed bidding logic
        // The winner should come from buyer1 with the highest bid i.e. ad2
        Assert.assertEquals(
                getUri(BUYER_1, AD_URI_PREFIX + "/ad2"), outcome.getRenderUri());

        ReportImpressionRequest reportImpressionRequest =
                new ReportImpressionRequest(outcome.getAdSelectionId(),
                        DEFAULT_AD_SELECTION_CONFIG);

        // Performing reporting, and asserting that no exception is thrown
        mAdSelectionClient
                .reportImpression(reportImpressionRequest)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testAdSelection_malformedScoringLogic_failure() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        CustomAudience customAudience2 = createCustomAudience(BUYER_2, bidsForBuyer2);

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Ad Selection will fail due to scoring logic malformed
        AdSelectionConfig adSelectionConfig = new AdSelectionConfig(
                SELLER,
                Uri.parse(
                        String.format(
                                "https://%s%s",
                                SELLER,
                                SELLER_MALFORMED_DECISION_LOGIC_URI_PATH)),
                Arrays.asList(BUYER_1, BUYER_2),
                AD_SELECTION_SIGNALS,
                SELLER_SIGNALS,
                PER_BUYER_SIGNALS,
                Uri.parse(
                        String.format(
                                "https://%s%s",
                                SELLER,
                                SELLER_TRUSTED_SIGNAL_URI_PATH)));

        Exception selectAdsException =
                assertThrows(
                        ExecutionException.class,
                        () ->
                                mAdSelectionClient
                                        .selectAds(adSelectionConfig)
                                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertThat(selectAdsException.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testAdSelection_skipAdsFailedGettingBiddingLogic_success() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        CustomAudience customAudience2 = createCustomAudience(
                BUYER_2,
                bidsForBuyer2,
                getValidActivationTime(),
                getValidExpirationTime(),
                "/invalid/bidding/logic/uri");

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Running ad selection and asserting that the outcome is returned in < 10 seconds
        AdSelectionOutcome outcome =
                mAdSelectionClient
                        .selectAds(DEFAULT_AD_SELECTION_CONFIG)
                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that the ad3 from buyer 2 is skipped despite having the highest bid, since it has
        // missing bidding logic
        // The winner should come from buyer1 with the highest bid i.e. ad2
        Assert.assertEquals(getUri(BUYER_1, AD_URI_PREFIX + "/ad2"), outcome.getRenderUri());

        ReportImpressionRequest reportImpressionRequest =
                new ReportImpressionRequest(outcome.getAdSelectionId(),
                        DEFAULT_AD_SELECTION_CONFIG);

        // Performing reporting, and asserting that no exception is thrown
        mAdSelectionClient
                .reportImpression(reportImpressionRequest)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testAdSelection_errorGettingScoringLogic_failure() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        CustomAudience customAudience2 = createCustomAudience(BUYER_2, bidsForBuyer2);

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Ad Selection will fail due to scoring logic not found, because the URI that is used to
        // fetch scoring logic does not exist
        AdSelectionConfig adSelectionConfig = new AdSelectionConfig(
                SELLER,
                Uri.parse(
                        String.format(
                                "https://%s%s",
                                SELLER,
                                "/invalid/seller/decision/logic/uri")),
                Arrays.asList(BUYER_1, BUYER_2),
                AD_SELECTION_SIGNALS,
                SELLER_SIGNALS,
                PER_BUYER_SIGNALS,
                Uri.parse(
                        String.format(
                                "https://%s%s",
                                SELLER,
                                SELLER_TRUSTED_SIGNAL_URI_PATH)));
        Exception selectAdsException =
                assertThrows(
                        ExecutionException.class,
                        () ->
                                mAdSelectionClient
                                        .selectAds(adSelectionConfig)
                                        .get(API_RESPONSE_LONGER_TIMEOUT_SECONDS,
                                                TimeUnit.SECONDS));
        // Sometimes a 400 status code is returned (ISE) instead of the network fetch timing out
        assertThat(
                selectAdsException.getCause() instanceof TimeoutException
                        || selectAdsException.getCause() instanceof IllegalStateException)
                .isTrue();
    }

    @Test
    public void testAdSelectionFlow_skipNonActivatedCA_Success() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        // CA 2 activated long in the future
        CustomAudience customAudience2 =
                createCustomAudience(
                        BUYER_2,
                        bidsForBuyer2,
                        getValidDelayedActivationTime(),
                        getValidDelayedExpirationTime(),
                        BUYER_BIDDING_LOGIC_URI_PATH);

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Running ad selection and asserting that the outcome is returned in < 10 seconds
        AdSelectionOutcome outcome =
                mAdSelectionClient
                        .selectAds(DEFAULT_AD_SELECTION_CONFIG)
                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that the ad3 from buyer 2 is skipped despite having the highest bid, since it is
        // not activated yet
        // The winner should come from buyer1 with the highest bid i.e. ad2
        Assert.assertEquals(
                getUri(BUYER_1, AD_URI_PREFIX + "/ad2"), outcome.getRenderUri());

        ReportImpressionRequest reportImpressionRequest =
                new ReportImpressionRequest(outcome.getAdSelectionId(),
                        DEFAULT_AD_SELECTION_CONFIG);

        // Performing reporting, and asserting that no exception is thrown
        mAdSelectionClient
                .reportImpression(reportImpressionRequest)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testAdSelectionFlow_skipExpiredCA_Success() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        int caTimeToExpireSeconds = 2;
        // Since we cannot create CA which is already expired, we create one which expires in few
        // seconds
        // We will then wait till this CA expires before we run Ad Selection
        CustomAudience customAudience2 =
                createCustomAudience(
                        BUYER_2,
                        bidsForBuyer2,
                        getValidActivationTime(),
                        Instant.now().plusSeconds(caTimeToExpireSeconds),
                        BUYER_BIDDING_LOGIC_URI_PATH);

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Wait to ensure that CA2 gets expired
        doSleep(caTimeToExpireSeconds * 2 * 1000);

        // Running ad selection and asserting that the outcome is returned in < 10 seconds
        AdSelectionOutcome outcome =
                mAdSelectionClient
                        .selectAds(DEFAULT_AD_SELECTION_CONFIG)
                        .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that the ad3 from buyer 2 is skipped despite having the highest bid, since it is
        // expired
        // The winner should come from buyer1 with the highest bid i.e. ad2
        Assert.assertEquals(
                getUri(BUYER_1, AD_URI_PREFIX + "/ad2"), outcome.getRenderUri());

        ReportImpressionRequest reportImpressionRequest =
                new ReportImpressionRequest(outcome.getAdSelectionId(),
                        DEFAULT_AD_SELECTION_CONFIG);

        // Performing reporting, and asserting that no exception is thrown
        mAdSelectionClient
                .reportImpression(reportImpressionRequest)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testAdSelectionFlow_skipCAsThatTimeoutDuringBidding_Success() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);
        CustomAudience customAudience2 = createCustomAudience(
                BUYER_2,
                bidsForBuyer2,
                getValidActivationTime(),
                getValidExpirationTime(),
                BUYER_BIDDING_LOGIC_URI_PATH + "?delay=" + 5000);

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception.
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Running ad selection and asserting that the outcome is returned in < 10 seconds
        AdSelectionOutcome outcome =
                mAdSelectionClient
                        .selectAds(DEFAULT_AD_SELECTION_CONFIG)
                        .get(API_RESPONSE_LONGER_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Assert that the ad3 from buyer 2 is skipped despite having the highest bid, since it
        // timed out
        // The winner should come from buyer1 with the highest bid i.e. ad2
        Assert.assertEquals(
                getUri(BUYER_1, AD_URI_PREFIX + "/ad2"), outcome.getRenderUri());

        ReportImpressionRequest reportImpressionRequest =
                new ReportImpressionRequest(outcome.getAdSelectionId(),
                        DEFAULT_AD_SELECTION_CONFIG);

        // Performing reporting, and asserting that no exception is thrown
        mAdSelectionClient
                .reportImpression(reportImpressionRequest)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testAdSelection_overallTimeout_Failure() throws Exception {
        // Skip the test if SDK extension 4 is not present.
        Assume.assumeTrue(AdServicesInfo.INSTANCE.version() >= 4);

        List<Double> bidsForBuyer1 = ImmutableList.of(1.1, 2.2);
        List<Double> bidsForBuyer2 = ImmutableList.of(4.5, 6.7, 10.0);

        CustomAudience customAudience1 = createCustomAudience(BUYER_1, bidsForBuyer1);

        CustomAudience customAudience2 = createCustomAudience(BUYER_2, bidsForBuyer2);

        // Joining custom audiences, no result to do assertion on. Failures will generate an
        // exception."
        mCustomAudienceClient
                .joinCustomAudience(customAudience1)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // TODO(b/266725238): Remove/modify once the API rate limit has been adjusted for FLEDGE
        doSleep(DEFAULT_API_RATE_LIMIT_SLEEP_MS);

        mCustomAudienceClient
                .joinCustomAudience(customAudience2)
                .get(API_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Running ad selection and asserting that the outcome is returned in < 10 seconds
        AdSelectionConfig adSelectionConfig = new AdSelectionConfig(
                SELLER,
                Uri.parse(
                        String.format(
                                "https://%s%s",
                                SELLER,
                                SELLER_DECISION_LOGIC_URI_PATH + "?delay=" + 10000)),
                Arrays.asList(BUYER_1, BUYER_2),
                AD_SELECTION_SIGNALS,
                SELLER_SIGNALS,
                PER_BUYER_SIGNALS,
                Uri.parse(
                        String.format(
                                "https://%s%s",
                                SELLER,
                                SELLER_TRUSTED_SIGNAL_URI_PATH)));
        Exception selectAdsException =
                assertThrows(
                        ExecutionException.class,
                        () ->
                                mAdSelectionClient
                                        .selectAds(adSelectionConfig)
                                        .get(API_RESPONSE_LONGER_TIMEOUT_SECONDS,
                                                TimeUnit.SECONDS));
        assertThat(selectAdsException.getCause()).isInstanceOf(TimeoutException.class);
    }

    @SuppressLint("BanThreadSleep")
    private static void doSleep(long timeout) {
        Log.i(TAG, String.format("Starting to sleep for %d ms", timeout));
        long currentTime = System.currentTimeMillis();
        long wakeupTime = currentTime + timeout;
        while (wakeupTime - currentTime > 0) {
            Log.i(TAG, String.format("Time left to sleep: %d ms", wakeupTime - currentTime));
            try {
                Thread.sleep(wakeupTime - currentTime);
            } catch (InterruptedException ignored) {

            }
            currentTime = System.currentTimeMillis();
        }
        Log.i(TAG, "Done sleeping");
    }

    private static Uri getUri(String authority, String path) {
        return Uri.parse("https://" + authority + path);
    }

    private static Uri getUri(AdTechIdentifier authority, String path) {
        return getUri(authority.toString(), path);
    }

    private static Uri getValidDailyUpdateUriByBuyer(AdTechIdentifier buyer) {
        return getUri(buyer, "/update");
    }

    private static Uri getValidTrustedBiddingUriByBuyer(AdTechIdentifier buyer) {
        return getUri(buyer, VALID_TRUSTED_BIDDING_URI_PATH);
    }

    private static List<String> getValidTrustedBiddingKeys() {
        return new ArrayList<>(VALID_TRUSTED_BIDDING_KEYS);
    }

    private static TrustedBiddingData getValidTrustedBiddingDataByBuyer(AdTechIdentifier buyer) {
        return new TrustedBiddingData(
                getValidTrustedBiddingUriByBuyer(buyer),
                getValidTrustedBiddingKeys());
    }

    @RequiresApi(26)
    private Instant getValidDelayedActivationTime() {
        Duration maxActivationDelayIn =
                Duration.ofMillis(FLEDGE_CUSTOM_AUDIENCE_MAX_ACTIVATION_DELAY_IN_MS);

        return Instant.now()
                .truncatedTo(ChronoUnit.MILLIS)
                .plus(maxActivationDelayIn.dividedBy(2));
    }

    @RequiresApi(26)
    private Instant getValidDelayedExpirationTime() {
        return getValidDelayedActivationTime().plusSeconds(DAY_IN_SECONDS);
    }

    @RequiresApi(26)
    private Instant getValidActivationTime() {
        return Instant.now()
                .truncatedTo(ChronoUnit.MILLIS);
    }

    @RequiresApi(26)
    private Instant getValidExpirationTime() {
        return getValidActivationTime()
                .plus(Duration.ofMillis(FLEDGE_CUSTOM_AUDIENCE_DEFAULT_EXPIRE_IN_MS));
    }


    /**
     * @param buyer The name of the buyer for this Custom Audience
     * @param bids these bids, are added to its metadata. Our JS logic then picks this value and
     *     creates ad with the provided value as bid
     * @return a real Custom Audience object that can be persisted and used in bidding and scoring
     */
    private CustomAudience createCustomAudience(final AdTechIdentifier buyer, List<Double> bids) {
        return createCustomAudience(
                buyer,
                bids,
                getValidActivationTime(),
                getValidExpirationTime(),
                BUYER_BIDDING_LOGIC_URI_PATH);
    }

    private CustomAudience createCustomAudience(
            final AdTechIdentifier buyer,
            List<Double> bids,
            Instant activationTime,
            Instant expirationTime,
            String biddingLogicUri) {
        // Generate ads for with bids provided
        List<AdData> ads = new ArrayList<>();

        // Create ads with the buyer name and bid number as the ad URI
        // Add the bid value to the metadata
        for (int i = 0; i < bids.size(); i++) {
            ads.add(
                    new AdData(getUri(buyer, AD_URI_PREFIX + "/ad" + (i + 1)),
                            "{\"bid\":" + bids.get(i) + "}"));
        }

        return new CustomAudience.Builder(
                buyer,
                buyer + VALID_NAME,
                getValidDailyUpdateUriByBuyer(buyer),
                getUri(buyer, biddingLogicUri),
                ads)
                .setActivationTime(activationTime)
                .setExpirationTime(expirationTime)
                .setUserBiddingSignals(VALID_USER_BIDDING_SIGNALS)
                .setTrustedBiddingData(
                        getValidTrustedBiddingDataByBuyer(buyer))
                .build();
    }

    private static class CustomAudienceClient {
        private final CustomAudienceManagerFutures mCustomAudienceManager;

        CustomAudienceClient(Context context) {
            mCustomAudienceManager = CustomAudienceManagerFutures.from(context);
        }

        public ListenableFuture<Unit> joinCustomAudience(CustomAudience customAudience) {
            JoinCustomAudienceRequest request = new JoinCustomAudienceRequest(customAudience);
            return mCustomAudienceManager
                    .joinCustomAudienceAsync(request);
        }
    }

    private static class AdSelectionClient {

        private final AdSelectionManagerFutures mAdSelectionManager;

        AdSelectionClient(Context context) {
            mAdSelectionManager = AdSelectionManagerFutures.from(context);
        }

        /**
         *  Invokes the {@code selectAds} method of {@link AdSelectionManager} and
         *  returns a future with {@link AdSelectionOutcome}
         */
        public ListenableFuture<AdSelectionOutcome> selectAds(AdSelectionConfig adSelectionConfig)
                throws Exception {
            return mAdSelectionManager.selectAdsAsync(adSelectionConfig);
        }

        /**
         * Invokes the {@code reportImpression} method of {@link AdSelectionManager} and returns
         * a future with Unit
         */
        public ListenableFuture<Unit> reportImpression(ReportImpressionRequest input) {
            return mAdSelectionManager.reportImpressionAsync(input);
        }
    }
}
