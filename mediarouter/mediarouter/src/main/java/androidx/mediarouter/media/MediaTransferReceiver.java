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

package androidx.mediarouter.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.List;

/**
 * A {@link BroadcastReceiver} class for enabling Media transfer feature.
 * <p>
 * Media transfer is a feature that media routing can be controlled via system UI. By using this,
 * media app users can re-route the media without opening the app activity again. Also, the media
 * can be transferred from one device to another device seamlessly, depending on the devices.
 * This feature is supported from Android 11.
 * <p>
 * To enable the media transfer feature, media apps should declare this receiver in the app's
 * manifest. For example:
 * <pre class="prettyprint">{@code
 * <application>
 *     <receiver android:name="androidx.mediarouter.media.MediaTransferReceiver" />
 * </application>
 * }</pre>
 * <p>
 * Media apps that enable this feature should implement the {@link MediaRouter.Callback} properly.
 * Specifically:
 * <ul>
 *     <li>Apps should be able to get events even when the app is in background. This means
 *         that the callback should not be removed in {@link Activity#onStop()}. (See
 *         {@link MediaRouter#addCallback(MediaRouteSelector, MediaRouter.Callback, int)} for
 *         how to add callback.</li>
 *     <li>Apps should handle the case where the media routing is changed from the outside of the
 *         app. The callback's
 *         {@link MediaRouter.Callback#onRouteSelected(MediaRouter, MediaRouter.RouteInfo, int)
 *         onRouteSelected} method should be able to handle the cases.</li>
 *     <li>In order to enable transferring media from remote to local (e.g. from TV to phone),
 *         media apps should {@link MediaRouterParams.Builder#setTransferToLocalEnabled(boolean)
 *         enable 'transfer to local' feature}. Otherwise, the local devices won't be shown as a
 *         transfer target while playing on a remote device.
 * </ul>
 */
// TODO: Mention that devs should implement onPrepareTransfer() - after the API is ready.
public final class MediaTransferReceiver extends BroadcastReceiver  {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        // Do nothing for now.
    }

    /**
     * Check whether the {@link MediaTransferReceiver} is declared in the app manifest.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static boolean isDeclared(@NonNull Context applicationContext) {
        Intent queryIntent = new Intent(applicationContext, MediaTransferReceiver.class);
        queryIntent.setPackage(applicationContext.getPackageName());
        PackageManager pm = applicationContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryBroadcastReceivers(queryIntent, 0);

        return resolveInfos.size() > 0;
    }
}
