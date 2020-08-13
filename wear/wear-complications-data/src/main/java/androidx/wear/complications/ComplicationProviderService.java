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

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.IComplicationManager;
import android.support.wearable.complications.IComplicationProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private static final String RETAIL_PACKAGE = "com.google.android.apps.wearable.settings";
    private static final String RETAIL_CLASS =
            "com.google.android.clockwork.settings.RetailStatusService";

    private IComplicationProviderWrapper mWrapper;
    final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

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
     * <p>Once this has been called, complication data may be sent for the given {@code
     * complicationId}, until {@link #onComplicationDeactivated} is called for that id.
     *
     * <p>This will usually be followed by a call to {@link #onComplicationUpdate}.
     *
     * <p>This will be called on the main thread.
     */
    public void onComplicationActivated(
            int complicationId, int type, @NonNull ComplicationManager manager) {}

    /**
     * Called when a complication data update is requested for the given complication id.
     *
     * <p>In response to this request, {@link ComplicationManager#updateComplicationData} should be
     * called on the provided {@link ComplicationManager} instance with the data to be displayed.
     * Or, if no update is needed, {@link ComplicationManager#noUpdateRequired} may be called
     * instead. One of these methods must be called so that the system knows when the provider has
     * finished responding to the request.
     *
     * <p>This call does not need to happen from within this method, but it should be made
     * reasonably soon after the call to this method occurred. If a call does not occur within
     * around 20 seconds (exact timeout length subject to change), then the system will unbind from
     * this service which may cause your eventual update to not be received.
     *
     * <p>This will be called on the main thread.
     */
    public abstract void onComplicationUpdate(
            int complicationId, int type, @NonNull ComplicationManager manager);

    /**
     * Called when a complication is deactivated.
     *
     * <p>This occurs when the current watch face changes, or when the watch face calls
     * setActiveComplications and does not include the given complication (usually because the watch
     * face has stopped displaying it).
     *
     * <p>Once this has been called, no complication data should be sent for the given {@code
     * complicationId}, until {@link #onComplicationActivated} is called again for that id.
     *
     * <p>This will be called on the main thread.
     */
    public void onComplicationDeactivated(int complicationId) {}

    /**
     * Returns true if the device is currently running in retail mode (e.g. the watch is being
     * demonstrated in a store, or the watch face is being configured by the system UI). If it's in
     * retail mode then representative mock data should be returned via
     * {@link ComplicationManager#updateComplicationData}.
     */
    protected boolean inRetailMode() {
        ComponentName component = new ComponentName(RETAIL_PACKAGE, RETAIL_CLASS);
        return (getPackageManager().getComponentEnabledSetting(component)
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    class IComplicationProviderWrapper extends IComplicationProvider.Stub {
        @Override
        public void onUpdate(final int complicationId, final int type, IBinder manager) {
            final ComplicationManager complicationManager =
                    new ComplicationManager(IComplicationManager.Stub.asInterface(manager));
            mMainThreadHandler.post(
                    () -> onComplicationUpdate(complicationId, type, complicationManager));
        }

        @Override
        public void onComplicationDeactivated(final int complicationId) {
            mMainThreadHandler.post(
                    () ->
                            ComplicationProviderService.this.onComplicationDeactivated(
                                    complicationId));
        }

        @Override
        public void onComplicationActivated(
                final int complicationId, final int type, IBinder manager) {
            final ComplicationManager complicationManager =
                    new ComplicationManager(IComplicationManager.Stub.asInterface(manager));
            mMainThreadHandler.post(
                    () ->
                            ComplicationProviderService.this.onComplicationActivated(
                                    complicationId, type, complicationManager));
        }
    }
}
