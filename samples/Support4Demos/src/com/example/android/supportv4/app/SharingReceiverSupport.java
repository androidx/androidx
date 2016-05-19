/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.supportv4.app;

import com.example.android.supportv4.R;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This example shows a simple way to handle data shared with your app through the
 * use of the support library's ShareCompat features. It will display shared text
 * content as well as the application label and icon of the app that shared the content.
 */
public class SharingReceiverSupport extends Activity {
    private static final String TAG = "SharingReceiverSupport";
    private static final int ICON_SIZE = 32; // dip

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.sharing_receiver_support);

        final float density = getResources().getDisplayMetrics().density;
        final int iconSize = (int) (ICON_SIZE * density + 0.5f);

        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);

        // The following provides attribution for the app that shared the data with us.
        TextView info = (TextView) findViewById(R.id.app_info);
        Drawable d = intentReader.getCallingActivityIcon();
        d.setBounds(0, 0, iconSize, iconSize);
        info.setCompoundDrawables(d, null, null, null);
        info.setText(intentReader.getCallingApplicationLabel());

        TextView tv = (TextView) findViewById(R.id.text);
        StringBuilder txt = new StringBuilder("Received share!\nText was: ");

        txt.append(intentReader.getText());
        txt.append("\n");

        txt.append("Streams included:\n");
        final int N = intentReader.getStreamCount();
        for (int i = 0; i < N; i++) {
            Uri uri = intentReader.getStream(i);
            txt.append("Share included stream " + i + ": " + uri + "\n");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        getContentResolver().openInputStream(uri)));
                try {
                    txt.append(reader.readLine() + "\n");
                } catch (IOException e) {
                    Log.e(TAG, "Reading stream threw exception", e);
                } finally {
                    reader.close();
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found from share.", e);
            } catch (IOException e) {
                Log.d(TAG, "I/O Error", e);
            }
        }

        tv.setText(txt.toString());
    }
}
