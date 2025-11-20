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

package androidx.privacysandbox.ads.adservices.java.endtoend;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import java.util.List;
import java.util.stream.Collectors;

public class TestUtil {
    private Instrumentation mInstrumentation;
    private String mTag;
    // Used to get the package name. Copied over from com.android.adservices.AdServicesCommon
    private static final String TOPICS_SERVICE_NAME = "android.adservices.TOPICS_SERVICE";
    private static final String EXT_SERVICES_PACKAGE_NAME = "ext.adservices";
    // The JobId of the Epoch Computation.
    private static final int EPOCH_JOB_ID = 2;

    public TestUtil(Instrumentation instrumentation, String tag) {
        mInstrumentation = instrumentation;
        mTag = tag;
    }

    // Run shell command.
    private void runShellCommand(String command) {
        mInstrumentation.getUiAutomation().executeShellCommand(command);
    }

    public void overrideKillSwitches(boolean override) {
        if (override) {
            runShellCommand("device_config put adservices global_kill_switch " + false);
            runShellCommand("device_config put adservices topics_kill_switch " + false);
        } else {
            runShellCommand("device_config put adservices global_kill_switch " + null);
            runShellCommand("device_config put adservices topics_kill_switch " + null);
        }
    }

    public void enableEnrollmentCheck(boolean enable) {
        runShellCommand("device_config put adservices disable_topics_enrollment_check " + enable);
    }

    // Override the Epoch Period to shorten the Epoch Length in the test.
    public void overrideEpochPeriod(long overrideEpochPeriod) {
        runShellCommand(
                "device_config put adservices topics_epoch_job_period_ms " + overrideEpochPeriod);
    }

    // Override the Percentage For Random Topic in the test.
    public void overridePercentageForRandomTopic(long overridePercentage) {
        runShellCommand(
                "device_config put adservices topics_percentage_for_random_topics "
                        + overridePercentage);
    }

    /** Forces JobScheduler to run the Epoch Computation job */
    public void forceEpochComputationJob() {
        runShellCommand(
                "cmd jobscheduler run -f " + getAdServicesPackageName() + " " + EPOCH_JOB_ID);
    }

    public void overrideConsentManagerDebugMode(boolean override) {
        String overrideStr = override ? "true" : "null";
        // This flag is only read through system property and not DeviceConfig
        runShellCommand("setprop debug.adservices.consent_manager_debug_mode " + overrideStr);
    }

    public void overrideConsentNotificationDebugMode(boolean override) {
        String overrideStr = override ? "true" : "null";
        // This flag is only read through system property and not DeviceConfig
        runShellCommand("setprop debug.adservices.consent_notification_debug_mode " + overrideStr);
    }

    public void overrideAllowlists(boolean override) {
        String overrideStr = override ? "*" : "null";
        runShellCommand("device_config put adservices ppapi_app_allow_list " + overrideStr);
        runShellCommand("device_config put adservices msmt_api_app_allow_list " + overrideStr);
        runShellCommand(
                "device_config put adservices ppapi_app_signature_allow_list " + overrideStr);
        runShellCommand(
                "device_config put adservices web_context_client_allow_list " + overrideStr);
    }

    public void overrideAdIdKillSwitch(boolean override) {
        if (override) {
            runShellCommand("device_config put adservices adid_kill_switch " + false);
        } else {
            runShellCommand("device_config put adservices adid_kill_switch " + null);
        }
    }

    public void overrideAppSetIdKillSwitch(boolean override) {
        if (override) {
            runShellCommand("device_config put adservices appsetid_kill_switch " + false);
        } else {
            runShellCommand("device_config put adservices appsetid_kill_switch " + null);
        }
    }

    public void enableBackCompatOnS() {
        runShellCommand("device_config put adservices enable_back_compat true");
        runShellCommand("device_config put adservices consent_source_of_truth 3");
        runShellCommand("device_config put adservices blocked_topics_source_of_truth 3");
    }

    public void disableBackCompatOnS() {
        runShellCommand("device_config put adservices enable_back_compat false");
        runShellCommand("device_config put adservices consent_source_of_truth null");
        runShellCommand("device_config put adservices blocked_topics_source_of_truth null");
    }

    // Override measurement related kill switch to ignore the effect of actual PH values.
    // If isOverride = true, override measurement related kill switch to OFF to allow adservices
    // If isOverride = false, override measurement related kill switch to meaningless value so that
    // PhFlags will use the default value.
    public void overrideMeasurementKillSwitches(boolean isOverride) {
        String overrideString = isOverride ? "false" : "null";
        runShellCommand("device_config put adservices global_kill_switch " + overrideString);
        runShellCommand("device_config put adservices measurement_kill_switch " + overrideString);
        runShellCommand(
                "device_config put adservices measurement_api_register_source_kill_switch "
                        + overrideString);
        runShellCommand(
                "device_config put adservices measurement_api_register_trigger_kill_switch "
                        + overrideString);
        runShellCommand(
                "device_config put adservices measurement_api_register_web_source_kill_switch "
                        + overrideString);
        runShellCommand(
                "device_config put adservices measurement_api_register_web_trigger_kill_switch "
                        + overrideString);
        runShellCommand(
                "device_config put adservices measurement_api_delete_registrations_kill_switch "
                        + overrideString);
        runShellCommand(
                "device_config put adservices measurement_api_status_kill_switch "
                        + overrideString);
    }

