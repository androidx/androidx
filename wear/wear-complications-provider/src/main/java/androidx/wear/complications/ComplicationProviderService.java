/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.IComplicationManager;
import android.support.wearable.complications.IComplicationProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.wear.complications.data.ComplicationData;
import androidx.wear.complications.data.ComplicationType;

/**
 * Class for providers of complication data.
 *
 * <p>A provider service must implement {@link #onComplicationUpdate} to respond to requests for
 * updates from the complication system.
 *
 * <p>Manifest requirements:
 * <ul>
 * <il>The manifest declaration of this service must include an intent filter for
 * android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST.</il>
 *
 * <il>A ComplicationProviderService must include a {@code meta-data} tag with
 * android.support.wearable.complications.SUPPORTED_TYPES in its manifest entry. The value of
 * this tag should be a comma separated list of types supported by
 * the provider. Types should be given as named as per the type fields in the {@link
 * ComplicationData}, but omitting the "TYPE_" prefix, e.g. {@code SHORT_TEXT}, {@code
 * LONG_TEXT}, {@code RANGED_VALUE}.
 *
 * <p>The order in which types are listed has no significance. In the case where a watch face
 * supports multiple types in a single complication slot, the watch face will determine which
 * types it prefers.
 *
 * <p>For example, a provider that supports the RANGED_VALUE, SHORT_TEXT, and ICON types would
 * include the following in its manifest entry:
 *
 * <pre class="prettyprint">
 * &lt;meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *         android:value="RANGED_VALUE,SHORT_TEXT,ICON"/&gt;</pre>
 * </il>
 *
 * <il>A ComplicationProviderService should include a {@code meta-data} tag with
 * android.support.wearable.complications.UPDATE_PERIOD_SECONDS its manifest entry. The value of
 * this tag is the number of seconds the provider would like to elapse between update requests.
 *
 * <p>Note that update requests are not guaranteed to be sent with this frequency.
 *
 * <p>If a provider never needs to receive update requests beyond the one sent when a
 * complication is activated, the value of this tag should be 0.
 *
 * <p>For example, a provider that would like to update every ten minutes should include the
 * following in its manifest entry:
 *
 * <pre class="prettyprint">
 * &lt;meta-data android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
 *         android:value="600"/&gt;</pre>
 * </il>
 *
 * <il>A ComplicationProviderService can include a {@code meta-data} tag with
 * android.support.wearable.complications.PROVIDER_CONFIG_ACTION its manifest entry to cause a
 * configuration activity to be shown when the provider is selected.
 *
 * <p>The configuration activity must reside in the same package as the provider, and must
 * register an intent filter for the action specified here, including
 * android.support.wearable.complications.category.PROVIDER_CONFIG as well as
 * {@link Intent#CATEGORY_DEFAULT} as categories.
 *
 * <p>The complication id being configured will be included in the intent that starts the config
 * activity using the extra key android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID.
 *
 * <p>The complication type that will be requested from the provider will also be included,
 * using the extra key android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_TYPE.
 *
 * <p>The provider's {@link ComponentName} will also be included in the intent that starts the
 * config activity, using the extra key
 * android.support.wearable.complications.EXTRA_CONFIG_PROVIDER_COMPONENT.
 *
 * <p>The config activity must call {@link Activity#setResult} with either {@link
 * Activity#RESULT_OK} or {@link Activity#RESULT_CANCELED} before it is finished, to tell the
 * system whether or not the provider should be set on the given complication.</il>
 *
 * <il>The manifest entry for the service should also include an android:icon attribute. The icon
 * provided there should be a single-color white icon that represents the provider. This icon will
 * be shown in the provider chooser interface, and may also be included in {@link
 * ComplicationProviderInfo} given to watch faces for display in their configuration activities.
 * </il>
 *
 * <il>The manifest entry should also include {@code
 * android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER"} to ensure
 * that only the system can bind to it.</il>
 * </ul>
 */
public abstract class ComplicationProviderService extends Service {

    /**
     * The intent action used to send update requests to the provider. Complication provider
     * services must declare an intent filter for this action in the manifest.
     */
    @SuppressWarnings("ActionValue")
    public static final String ACTION_COMPLICATION_UPDATE_REQUEST =
            "android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST";

    /**
     * Metadata key used to declare supported complication types.
     *
     * <p>A ComplicationProviderService must include a {@code meta-data} tag with this name in its
     * manifest entry. The value of this tag should be a comma separated list of types supported by
     * the provider. Types should be given as named as per the type fields in the {@link
     * ComplicationData}, but omitting the "TYPE_" prefix, e.g. {@code SHORT_TEXT},
     * {@code LONG_TEXT}, {@code RANGED_VALUE}.
     *
     * <p>The order in which types are listed has no significance. In the case where a watch face
     * supports multiple types in a single complication slot, the watch face will determine which
     * types it prefers.
     *
     * <p>For example, a provider that supports the RANGED_VALUE, SHORT_TEXT, and ICON types would
     * include the following in its manifest entry:
     *
     * <pre class="prettyprint">
     * &lt;meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
     *         android:value="RANGED_VALUE,SHORT_TEXT,ICON"/&gt;</pre>
     */
    public static final String METADATA_KEY_SUPPORTED_TYPES =
            "android.support.wearable.complications.SUPPORTED_TYPES";

