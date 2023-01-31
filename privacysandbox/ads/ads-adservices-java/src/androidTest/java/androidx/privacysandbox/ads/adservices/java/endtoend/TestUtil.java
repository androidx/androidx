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

public class TestUtil {
    private Instrumentation mInstrumentation;
    private String mTag;
    // Used to get the package name. Copied over from com.android.adservices.AdServicesCommon
    private static final String TOPICS_SERVICE_NAME = "android.adservices.TOPICS_SERVICE";
    // The JobId of the Epoch Computation.
    private static final int EPOCH_JOB_ID = 2;

    public TestUtil(Instrumentation instrumentation, String tag) {
        mInstrumentation = instrumentation;
        mTag = tag;
    }
    // Run shell command.
    private void runShellCommand(String command) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mInstrumentation.getUiAutomation().executeShellCommand(command);
            }
        }
    }
    public void overrideKillSwitches(boolean override) {
        if (override) {
            runShellCommand("setprop debug.adservices.global_kill_switch " + false);
            runShellCommand("setprop debug.adservices.topics_kill_switch " + false);
        } else {
            runShellCommand("setprop debug.adservices.global_kill_switch " + null);
            runShellCommand("setprop debug.adservices.topics_kill_switch " + null);
        }
    }

    public void enableEnrollmentCheck(boolean enable) {
        runShellCommand(
                "setprop debug.adservices.disable_topics_enrollment_check " + enable);
    }

    // Override the Epoch Period to shorten the Epoch Length in the test.
    public void overrideEpochPeriod(long overrideEpochPeriod) {
        runShellCommand(
                "setprop debug.adservices.topics_epoch_job_period_ms " + overrideEpochPeriod);
    }

    // Override the Percentage For Random Topic in the test.
    public void overridePercentageForRandomTopic(long overridePercentage) {
        runShellCommand(
                "setprop debug.adservices.topics_percentage_for_random_topics "
                        + overridePercentage);
    }

    /** Forces JobScheduler to run the Epoch Computation job */
    public void forceEpochComputationJob() {
        runShellCommand(
                "cmd jobscheduler run -f" + " " + getAdServicesPackageName() + " " + EPOCH_JOB_ID);
    }

    public void overrideConsentManagerDebugMode(boolean override) {
        String overrideStr = override ? "true" : "null";
        runShellCommand("setprop debug.adservices.consent_manager_debug_mode " + overrideStr);
    }

    public void overrideAllowlists(boolean override) {
        String overrideStr = override ? "*" : "null";
        runShellCommand("device_config put adservices ppapi_app_allow_list " + overrideStr);
        runShellCommand("device_config put adservices ppapi_app_signature_allow_list "
                + overrideStr);
        runShellCommand(
                "device_config put adservices web_context_client_allow_list " + overrideStr);
    }

    public void overrideAdIdKillSwitch(boolean override) {
        if (override) {
            runShellCommand("setprop debug.adservices.adid_kill_switch " + false);
        } else {
            runShellCommand("setprop debug.adservices.adid_kill_switch " + null);
        }
    }

    // Override measurement related kill switch to ignore the effect of actual PH values.
    // If isOverride = true, override measurement related kill switch to OFF to allow adservices
    // If isOverride = false, override measurement related kill switch to meaningless value so that
    // PhFlags will use the default value.
    public void overrideMeasurementKillSwitches(boolean isOverride) {
        String overrideString = isOverride ? "false" : "null";
        runShellCommand("setprop debug.adservices.global_kill_switch " + overrideString);
        runShellCommand("setprop debug.adservices.measurement_kill_switch " + overrideString);
        runShellCommand("setprop debug.adservices.measurement_api_register_source_kill_switch "
                + overrideString);
        runShellCommand("setprop debug.adservices.measurement_api_register_trigger_kill_switch "
                + overrideString);
        runShellCommand("setprop debug.adservices.measurement_api_register_web_source_kill_switch "
                + overrideString);
        runShellCommand("setprop debug.adservices.measurement_api_register_web_trigger_kill_switch "
                + overrideString);
        runShellCommand("setprop debug.adservices.measurement_api_delete_registrations_kill_switch "
                + overrideString);
        runShellCommand("setprop debug.adservices.measurement_api_status_kill_switch "
                + overrideString);
    }

    // Override the flag to disable Measurement enrollment check. Setting to 1 disables enforcement.
    public void overrideDisableMeasurementEnrollmentCheck(String val) {
        runShellCommand("setprop debug.adservices.disable_measurement_enrollment_check " + val);
    }

    public void resetOverrideDisableMeasurementEnrollmentCheck() {
        runShellCommand("setprop debug.adservices.disable_measurement_enrollment_check null");
    }

    @SuppressWarnings("deprecation")
    // Used to get the package name. Copied over from com.android.adservices.AndroidServiceBinder
    public String getAdServicesPackageName() {
        final Intent intent = new Intent(TOPICS_SERVICE_NAME);
        final List<ResolveInfo> resolveInfos = ApplicationProvider.getApplicationContext()
                .getPackageManager()
                .queryIntentServices(intent, PackageManager.MATCH_SYSTEM_ONLY);

        if (resolveInfos == null || resolveInfos.isEmpty()) {
            Log.e(mTag, "Failed to find resolveInfo for adServices service. Intent action: "
                            + TOPICS_SERVICE_NAME);
            return null;
        }

        if (resolveInfos.size() > 1) {
            String str = String.format(
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