    // Override the flag to disable Measurement enrollment check. Setting to 1 disables enforcement.
    public void overrideDisableMeasurementEnrollmentCheck(String val) {
        runShellCommand("device_config put adservices disable_measurement_enrollment_check " + val);
    }

    public void resetOverrideDisableMeasurementEnrollmentCheck() {
        runShellCommand("device_config put adservices disable_measurement_enrollment_check null");
    }

    // Force using bundled files instead of using MDD downloaded files. This helps to make the test
    // results deterministic.
    public void shouldForceUseBundledFiles(boolean shouldUse) {
        if (shouldUse) {
            runShellCommand("device_config put adservices classifier_force_use_bundled_files true");
        } else {
            runShellCommand("device_config delete adservices classifier_force_use_bundled_files");
        }
    }

    public void enableVerboseLogging() {
        runShellCommand("setprop log.tag.adservices VERBOSE");
        runShellCommand("setprop log.tag.adservices.adid VERBOSE");
        runShellCommand("setprop log.tag.adservices.appsetid VERBOSE");
        runShellCommand("setprop log.tag.adservices.topics VERBOSE");
        runShellCommand("setprop log.tag.adservices.fledge VERBOSE");
        runShellCommand("setprop log.tag.adservices.measurement VERBOSE");
    }

    public void overrideFledgeSelectAdsKillSwitch(boolean override) {
        if (override) {
            runShellCommand("device_config put adservices fledge_select_ads_kill_switch " + false);
        } else {
            runShellCommand("device_config put adservices fledge_select_ads_kill_switch " + null);
        }
    }

    public void overrideFledgeCustomAudienceServiceKillSwitch(boolean override) {
        if (override) {
            runShellCommand(
                    "device_config put adservices fledge_custom_audience_service_kill_switch "
                            + false);
        } else {
            runShellCommand(
                    "device_config put adservices fledge_custom_audience_service_kill_switch "
                            + null);
        }
    }

    public void overrideSdkRequestPermitsPerSecond(long maxRequests) {
        runShellCommand(
                "device_config put adservices sdk_request_permits_per_second " + maxRequests);
    }

    public void disableDeviceConfigSyncForTests(boolean disabled) {
        if (disabled) {
            runShellCommand("device_config set_sync_disabled_for_tests persistent");
        } else {
            runShellCommand("device_config set_sync_disabled_for_tests none");
        }
    }

    public void disableFledgeEnrollmentCheck(boolean disabled) {
        if (disabled) {
            runShellCommand("device_config put adservices disable_fledge_enrollment_check true");
        } else {
            runShellCommand("device_config put adservices disable_fledge_enrollment_check false");
        }
    }

    public void enableAdServiceSystemService(boolean enabled) {
        if (enabled) {
            runShellCommand(
                    "device_config put adservices adservice_system_service_enabled \"true\"");
        } else {
            runShellCommand(
                    "device_config put adservices adservice_system_service_enabled \"false\"");
        }
    }

    public void enforceFledgeJsIsolateMaxHeapSize(boolean enforce) {
        if (enforce) {
            runShellCommand(
                    "device_config put adservices fledge_js_isolate_enforce_max_heap_size"
                            + " true");
        } else {
            runShellCommand(
                    "device_config put adservices fledge_js_isolate_enforce_max_heap_size"
                            + " false");
        }
    }

    @SuppressWarnings("deprecation")
    // Used to get the package name. Copied over from com.android.adservices.AndroidServiceBinder
    public String getAdServicesPackageName() {
        final Intent intent = new Intent(TOPICS_SERVICE_NAME);
        List<ResolveInfo> resolveInfos =
                ApplicationProvider.getApplicationContext()
                        .getPackageManager()
                        .queryIntentServices(intent, PackageManager.MATCH_SYSTEM_ONLY);

        // TODO: b/271866693 avoid hardcoding package names
        if (resolveInfos != null && Build.VERSION.SDK_INT >= 33) {
            resolveInfos =
                    resolveInfos.stream()
                            .filter(
                                    info ->
                                            !info.serviceInfo.packageName.contains(
                                                    EXT_SERVICES_PACKAGE_NAME))
                            .collect(Collectors.toList());
        }

        if (resolveInfos == null || resolveInfos.isEmpty()) {
            Log.e(
                    mTag,
                    "Failed to find resolveInfo for adServices service. Intent action: "
                            + TOPICS_SERVICE_NAME);
            return null;
        }

        if (resolveInfos.size() > 1) {
            String str =
                    String.format(
                            "Found multiple services (%1$s) for the same intent action (%2$s)",
                            TOPICS_SERVICE_NAME, resolveInfos);
            Log.e(mTag, str);
            return null;
        }

        final ServiceInfo serviceInfo = resolveInfos.get(0).serviceInfo;
        if (serviceInfo == null) {
            Log.e(mTag, "Failed to find serviceInfo for adServices service");
            return null;
        }

        return serviceInfo.packageName;
    }
}
