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
package androidx.mediarouter.app;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.robolectric.Shadows.shadowOf;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.AudioDeviceInfoBuilder;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowAudioManager;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.List;

@RunWith(Enclosed.class)
public class SystemOutputSwitcherDialogControllerTest {
    private static final String PACKAGE_NAME_SYSTEM_UI = "com.android.systemui";
    private static final String OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_R =
            "com.android.settings.panel.action.MEDIA_OUTPUT";
    private static final String OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_S =
            "com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG";

    private enum SystemIntentResolverType {
        ACTIVITY,
        BROADCAST_RECEIVER
    }

    private static void registerSystemUiIntentResolver(
            @NonNull ShadowPackageManager shadowPackageManager,
            @NonNull String action, @NonNull SystemIntentResolverType resolverType)
            throws PackageManager.NameNotFoundException {
        ComponentName systemUiComponentName =
                new ComponentName(PACKAGE_NAME_SYSTEM_UI, "SystemUIDummyActivity");

        ApplicationInfo systemAppInfo = new ApplicationInfo();
        systemAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;

        ActivityInfo systemUiActivityInfo = new ActivityInfo();
        systemUiActivityInfo.applicationInfo = systemAppInfo;
        systemUiActivityInfo.name = systemUiComponentName.getClassName();
        systemUiActivityInfo.packageName = systemUiComponentName.getPackageName();

        switch (resolverType) {
            case ACTIVITY:
                shadowPackageManager.addOrUpdateActivity(systemUiActivityInfo);
                shadowPackageManager.addIntentFilterForActivity(systemUiComponentName,
                        new IntentFilter(action));
                break;
            case BROADCAST_RECEIVER:
                shadowPackageManager.addOrUpdateReceiver(systemUiActivityInfo);
                shadowPackageManager.addIntentFilterForReceiver(systemUiComponentName,
                        new IntentFilter(action));
                break;
        }
    }

    private static void setupConnectedAudioOutput(int... deviceTypes) {
        ShadowAudioManager shadowAudioManager = shadowOf(
                ApplicationProvider.getApplicationContext().getSystemService(AudioManager.class));
        ImmutableList.Builder<AudioDeviceInfo> deviceListBuilder = ImmutableList.builder();
        for (int deviceType : deviceTypes) {
            deviceListBuilder.add(AudioDeviceInfoBuilder.newBuilder().setType(deviceType).build());
        }
        shadowAudioManager.setOutputDevices(deviceListBuilder.build());
    }

    private static void assertAndroidROrBelowOutputSwitcherIntent(
            @NonNull Intent intent, @NonNull Context context) {
        assertThat(intent.getAction()).isEqualTo(OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_R);
        assertThat(intent.getStringExtra("com.android.settings.panel.extra.PACKAGE_NAME"))
                .isEqualTo(context.getPackageName());
    }

    private static void assertAndroidSOrAboveOutputSwitcherIntent(
            @NonNull Intent intent, @NonNull Context context) {
        assertThat(intent.getAction())
                .isEqualTo(OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_S);
        assertThat(intent.getPackage()).isEqualTo(PACKAGE_NAME_SYSTEM_UI);
        assertThat(intent.getStringExtra("package_name")).isEqualTo(context.getPackageName());
    }