    /**
     * Metadata key used to declare the requested frequency of update requests.
     *
     * <p>A ComplicationProviderService should include a {@code meta-data} tag with this name in its
     * manifest entry. The value of this tag is the number of seconds the provider would like to
     * elapse between update requests.
     *
     * <p>Note that update requests are not guaranteed to be sent with this frequency.
     *
     * <p>If a provider never needs to receive update requests beyond the one sent when a
     * complication is activated, the value of this tag should be 0.
     *
     * <p>For example, a provider that would like to update every ten minutes should include the
     * following in its manifest entry:
     *
     * <pre class="prettyprint">
     * &lt;meta-data android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
     *         android:value="600"/&gt;</pre>
     */
    public static final String METADATA_KEY_UPDATE_PERIOD_SECONDS =
            "android.support.wearable.complications.UPDATE_PERIOD_SECONDS";

    /**
     * Metadata key used to declare a list of watch faces that may receive data from a provider
     * before they are granted the RECEIVE_COMPLICATION_DATA permission. This allows the listed
     * watch
     * faces to set the provider as a default and have the complication populate when the watch face
     * is first seen.
     *
     * <p>Only trusted watch faces that will set this provider as a default should be included in
     * this list.
     *
     * <p>Note that if a watch face is in the same app package as the provider, it does not need to
     * be added to this list.
     *
     * <p>The value of this tag should be a comma separated list of watch faces or packages. An
     * entry
     * can be a flattened component, as if {@link ComponentName#flattenToString()} had been called,
     * to declare a specific watch face as safe. An entry can also be a package name, as if {@link
     * ComponentName#getPackageName()} had been called, in which case any watch face under the app
     * with that package name will be considered safe for this provider.
     */
    public static final String METADATA_KEY_SAFE_WATCH_FACES =
            "android.support.wearable.complications.SAFE_WATCH_FACES";

    /**
     * Metadata key used to declare that the provider should be hidden from the provider chooser
     * interface. If set to "true", users will not be able to select this provider. The provider may
     * still be specified as a default provider by watch faces.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String METADATA_KEY_HIDDEN =
            "android.support.wearable.complications.HIDDEN";

    /**
     * Metadata key used to declare an action for a configuration activity for a provider.
     *
     * <p>A ComplicationProviderService can include a {@code meta-data} tag with this name in its
     * manifest entry to cause a configuration activity to be shown when the provider is selected.
     *
     * <p>The configuration activity must reside in the same package as the provider, and must
     * register an intent filter for the action specified here, including {@link
     * #CATEGORY_PROVIDER_CONFIG_ACTION} as well as {@link Intent#CATEGORY_DEFAULT} as categories.
     *
     * <p>The complication id being configured will be included in the intent that starts the config
     * activity using the extra key {@link #EXTRA_CONFIG_COMPLICATION_ID}.
     *
     * <p>The complication type that will be requested from the provider will also be included,
     * using
     * the extra key {@link #EXTRA_CONFIG_COMPLICATION_TYPE}.
     *
     * <p>The provider's {@link ComponentName} will also be included in the intent that starts the
     * config activity, using the extra key {@link #EXTRA_CONFIG_PROVIDER_COMPONENT}.
     *
     * <p>The config activity must call {@link Activity#setResult} with either {@link
     * Activity#RESULT_OK} or {@link Activity#RESULT_CANCELED} before it is finished, to tell the
     * system whether or not the provider should be set on the given complication.
     */
    @SuppressLint("IntentName")
    public static final String METADATA_KEY_PROVIDER_CONFIG_ACTION =
            "android.support.wearable.complications.PROVIDER_CONFIG_ACTION";

    /**
     * Category for provider config activities. The configuration activity for a complication
     * provider must specify this category in its intent filter.
     *
     * @see #METADATA_KEY_PROVIDER_CONFIG_ACTION
     */
    @SuppressLint("IntentName")
    public static final String CATEGORY_PROVIDER_CONFIG_ACTION =
            "android.support.wearable.complications.category.PROVIDER_CONFIG";

    /** Extra used to supply the complication id to a provider configuration activity. */
    @SuppressLint("ActionValue")
    public static final String EXTRA_CONFIG_COMPLICATION_ID =
            "android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID";

    /** Extra used to supply the complication type to a provider configuration activity. */
    @SuppressLint("ActionValue")
    public static final String EXTRA_CONFIG_COMPLICATION_TYPE =
            "android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_TYPE";

    /** Extra used to supply the provider component to a provider configuration activity. */
    @SuppressLint("ActionValue")
    public static final String EXTRA_CONFIG_PROVIDER_COMPONENT =
            "android.support.wearable.complications.EXTRA_CONFIG_PROVIDER_COMPONENT";

