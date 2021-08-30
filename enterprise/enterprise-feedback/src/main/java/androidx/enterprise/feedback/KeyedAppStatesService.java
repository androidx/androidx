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
import static androidx.enterprise.feedback.KeyedAppStatesReporter.WHAT_IMMEDIATE_STATE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.WHAT_STATE;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base service for receiving app states in Device Owner and Profile Owner apps.
 *
 * <p>Extend this class and declare it as a service in the manifest. For example, if your service is
 * called 'MyAppStatesService', including the following in the manifest:
 *
 * <pre>
 * {@literal
 * <service android:name=".MyAppStatesService">
 *     <intent-filter>
 *         <action android:name="androidx.enterprise.feedback.action.APP_STATES" />
 *     </intent-filter>
 * </service>}</pre>
 *
 * <p>Override {@link #onReceive(Collection, boolean)} to receive keyed app states. {@link
 * #onReceive(Collection, boolean)} is invoked on a background thread.
 */
public abstract class KeyedAppStatesService extends Service {

    private static final String LOG_TAG = "KeyedAppStatesService";

    // This form is used instead of AsyncTask.execute(Runnable) as Robolectric causes tests to wait
    // for execution of these but does not currently wait for execution of
    // android.os.AsyncTask.execute(runnable).
    @SuppressWarnings("deprecation") /* AsyncTask */
    private static final class KeyedAppStatesServiceAsyncTask
            extends android.os.AsyncTask<Void, Void, Void> {

        @SuppressLint("StaticFieldLeak")
        // Instances are short-lived so won't block garbage collection.
        private final KeyedAppStatesService mKeyedAppStatesService;

        private final Collection<ReceivedKeyedAppState> mStates;
        private final boolean mRequestSync;

        KeyedAppStatesServiceAsyncTask(
                KeyedAppStatesService keyedAppStatesService,
                Collection<ReceivedKeyedAppState> states,
                boolean requestSync) {

            this.mKeyedAppStatesService = keyedAppStatesService;
            this.mStates = states;
            this.mRequestSync = requestSync;
        }

        @Override
        protected Void doInBackground(Void... o) {
            mKeyedAppStatesService.onReceive(mStates, mRequestSync);
            return null;
        }
    }

    @SuppressWarnings("deprecation") /* AsyncTask */
    private static class IncomingHandler extends Handler {
        private final KeyedAppStatesService mKeyedAppStatesService;

        IncomingHandler(KeyedAppStatesService keyedAppStatesService) {
            this.mKeyedAppStatesService = keyedAppStatesService;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case WHAT_STATE:
                    handleStateMessage(message, /* requestSync = */ false);
                    break;
                case WHAT_IMMEDIATE_STATE:
                    handleStateMessage(message, /* requestSync = */ true);
                    break;
                default:
                    super.handleMessage(message);
            }
        }

        private void handleStateMessage(Message message, boolean requestSync) {
            // Fetch the timestamp as close to sending time as possible.
            long timestamp = System.currentTimeMillis();

            String packageName =
                    mKeyedAppStatesService
                            .getApplicationContext()
                            .getPackageManager()
                            .getNameForUid(message.sendingUid);

            Collection<ReceivedKeyedAppState> states =
                    extractReceivedKeyedAppStates(message, packageName, timestamp);
            if (states.isEmpty()) {
                return;
            }

            KeyedAppStatesServiceAsyncTask asyncTask =
                    new KeyedAppStatesServiceAsyncTask(
                            mKeyedAppStatesService, deduplicateStates(states), requestSync);

            asyncTask.execute();
        }

        private static Collection<ReceivedKeyedAppState> extractReceivedKeyedAppStates(
                Message message, String packageName, long timestamp) {
            Bundle bundle;

            try {
                bundle = (Bundle) message.obj;
            } catch (ClassCastException e) {
                Log.e(LOG_TAG, "Could not extract state bundles from message", e);
                return Collections.emptyList();
            }

            if (bundle == null) {
                Log.e(LOG_TAG, "Could not extract state bundles from message");
                return Collections.emptyList();
            }

            Collection<Bundle> stateBundles = bundle.getParcelableArrayList(APP_STATES);

            if (stateBundles == null) {
                Log.e(LOG_TAG, "Could not extract state bundles from message");
                return Collections.emptyList();
            }

            Collection<ReceivedKeyedAppState> states = new ArrayList<>();
            for (Bundle stateBundle : stateBundles) {
                if (!KeyedAppState.isValid(stateBundle)) {
                    Log.e(LOG_TAG, "Invalid KeyedAppState in bundle");
                    continue;
                }
                states.add(ReceivedKeyedAppState.fromBundle(stateBundle, packageName, timestamp));
            }

            return Collections.unmodifiableCollection(states);
        }

        private static Collection<ReceivedKeyedAppState> deduplicateStates(
                Collection<ReceivedKeyedAppState> keyedAppStates) {
            Map<String, ReceivedKeyedAppState> mappedStates = new HashMap<>();
            for (ReceivedKeyedAppState state : keyedAppStates) {
                mappedStates.put(state.getKey(), state);
            }

            return mappedStates.values();
        }
    }

    @Override
    @NonNull
    public IBinder onBind(@NonNull Intent intent) {
        Messenger messenger = new Messenger(new IncomingHandler(this));
        return messenger.getBinder();
    }

    /**
     * Called when an app sends states. States are key/value, so new values should replace existing
     * ones for the same key.
     *
     * @param states      The states sent by an app. Every state will have the same packageName
     *                    and timestamp.
     * @param requestSync {@code true} if the app requests an immediate upload for access by server
     *                    APIs. This immediate upload request does not have to be respected if a
     *                    quota that you have defined has been exceeded.
     */
    public abstract void onReceive(
            @NonNull Collection<ReceivedKeyedAppState> states, boolean requestSync);
}