    private static void assertWearOsBluetoothSettingsIntentWithCloseOnConnectFlag(
            @NonNull Intent intent) {
        assertThat(intent.getAction())
                .isEqualTo(Settings.ACTION_BLUETOOTH_SETTINGS);
        assertThat(intent.getBooleanExtra("EXTRA_CONNECTION_ONLY", false)).isTrue();
        assertThat(intent.getBooleanExtra("EXTRA_CLOSE_ON_CONNECT", false)).isTrue();
        assertThat(intent.getIntExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0))
                .isEqualTo(1);
    }

    private static void assertWearOsBluetoothSettingsIntentWithoutCloseOnConnectFlag(
            @NonNull Intent intent) {
        assertThat(intent.getAction())
                .isEqualTo(Settings.ACTION_BLUETOOTH_SETTINGS);
        assertThat(intent.getBooleanExtra("EXTRA_CONNECTION_ONLY", false)).isTrue();
        assertThat(intent.getBooleanExtra("EXTRA_CLOSE_ON_CONNECT", false)).isFalse();
        assertThat(intent.getIntExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0))
                .isEqualTo(1);
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = Build.VERSION_CODES.R)
    public static class SystemOutputSwitcherDialogControllerAndroidRTest {
        private Context mContext;
        private ShadowPackageManager mShadowPackageManager;
        private ShadowApplication mShadowApplication;

        @Before
        public void setUp() {
            Application application = ApplicationProvider.getApplicationContext();
            this.mContext = application;
            this.mShadowApplication = Shadows.shadowOf(application);
            mShadowPackageManager = Shadows.shadowOf(application.getPackageManager());
        }

        @Test
        public void testThatDoesNotShowDialogWhenThereAreNoActivitiesToResolveTheIntent() {
            assertThat(SystemOutputSwitcherDialogController.showDialog(mContext)).isFalse();

            assertThat(mShadowApplication.getNextStartedActivity()).isNull();
            // check that no API S code was called
            assertThat(mShadowApplication.getBroadcastIntents()).isEmpty();
        }

        @Test
        public void testThatShowsDialogWhenThereIsAnActivityResolvingTheIntent() {
            try {
                registerSystemUiIntentResolver(mShadowPackageManager,
                        OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_R,
                        SystemIntentResolverType.ACTIVITY);
            } catch (PackageManager.NameNotFoundException e) {
                assertWithMessage("Cannot register System UI Activity").fail();
            }

            boolean retVal = SystemOutputSwitcherDialogController.showDialog(mContext);

            assertThat(retVal).isTrue();
            Intent intent = mShadowApplication.getNextStartedActivity();
            assertAndroidROrBelowOutputSwitcherIntent(intent, mContext);
            // check that no API S code was called
            assertThat(mShadowApplication.getBroadcastIntents()).isEmpty();
        }

        @SuppressLint("NewApi")
        @Test
        public void testThatShowsBluetoothSettingsDialogWithoutBtDeviceConnectedOnWearOs() {
            try {
                registerSystemUiIntentResolver(mShadowPackageManager,
                        Settings.ACTION_BLUETOOTH_SETTINGS,
                        SystemIntentResolverType.ACTIVITY);
            } catch (PackageManager.NameNotFoundException e) {
                assertWithMessage("Cannot register Settings Bluetooth").fail();
            }
            // enable wear os
            mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_WATCH, true);

            boolean retVal = SystemOutputSwitcherDialogController.showDialog(mContext);

            assertThat(retVal).isTrue();
            Intent intent = mShadowApplication.getNextStartedActivity();
            assertWearOsBluetoothSettingsIntentWithCloseOnConnectFlag(intent);
            // check that no API S code was called
            assertThat(mShadowApplication.getBroadcastIntents()).isEmpty();
        }

        @SuppressLint("NewApi")
        @Test
        public void testThatShowsBluetoothSettingsDialogWithBtDeviceConnectedOnWearOs() {
            try {
                registerSystemUiIntentResolver(mShadowPackageManager,
                        Settings.ACTION_BLUETOOTH_SETTINGS,
                        SystemIntentResolverType.ACTIVITY);
            } catch (PackageManager.NameNotFoundException e) {
                assertWithMessage("Cannot register Settings Bluetooth").fail();
            }
            // enable wear os
            mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_WATCH, true);
            setupConnectedAudioOutput(
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);

            boolean retVal = SystemOutputSwitcherDialogController.showDialog(mContext);

            assertThat(retVal).isTrue();
            Intent intent = mShadowApplication.getNextStartedActivity();
            assertWearOsBluetoothSettingsIntentWithoutCloseOnConnectFlag(intent);
            // check that no API S code was called
            assertThat(mShadowApplication.getBroadcastIntents()).isEmpty();
        }
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = Build.VERSION_CODES.S)
    public static class SystemOutputSwitcherDialogControllerAndroidSTest {
        private Context mContext;
        private ShadowPackageManager mShadowPackageManager;
        private ShadowApplication mShadowApplication;

        @Before
        public void setUp() {
            Application application = ApplicationProvider.getApplicationContext();
            this.mContext = application;
            this.mShadowApplication = Shadows.shadowOf(application);
            mShadowPackageManager = Shadows.shadowOf(application.getPackageManager());
        }

        @Test
        public void testThatDoesNotShowDialogWhenThereAreNoActivitiesToResolveTheIntent() {
            assertThat(SystemOutputSwitcherDialogController.showDialog(mContext)).isFalse();
            assertThat(mShadowApplication.getBroadcastIntents()).isEmpty();
            // check that no API R code was called
            assertThat(mShadowApplication.getNextStartedActivity()).isNull();
        }

        @Test
        public void testThatFallbacksToAndroidRApiWhenThereIsAnActivityButNoBroadcast() {
            try {
                registerSystemUiIntentResolver(mShadowPackageManager,
                        OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_R,
                        SystemIntentResolverType.ACTIVITY);
            } catch (PackageManager.NameNotFoundException e) {
                assertWithMessage("Cannot register System UI Activity").fail();
            }

            boolean retVal = SystemOutputSwitcherDialogController.showDialog(mContext);

            assertThat(retVal).isTrue();
            // check that there are no receivers to resolve the intent
            assertThat(mShadowApplication.getBroadcastIntents()).isEmpty();
            Intent intent = mShadowApplication.getNextStartedActivity();
            assertAndroidROrBelowOutputSwitcherIntent(intent, mContext);
        }

        @Test
        public void testThatShowsDialogFromSWhenThereIsABroadcastAndNoActivity() {
            try {
                registerSystemUiIntentResolver(mShadowPackageManager,
                        OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_S,
                        SystemIntentResolverType.BROADCAST_RECEIVER);
            } catch (PackageManager.NameNotFoundException e) {
                assertWithMessage("Cannot register System UI Broadcast receiver")
                        .fail();
            }

            boolean retVal = SystemOutputSwitcherDialogController.showDialog(mContext);

            assertThat(retVal).isTrue();
            List<Intent> broadcastIntents = mShadowApplication.getBroadcastIntents();
            assertThat(broadcastIntents).isNotEmpty();
            assertThat(broadcastIntents).hasSize(1);
            Intent intent = broadcastIntents.get(0);
            assertAndroidSOrAboveOutputSwitcherIntent(intent, mContext);
            // check that no API R code was called
            assertThat(mShadowApplication.getNextStartedActivity()).isNull();
        }

        @Test
        public void testThatShowsDialogFromSWhenThereIsABroadcastAndAnActivity() {
            try {
                registerSystemUiIntentResolver(mShadowPackageManager,
                        OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_R,
                        SystemIntentResolverType.ACTIVITY);
                registerSystemUiIntentResolver(mShadowPackageManager,
                        OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_S,
                        SystemIntentResolverType.BROADCAST_RECEIVER);
            } catch (PackageManager.NameNotFoundException e) {
                assertWithMessage("Cannot register either Activity or Broadcast")
                        .fail();
            }

            boolean retVal = SystemOutputSwitcherDialogController.showDialog(mContext);

            assertThat(retVal).isTrue();
            List<Intent> broadcastIntents = mShadowApplication.getBroadcastIntents();
            assertThat(broadcastIntents).isNotEmpty();
            assertThat(broadcastIntents).hasSize(1);
            Intent intent = broadcastIntents.get(0);
            assertAndroidSOrAboveOutputSwitcherIntent(intent, mContext);
            // check that no API R code was called
            assertThat(mShadowApplication.getNextStartedActivity()).isNull();
        }

        @Test
        public void testThatShowsBluetoothSettingsDialogWithoutBtDeviceConnectedOnWearOs() {
            try {
                registerSystemUiIntentResolver(mShadowPackageManager,
                        Settings.ACTION_BLUETOOTH_SETTINGS,
                        SystemIntentResolverType.ACTIVITY);
            } catch (PackageManager.NameNotFoundException e) {
                assertWithMessage("Cannot register Settings Bluetooth").fail();
            }
            // enable wear os
            mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_WATCH, true);

            boolean retVal = SystemOutputSwitcherDialogController.showDialog(mContext);

            assertThat(retVal).isTrue();
            Intent intent = mShadowApplication.getNextStartedActivity();
            assertWearOsBluetoothSettingsIntentWithCloseOnConnectFlag(intent);
            // check that no API S code was as there is no receiver to resolve broadcast
            assertThat(mShadowApplication.getBroadcastIntents()).isEmpty();
        }

        @SuppressLint("NewApi")
        @Test
        public void testThatShowsBluetoothSettingsDialogWithBtDeviceConnectedOnWearOs() {
            try {
                registerSystemUiIntentResolver(mShadowPackageManager,
                        Settings.ACTION_BLUETOOTH_SETTINGS,
                        SystemIntentResolverType.ACTIVITY);
            } catch (PackageManager.NameNotFoundException e) {
                assertWithMessage("Cannot register Settings Bluetooth").fail();
            }
            // enable wear os
            mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_WATCH, true);
            setupConnectedAudioOutput(
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);

            boolean retVal = SystemOutputSwitcherDialogController.showDialog(mContext);

            assertThat(retVal).isTrue();
            Intent intent = mShadowApplication.getNextStartedActivity();
            assertWearOsBluetoothSettingsIntentWithoutCloseOnConnectFlag(intent);
            // check that no API S code was called
            assertThat(mShadowApplication.getBroadcastIntents()).isEmpty();
        }
    }
}