    @Nullable
    private IComplicationProviderWrapper mWrapper;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    @SuppressLint("SyntheticAccessor")
    @Override
    @Nullable
    public IBinder onBind(@NonNull Intent intent) {
        if (ACTION_COMPLICATION_UPDATE_REQUEST.equals(intent.getAction())) {
            if (mWrapper == null) {
                mWrapper = new IComplicationProviderWrapper();
            }
            return mWrapper;
        }
        return null;
    }

    /**
     * Called when a complication is activated.
     *
     * <p>This occurs when the watch face calls setActiveComplications, or when this provider is
     * chosen for a complication which is already active.
     *
     * <p>This will usually be followed by a call to {@link #onComplicationUpdate}.
     *
     * <p>This will be called on the main thread.
     */
    @UiThread
    public void onComplicationActivated(int complicationId, int type) {
    }

    /**
     * Called when a complication data update is requested for the given complication id.
     *
     * <p>In response to this request the result callback should be called with the data to be
     * displayed. If the request can not be fulfilled or no update is needed then null should be
     * passed to the callback.
     *
     * <p>The callback doesn't have be called within onComplicationUpdate but it should be called
     * soon after. If this does not occur within around 20 seconds (exact timeout length subject
     * to change), then the system will unbind from this service which may cause your eventual
     * update to not be received.
     *
     * @param complicationId The id of the requested complication. Note this ID is distinct from
     *                       ids used by the watch face itself.
     * @param type           The type of complication data requested.
     * @param resultCallback The callback to pass the result to the system.
     */
    @UiThread
    public abstract void onComplicationUpdate(
            int complicationId,
            @NonNull ComplicationType type,
            @NonNull ComplicationUpdateCallback resultCallback);

    /**
     * A request for representative preview data for the complication, for use in the editor UI.
     * Preview data is assumed to be static per type. E.g. for a complication that displays the date
     * and time of an event, rather than returning the real time it should return a fixed date and
     * time such as 10:10 Aug 1st.
     *
     * <p>This will be called on a background thread.
     *
     * @param type The type of complication preview data requested.
     * @return Preview data for the given complication type.
     */
    @Nullable
    public abstract ComplicationData getPreviewData(@NonNull ComplicationType type);

    /** Callback for {@link #onComplicationUpdate}. */
    public interface ComplicationUpdateCallback {
        /**
         * Sends the complicationData to the system. If null is passed then any
         * previous complication data will not be overwritten. Can be called on any thread. Should
         * only be called once.
         */
        void onUpdateComplication(@Nullable ComplicationData complicationData)
                throws RemoteException;
    }

    /**
     * Called when a complication is deactivated.
     *
     * <p>This occurs when the current watch face changes, or when the watch face calls
     * setActiveComplications and does not include the given complication (usually because the watch
     * face has stopped displaying it).
     *
     * <p>This will be called on the main thread.
     */
    @UiThread
    public void onComplicationDeactivated(int complicationId) {
    }

    private class IComplicationProviderWrapper extends IComplicationProvider.Stub {
        @SuppressLint("SyntheticAccessor")
        @Override
        public void onUpdate(final int complicationId, final int type, IBinder manager) {
            final ComplicationType complicationType = ComplicationType.fromWireType(type);
            final IComplicationManager iComplicationManager =
                    IComplicationManager.Stub.asInterface(manager);
            mMainThreadHandler.post(
                    () -> onComplicationUpdate(complicationId, complicationType,
                            complicationData -> {
                                // This can be run on an arbitrary thread, but that's OK.
                                ComplicationType dataType =
                                        complicationData != null ? complicationData.getType() :
                                                ComplicationType.NO_DATA;
                                if (dataType == ComplicationType.NOT_CONFIGURED
                                        || dataType == ComplicationType.EMPTY) {
                                    throw new IllegalArgumentException(
                                            "Cannot send data of TYPE_NOT_CONFIGURED or "
                                                    + "TYPE_EMPTY. Use TYPE_NO_DATA instead.");
                                }

                                // When no update is needed, the complicationData is going to be
                                // null.
                                iComplicationManager.updateComplicationData(
                                        complicationId,
                                        (complicationData != null)
                                                ? complicationData.asWireComplicationData()
                                                : null);
                            }));
        }

        @Override
        @SuppressLint("SyntheticAccessor")
        public void onComplicationDeactivated(final int complicationId) {
            mMainThreadHandler.post(
                    () -> ComplicationProviderService.this.onComplicationDeactivated(
                            complicationId));
        }

        @Override
        @SuppressLint("SyntheticAccessor")
        public void onComplicationActivated(
                final int complicationId, final int type, IBinder manager) {
            mMainThreadHandler.post(
                    () -> ComplicationProviderService.this.onComplicationActivated(
                            complicationId, type));
        }

        @Override
        public int getApiVersion() {
            return IComplicationProvider.API_VERSION;
        }

        @Override
        @SuppressLint("SyntheticAccessor")
        public android.support.wearable.complications.ComplicationData getComplicationPreviewData(
                final int type) {
            return getPreviewData(ComplicationType.fromWireType(type)).asWireComplicationData();
        }
    }
}
