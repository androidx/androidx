/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice.test;

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;
import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static androidx.slice.test.SampleSliceProvider.DATE_MILLIS_VALUE;
import static androidx.slice.test.SampleSliceProvider.EXTRA_ITEM_INDEX;
import static androidx.slice.test.SampleSliceProvider.STAR_RATING;
import static androidx.slice.test.SampleSliceProvider.TIME_MILLIS_VALUE;
import static androidx.slice.test.SampleSliceProvider.getUri;
import static androidx.slice.test.SampleSliceProvider.sGroceryList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.Date;

/**
 * Responds to actions performed on slices and notifies slices of updates in state changes.
 */
@RequiresApi(19)
public class SliceBroadcastReceiver extends BroadcastReceiver {
    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(final Context context, Intent i) {
        String action = i.getAction();
        switch (action) {
            case SampleSliceProvider.ACTION_ITEM_CHECKED:
                int index = i.getExtras().getInt(EXTRA_ITEM_INDEX, -1);
                if (index != -1 && sGroceryList.size() > index) {
                    sGroceryList.remove(index);
                    context.getContentResolver().notifyChange(getUri("grocery", context), null);
                }
                break;
            case SampleSliceProvider.ACTION_WIFI_CHANGED:
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                boolean newState = i.getBooleanExtra(EXTRA_TOGGLE_STATE, wm.isWifiEnabled());
                wm.setWifiEnabled(newState);
                // Wait a bit for wifi to update (TODO: is there a better way to do this?)
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        context.getContentResolver().notifyChange(getUri("wifi", context), null);
                    }
                }, 1000);
                break;
            case SampleSliceProvider.ACTION_TOAST:
                String message = i.getExtras().getString(SampleSliceProvider.EXTRA_TOAST_MESSAGE,
                        "no message");
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                break;
            case SampleSliceProvider.ACTION_TOAST_RANGE_VALUE:
                int range = i.getExtras().getInt(EXTRA_RANGE_VALUE, 0);
                STAR_RATING = range;
                Toast.makeText(context, "value: " + range, Toast.LENGTH_SHORT).show();
                context.getContentResolver().notifyChange(getUri("inputrange", context), null);
                context.getContentResolver().notifyChange(getUri("richinputrange", context), null);
                context.getContentResolver().notifyChange(getUri("indeterminateprogress3", context),
                        null);
                context.getContentResolver().notifyChange(getUri("starrating",
                        context),
                        null);
                break;
            case SampleSliceProvider.ACTION_TOAST_DATE_VALUE:
                long dateMillis = i.getExtras().getLong(EXTRA_RANGE_VALUE, 0);
                DATE_MILLIS_VALUE = dateMillis;
                Toast.makeText(context, "datevalue: " + new Date(dateMillis),
                        Toast.LENGTH_SHORT).show();
                context.getContentResolver().notifyChange(getUri("picker",
                        context),
                        null);
                context.getContentResolver().notifyChange(getUri("singlepicker",
                        context),
                        null);
                break;
            case SampleSliceProvider.ACTION_TOAST_TIME_VALUE:
                long timeMillis = i.getExtras().getLong(EXTRA_RANGE_VALUE, 0);
                TIME_MILLIS_VALUE = timeMillis;
                Toast.makeText(context, "timevalue: " + new Date(timeMillis),
                        Toast.LENGTH_SHORT).show();
                context.getContentResolver().notifyChange(getUri("picker",
                        context),
                        null);
                context.getContentResolver().notifyChange(getUri("singlepicker",
                        context),
                        null);
                break;
            case SampleSliceProvider.ACTION_PLAY_TTS:
                context.getContentResolver().notifyChange(getUri("tts", context), null);
                break;
        }
    }
}
