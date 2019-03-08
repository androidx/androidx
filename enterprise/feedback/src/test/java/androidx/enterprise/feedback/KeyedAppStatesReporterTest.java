/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.enterprise.feedback;

import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATES;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATES_ACTION;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_DATA;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_KEY;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_MESSAGE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_SEVERITY;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.PHONESKY_PACKAGE_NAME;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.WHAT_IMMEDIATE_STATE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.WHAT_STATE;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.fail;

import static org.robolectric.Shadows.shadowOf;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;

/** Tests {@link KeyedAppStatesReporter}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = 21)
public class KeyedAppStatesReporterTest {

    private final ComponentName mTestComponentName = new ComponentName("test_package", "");

    private final Executor mExecutor = new TestExecutor();

    private final ContextWrapper mContext = ApplicationProvider.getApplicationContext();
    private final DevicePolicyManager mDevicePolicyManager =
            (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
    private final PackageManager mPackageManager = mContext.getPackageManager();
    private final Application mApplication = (Application) mContext.getApplicationContext();

    private final TestHandler mTestHandler = new TestHandler();

    private final KeyedAppState mState =
            KeyedAppState.builder().setKey("key").setSeverity(KeyedAppState.SEVERITY_INFO).build();

    @Before
    public void setUp() {
        // Reset the singleton so tests are independent
        KeyedAppStatesReporter.resetSingleton();
    }

    @Test
    @SmallTest
    public void getInstance_nullContext_throwsNullPointerException() {
        KeyedAppStatesReporter.resetSingleton();
        try {
            KeyedAppStatesReporter.getInstance(null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    @SmallTest
    public void initialize_usesExecutor() {
        KeyedAppStatesReporter.resetSingleton();
        TestExecutor testExecutor = new TestExecutor();
        KeyedAppStatesReporter.initialize(mContext, testExecutor);

        KeyedAppStatesReporter.getInstance(mContext).set(singleton(mState));

        assertThat(testExecutor.lastExecuted()).isNotNull();
    }

    @Test
    @SmallTest
    public void initialize_calledMultipleTimes_throwsIllegalStateException() {
        KeyedAppStatesReporter.resetSingleton();
        KeyedAppStatesReporter.initialize(mContext, mExecutor);

        try {
            KeyedAppStatesReporter.initialize(mContext, mExecutor);
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    @SmallTest
    public void initialize_calledAfterGetInstance_throwsIllegalStateException() {
        KeyedAppStatesReporter.resetSingleton();
        KeyedAppStatesReporter.getInstance(mContext);

        try {
            KeyedAppStatesReporter.initialize(mContext, mExecutor);
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    @SmallTest
    public void setIncludesAppStateBundle() {
        setTestHandlerReceivesStates();

        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));

        Bundle appStatesBundle = buildStatesBundle(singleton(mState));
        assertAppStateBundlesEqual(appStatesBundle, (Bundle) mTestHandler.latestMessage().obj);
    }

    private static Bundle buildStatesBundle(Collection<KeyedAppState> keyedAppStates) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(APP_STATES, buildStateBundles(keyedAppStates));
        return bundle;
    }

    private static ArrayList<Bundle> buildStateBundles(Collection<KeyedAppState> keyedAppStates) {
        ArrayList<Bundle> bundles = new ArrayList<>();
        for (KeyedAppState keyedAppState : keyedAppStates) {
            bundles.add(keyedAppState.toStateBundle());
        }
        return bundles;
    }

    private static void assertAppStateBundlesEqual(Bundle expected, Bundle actual) {
        ArrayList<Bundle> expectedAppStatesBundles = expected.getParcelableArrayList(APP_STATES);
        ArrayList<Bundle> actualAppStatesBundles = actual.getParcelableArrayList(APP_STATES);

        assertThat(actualAppStatesBundles).hasSize(expectedAppStatesBundles.size());

        for (int i = 0; i < expectedAppStatesBundles.size(); i++) {
            assertAppStateBundleEqual(expectedAppStatesBundles.get(i),
                    actualAppStatesBundles.get(i));
        }
    }

    private static void assertAppStateBundleEqual(Bundle expected, Bundle actual) {
        assertThat(actual.getString(APP_STATE_KEY)).isEqualTo(expected.getString(APP_STATE_KEY));
        assertThat(actual.getString(APP_STATE_SEVERITY))
                .isEqualTo(expected.getString(APP_STATE_SEVERITY));
        assertThat(actual.getString(APP_STATE_MESSAGE))
                .isEqualTo(expected.getString(APP_STATE_MESSAGE));
        assertThat(actual.getString(APP_STATE_DATA)).isEqualTo(expected.getString(APP_STATE_DATA));
    }

    @Test
    @SmallTest
    public void setEmpty_doesNotSend() {
        setTestHandlerReceivesStates();

        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(Collections.<KeyedAppState>emptyList());

        assertThat(mTestHandler.latestMessage()).isNull();
    }

    @Test
    @SmallTest
    public void setNotImmediate() {
        setTestHandlerReceivesStates();

        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));

        assertThat(mTestHandler.latestMessage().what).isEqualTo(WHAT_STATE);
    }

    @Test
    @SmallTest
    public void setImmediate() {
        setTestHandlerReceivesStates();

        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.setImmediate(singletonList(mState));

        assertThat(mTestHandler.latestMessage().what).isEqualTo(WHAT_IMMEDIATE_STATE);
    }

    @Test
    @SmallTest
    public void set_doesNotGoToNormalApps() {
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);

        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));

        assertThat(mTestHandler.latestMessage()).isNull();
    }

    @Test
    @SmallTest
    public void set_goesToDeviceOwner() {
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setDeviceOwner(mTestComponentName);

        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));

        assertThat(mTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    public void set_goesToProfileOwner() {
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(mTestComponentName);

        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));

        assertThat(mTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    public void set_goesToPhonesky() {
        ComponentName phoneskyComponentName = new ComponentName(PHONESKY_PACKAGE_NAME, "");
        addComponentAsRespondingToAppStatesIntent(phoneskyComponentName);
        setComponentBindingToHandler(phoneskyComponentName, mTestHandler);

        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));

        assertThat(mTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    public void set_goesToMultiple() {
        // Arrange
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(mTestComponentName);

        ComponentName phoneskyComponentName = new ComponentName(PHONESKY_PACKAGE_NAME, "");
        TestHandler phoneskyTestHandler = new TestHandler();
        addComponentAsRespondingToAppStatesIntent(phoneskyComponentName);
        setComponentBindingToHandler(phoneskyComponentName, phoneskyTestHandler);

        // Act
        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));

        // Assert
        assertThat(mTestHandler.latestMessage()).isNotNull();
        assertThat(phoneskyTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    public void set_changeProfileOwner_goesToNewProfileOwner() {
        // Arrange
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(mTestComponentName);
        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));
        mTestHandler.reset();

        ComponentName newComponentName = new ComponentName("second_test_package", "");
        TestHandler newTestHandler = new TestHandler();
        addComponentAsRespondingToAppStatesIntent(newComponentName);
        setComponentBindingToHandler(newComponentName, newTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(newComponentName);

        // Act
        reporter.set(singletonList(mState));

        // Assert
        assertThat(mTestHandler.latestMessage()).isNull();
        assertThat(newTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    public void set_changeDeviceOwner_goesToNewDeviceOwner() {
        // Arrange
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setDeviceOwner(mTestComponentName);
        KeyedAppStatesReporter reporter = getReporter(mContext);
        reporter.set(singletonList(mState));
        mTestHandler.reset();

        ComponentName newComponentName = new ComponentName("second_test_package", "");
        TestHandler newTestHandler = new TestHandler();
        addComponentAsRespondingToAppStatesIntent(newComponentName);
        setComponentBindingToHandler(newComponentName, newTestHandler);
        shadowOf(mDevicePolicyManager).setDeviceOwner(newComponentName);

        // Act
        reporter.set(singletonList(mState));

        // Assert
        assertThat(mTestHandler.latestMessage()).isNull();
        assertThat(newTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    @Config(minSdk = 26)
    public void set_deadConnection_reconnectsAndSendsToNewApp() {
        // Arrange
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(mTestComponentName);

        KeyedAppStatesReporter reporter = getReporter(mContext);

        // Set the binding to a different handler - as if the app has restarted.
        TestHandler newAppTestHandler = new TestHandler();
        setComponentBindingToHandler(mTestComponentName, newAppTestHandler);

        simulateDeadServiceConnection();

        // Act
        reporter.set(singletonList(mState));

        // Assert
        assertThat(mTestHandler.latestMessage()).isNull();
        assertThat(newAppTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    @Config(maxSdk = 25)
    public void set_connectionHasDisconnected_sdkLessThan26_reconnectsAndSendsToNewApp() {
        // Arrange
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(mTestComponentName);

        KeyedAppStatesReporter reporter = getReporter(mContext);

        // Set the binding to a different handler - as if the app has restarted.
        TestHandler newAppTestHandler = new TestHandler();
        setComponentBindingToHandler(mTestComponentName, newAppTestHandler);

        simulateDisconnectingServiceConnection();

        // Act
        reporter.set(singletonList(mState));

        // Assert
        assertThat(mTestHandler.latestMessage()).isNull();
        assertThat(newAppTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    @Config(minSdk = 26)
    public void set_connectionHasDisconnected_doesNotSend() {
        // Arrange
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(mTestComponentName);

        KeyedAppStatesReporter reporter = getReporter(mContext);

        simulateDisconnectingServiceConnection();

        // Act
        reporter.set(singletonList(mState));

        // Assert
        assertThat(mTestHandler.latestMessage()).isNull();
    }

    @Test
    @SmallTest
    @Config(minSdk = 26)
    public void set_sendsWhenReconnected() {
        // Arrange
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(mTestComponentName);

        KeyedAppStatesReporter reporter = getReporter(mContext);

        simulateDisconnectingServiceConnection();
        reporter.set(singletonList(mState));

        // Act
        simulateReconnectingServiceConnection();

        // Assert
        assertThat(mTestHandler.latestMessage()).isNotNull();
    }

    @Test
    @SmallTest
    public void set_connectionHasReconnected_doesSend() {
        // Arrange
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setProfileOwner(mTestComponentName);

        KeyedAppStatesReporter reporter = getReporter(mContext);

        // Change the component binding to ensure that it doesn't reconnect
        setComponentBindingToHandler(mTestComponentName, new TestHandler());

        simulateDisconnectingServiceConnection();
        simulateReconnectingServiceConnection();

        // Act
        reporter.set(singletonList(mState));

        // Assert
        assertThat(mTestHandler.latestMessage()).isNotNull();
    }

    private void setTestHandlerReceivesStates() {
        addComponentAsRespondingToAppStatesIntent(mTestComponentName);
        setComponentBindingToHandler(mTestComponentName, mTestHandler);
        shadowOf(mDevicePolicyManager).setDeviceOwner(mTestComponentName);
    }

    private void addComponentAsRespondingToAppStatesIntent(ComponentName componentName) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = componentName.getPackageName();
        resolveInfo.serviceInfo.name = componentName.getClassName();

        Intent appStatesIntent = new Intent(APP_STATES_ACTION);

        shadowOf(mPackageManager).addResolveInfoForIntent(appStatesIntent, resolveInfo);
    }

    private void setComponentBindingToHandler(ComponentName componentName, Handler handler) {
        Intent intent = new Intent();
        intent.setComponent(componentName);

        IBinder service = new Messenger(handler).getBinder();
        shadowOf(mApplication)
                .setComponentNameAndServiceForBindServiceForIntent(intent, componentName, service);
    }

    private void simulateDeadServiceConnection() {
        getServiceConnection().onBindingDied(mTestComponentName);
    }

    private void simulateDisconnectingServiceConnection() {
        getServiceConnection().onServiceDisconnected(mTestComponentName);
    }

    private void simulateReconnectingServiceConnection() {
        IBinder service = new Messenger(mTestHandler).getBinder();
        getServiceConnection().onServiceConnected(mTestComponentName, service);
    }

    private ServiceConnection getServiceConnection() {
        return shadowOf((Application) mContext).getBoundServiceConnections().get(0);
    }

    private KeyedAppStatesReporter getReporter(Context context) {
        KeyedAppStatesReporter.initialize(context, mExecutor);
        return KeyedAppStatesReporter.getInstance(context);
    }
}
