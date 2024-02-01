/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.car.app.mediaextensions.analytics.client;

import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_BUNDLE_KEY_PASSKEY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.mediaextensions.analytics.Constants;
import androidx.car.app.mediaextensions.analytics.ThreadUtils;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * BroadcastReceiver that parses {@link AnalyticsEvent} from intent and hands off to
 * {@link AnalyticsCallback} .
 *
 * <p>
 *     Extend and add to manifest with {@link Constants#ACTION_ANALYTICS}.
 *
 * <p>
 *     Add analytics opt-in and sessionId to rootHints with
 *     {@link RootHintsUtil.RootHintsPopulator}
 */
@ExperimentalCarApi
public abstract class AnalyticsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "AnalyticsBroadcastRcvr";
    static final UUID sAuthKey = UUID.randomUUID();

    private final AnalyticsCallback mAnalyticsCallback;
    private final Executor mExecutor;

    /**
     * BroadcastReceiver used to receive analytic events.
     * <p>
     * Note that the callback will be executed on the main thread using
     * {@link Looper#getMainLooper()}. To specify the execution thread, use
     * {@link #AnalyticsBroadcastReceiver(Executor, AnalyticsCallback)}.
     *
     * @param analyticsCallback Callback for {@link AnalyticsEvent AnalyticEvents} handled on
     *                          main thread.
     */
    @MainThread
    public AnalyticsBroadcastReceiver(@NonNull AnalyticsCallback analyticsCallback) {
        super();
        this.mExecutor = ThreadUtils.getMainThreadExecutor();
        this.mAnalyticsCallback = analyticsCallback;
    }

    /**
     * BroadcastReceiver used to receive analytic events.
     *
     * @param executor executor used in calling callback.
     * @param analyticsCallback Callback for {@link AnalyticsEvent AnalyticEvents} handled on
     *                          main thread.
     */
    public AnalyticsBroadcastReceiver(@NonNull Executor executor,
            @NonNull AnalyticsCallback analyticsCallback) {
        super();
        this.mExecutor = executor;
        this.mAnalyticsCallback = analyticsCallback;
    }

    /**
     * Receives intent with analytic events packed in arraylist of bundles.
     * <p>
     * Parses and sends to {@link AnalyticsCallback} with the result on main thread.
     * <p>
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() != null && isValid(sAuthKey.toString(), intent.getExtras())) {
            AnalyticsParser.parseAnalyticsIntent(intent, mExecutor, mAnalyticsCallback);
        } else {
            Log.w(TAG, "Invalid analytics auth key, ignoring analytics event!");
        }
    }

    /**
     * Checks if passkey in {@link AnalyticsEvent analyticsEvent} bundle is same passkey as
     * {@link AnalyticsBroadcastReceiver#sAuthKey}.
     */
    private boolean isValid(@NonNull String receiverPassKey,
            @NonNull Bundle bactchBundle) {
        String bundlePassKey = bactchBundle.getString(ANALYTICS_BUNDLE_KEY_PASSKEY);
        return bundlePassKey != null && Objects.equals(receiverPassKey, bundlePassKey);
    }
}
