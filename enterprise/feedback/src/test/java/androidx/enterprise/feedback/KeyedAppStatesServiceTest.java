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

import static android.os.Looper.getMainLooper;

import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATES;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_KEY;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_SEVERITY;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.WHAT_IMMEDIATE_STATE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.WHAT_STATE;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowBinder;
import org.robolectric.shadows.ShadowPausedAsyncTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/** Tests {@link KeyedAppStatesService}. */
@SuppressWarnings("UnstableApiUsage") // PausedExecutorService and ShadowPausedAsyncTask are @Beta
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = 21)
public class KeyedAppStatesServiceTest {

    private static class TestKeyedAppStatesService extends KeyedAppStatesService {
        Collection<ReceivedKeyedAppState> mStates;
        boolean mRequestSync;

        @Override
        public void onReceive(
                @NonNull Collection<ReceivedKeyedAppState> states, boolean requestSync) {
            this.mStates = Collections.unmodifiableCollection(states);
            this.mRequestSync = requestSync;
        }
    }

    private static final KeyedAppState STATE =
            KeyedAppState.builder()
                    .setKey("key1")
                    .setMessage("message1")
                    .setSeverity(KeyedAppState.SEVERITY_INFO)
                    .setData("data1")
                    .build();

    private static final KeyedAppState STATE2 =
            KeyedAppState.builder()
                    .setKey("key2")
                    .setMessage("message2")
                    .setSeverity(KeyedAppState.SEVERITY_INFO)
                    .setData("data2")
                    .build();

    private final TestKeyedAppStatesService mKeyedAppStatesService =
            Robolectric.setupService(TestKeyedAppStatesService.class);

    private final IBinder mBinder = mKeyedAppStatesService.onBind(new Intent());
    private final Messenger mMessenger = new Messenger(mBinder);

    private final ContextWrapper mContext = ApplicationProvider.getApplicationContext();
    private final PackageManager mPackageManager = mContext.getPackageManager();

    private static PausedExecutorService sAsyncTaskExecutor;

    private static final int DEFAULT_SENDING_UID = -1;

    private static final long CURRENT_TIME_MILLIS = 1234567;

    @BeforeClass
    public static void setUpClass() {
        sAsyncTaskExecutor = new PausedExecutorService();
    }

    @AfterClass
    public static void tearDownClass() {
        sAsyncTaskExecutor.shutdown();
    }

    @Before
    public void setUp() {
        shadowOf(mPackageManager).setNameForUid(DEFAULT_SENDING_UID, "test_package");
        ShadowBinder.setCallingUid(DEFAULT_SENDING_UID);
        ShadowPausedAsyncTask.overrideExecutor(sAsyncTaskExecutor);
    }

    @Test
    public void receivesStates() throws RemoteException {
        Collection<KeyedAppState> keyedAppStates = asList(STATE, STATE2);
        Bundle appStatesBundle = buildStatesBundle(keyedAppStates);
        Message message = createStateMessage(appStatesBundle);

        mMessenger.send(message);
        idleKeyedAppStatesService();

        assertReceivedStatesMatch(mKeyedAppStatesService.mStates, asList(STATE, STATE2));
    }

    private void assertReceivedStatesMatch(
            Collection<ReceivedKeyedAppState> receivedStates, Collection<KeyedAppState> states) {
        Collection<KeyedAppState> convertedReceivedStates =
                convertReceivedStatesToKeyedAppState(receivedStates);
        assertThat(convertedReceivedStates).containsExactlyElementsIn(states);
    }

    private Collection<KeyedAppState> convertReceivedStatesToKeyedAppState(
            Collection<ReceivedKeyedAppState> receivedStates) {
        Collection<KeyedAppState> states = new ArrayList<>();
        for (ReceivedKeyedAppState receivedState : receivedStates) {
            states.add(
                    KeyedAppState.builder()
                            .setKey(receivedState.getKey())
                            .setSeverity(receivedState.getSeverity())
                            .setMessage(receivedState.getMessage())
                            .setData(receivedState.getData())
                            .build());
        }
        return states;
    }

    @Test
    public void receivesTimestamp() throws RemoteException {
        SystemClock.setCurrentTimeMillis(CURRENT_TIME_MILLIS);

        mMessenger.send(createTestStateMessage());
        idleKeyedAppStatesService();

        ReceivedKeyedAppState receivedState = mKeyedAppStatesService.mStates.iterator().next();
        long timestamp = receivedState.getTimestamp();
        assertThat(timestamp).isEqualTo(CURRENT_TIME_MILLIS);
    }

    @Test
    public void receivesPackageName() throws RemoteException {
        final String packageName = "test.package.name";
        shadowOf(mPackageManager).setNameForUid(DEFAULT_SENDING_UID, packageName);

        mMessenger.send(createTestStateMessage());
        idleKeyedAppStatesService();

        ReceivedKeyedAppState receivedState = mKeyedAppStatesService.mStates.iterator().next();
        assertThat(receivedState.getPackageName()).isEqualTo(packageName);
    }

