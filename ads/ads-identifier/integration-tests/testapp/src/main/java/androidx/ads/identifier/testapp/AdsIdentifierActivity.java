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

package androidx.ads.identifier.testapp;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import androidx.ads.identifier.AdvertisingIdClient;
import androidx.ads.identifier.AdvertisingIdInfo;
import androidx.ads.identifier.AdvertisingIdUtils;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Simple activity as an ads identifier developer.
 */
public class AdsIdentifierActivity extends Activity {

    private static final String HIGH_PRIORITY_PERMISSION =
            "androidx.ads.identifier.provider.HIGH_PRIORITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads_identifier);
    }

    /** Gets Advertising ID. */
    public void getId(View view) {
        TextView textView = findViewById(R.id.text);
        ListenableFuture<AdvertisingIdInfo> advertisingIdInfoListenableFuture =
                AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
        Futures.addCallback(advertisingIdInfoListenableFuture,
                new FutureCallback<AdvertisingIdInfo>() {
                    @Override
                    public void onSuccess(AdvertisingIdInfo advertisingIdInfo) {
                        runOnUiThread(() -> textView.setText(advertisingIdInfo.toString()));
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        runOnUiThread(() -> textView.setText(throwable.toString()));
                    }
                }, MoreExecutors.directExecutor());
    }

    /** Gets Advertising ID synchronously. */
    public void getIdSync(View view) {
        TextView textView = findViewById(R.id.text);
        new Thread(() -> {
            AdvertisingIdInfo advertisingIdInfo;
            try {
                advertisingIdInfo =
                        AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext()).get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                runOnUiThread(() -> textView.setText(cause.toString()));
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                runOnUiThread(() -> textView.setText(e.toString()));
                return;
            }
            runOnUiThread(() -> textView.setText(advertisingIdInfo.toString()));
        }).start();
    }

    /** Checks is provider available. */
    public void isProviderAvailable(View view) {
        TextView textView = findViewById(R.id.text);
        boolean isAvailable = AdvertisingIdClient.isAdvertisingIdProviderAvailable(this);
        textView.setText(String.valueOf(isAvailable));
    }

    /** Lists all the providers. */
    @SuppressWarnings("deprecation")
    public void listProvider(View view) {
        TextView textView = findViewById(R.id.text);
        textView.setText("Services:\n");

        List<ServiceInfo> serviceInfos =
                AdvertisingIdUtils.getAdvertisingIdProviderServices(getPackageManager());
        for (ServiceInfo serviceInfo : serviceInfos) {
            PackageInfo packageInfo;
            try {
                packageInfo = getPackageManager().getPackageInfo(serviceInfo.packageName,
                        PackageManager.GET_PERMISSIONS);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            show(textView, packageInfo);
        }
    }

    private void show(TextView textView, PackageInfo packageInfo) {
        textView.append(String.format(Locale.US, "%s\nFLAG_SYSTEM:%d\n",
                packageInfo.packageName,
                packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM));
        textView.append(String.format(Locale.US, "isRequestHighPriority:%s\n",
                isRequestHighPriority(packageInfo.requestedPermissions)));
        textView.append(String.format(Locale.US, "firstInstallTime:%s\n",
                DateFormat.format("yyyy-MM-dd HH:mm:ss", packageInfo.firstInstallTime)));
        textView.append("\n");
    }

    private static boolean isRequestHighPriority(String[] array) {
        if (array == null) {
            return false;
        }
        for (String permission : array) {
            if (HIGH_PRIORITY_PERMISSION.equals(permission)) {
                return true;
            }
        }
        return false;
    }
}