    @Test
    public void receivesDoesNotRequestSync() throws RemoteException {
        mMessenger.send(createTestStateMessage());
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mRequestSync).isFalse();
    }

    @Test
    public void receivesRequestSync() throws RemoteException {
        Message message = createStateMessageImmediate(buildStatesBundle(singleton(STATE)));

        mMessenger.send(message);
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mRequestSync).isTrue();
    }

    @Test
    public void deduplicatesStates() throws RemoteException {
        // Arrange
        Collection<KeyedAppState> keyedAppStates =
                asList(
                        KeyedAppState.builder().setKey("key").setSeverity(
                                KeyedAppState.SEVERITY_INFO).build(),
                        KeyedAppState.builder().setKey("key").setSeverity(
                                KeyedAppState.SEVERITY_INFO).build(),
                        KeyedAppState.builder()
                                .setKey("key")
                                .setSeverity(KeyedAppState.SEVERITY_INFO)
                                .setMessage("message")
                                .build());

        Bundle appStatesBundle = buildStatesBundle(keyedAppStates);
        Message message = createStateMessage(appStatesBundle);

        // Act
        mMessenger.send(message);
        idleKeyedAppStatesService();

        // Assert
        assertThat(mKeyedAppStatesService.mStates).hasSize(1);
    }

    @Test
    public void send_emptyStates_doesNotCallback() throws RemoteException {
        Bundle appStatesBundle = buildStatesBundle(Collections.emptyList());
        Message message = createStateMessage(appStatesBundle);

        mMessenger.send(message);
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mStates).isNull();
    }

    @Test
    public void send_messageWithoutWhat_doesNotCallback() throws RemoteException {
        Message message = Message.obtain();

        mMessenger.send(message);
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mStates).isNull();
    }

    @Test
    public void send_messageWithoutBundle_doesNotCallback() throws RemoteException {
        mMessenger.send(createStateMessage(null));
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mStates).isNull();
    }

    @Test
    public void send_messageWithIncorrectObj_doesNotCallback() throws RemoteException {
        Message message = createStateMessage(null);
        message.obj = "";

        mMessenger.send(message);
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mStates).isNull();
    }

    @Test
    public void send_messageWithEmptyBundle_doesNotCallback() throws RemoteException {
        mMessenger.send(createStateMessage(new Bundle()));
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mStates).isNull();
    }

    @Test
    public void send_messsageWithInvalidState_doesNotCallback() throws RemoteException {
        Bundle invalidStateBundle = createDefaultStateBundle();
        invalidStateBundle.remove(APP_STATE_KEY);
        Bundle bundle = buildStatesBundleFromBundles(singleton(invalidStateBundle));
        Message message = createStateMessage(bundle);

        mMessenger.send(message);
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mStates).isNull();
    }

    @Test
    public void send_messageWithBothInvalidAndValidStates_callsBackWithOnlyValidStates()
            throws RemoteException {
        Bundle invalidStateBundle = createDefaultStateBundle();
        invalidStateBundle.remove(APP_STATE_KEY);
        Bundle bundle =
                buildStatesBundleFromBundles(
                        asList(createDefaultStateBundle(), invalidStateBundle));
        Message message = createStateMessage(bundle);

        mMessenger.send(message);
        idleKeyedAppStatesService();

        assertThat(mKeyedAppStatesService.mStates).hasSize(1);
    }

    private static Bundle createDefaultStateBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(APP_STATE_SEVERITY, KeyedAppState.SEVERITY_INFO);
        bundle.putString(APP_STATE_KEY, "key1");

        return bundle;
    }

    private static Message createTestStateMessage() {
        return createStateMessage(buildStatesBundle(singleton(STATE)));
    }

    private static Bundle buildStatesBundleFromBundles(Collection<Bundle> bundles) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(APP_STATES, new ArrayList<>(bundles));
        return bundle;
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

    private static Message createStateMessage(Bundle appStatesBundle) {
        return createStateMessage(appStatesBundle, false);
    }

    private static Message createStateMessage(Bundle appStatesBundle, boolean immediate) {
        Message message = Message.obtain();
        message.what = immediate ? WHAT_IMMEDIATE_STATE : WHAT_STATE;
        message.obj = appStatesBundle;
        return message;
    }

    private static Message createStateMessageImmediate(Bundle appStatesBundle) {
        return createStateMessage(appStatesBundle, true);
    }

    private static void idleKeyedAppStatesService() {
        // Ensure messages are sent and handled by service
        shadowOf(getMainLooper()).idle();

        // Run any AsyncTasks executed by service
        int numRun = sAsyncTaskExecutor.runAll();

        if (numRun > 0) {
            // Receive results of AsyncTasks
            shadowOf(getMainLooper()).idle();
        }
    }
}
