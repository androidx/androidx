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

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.core.app.ShareCompat;

import com.example.android.supportv4.R;
import com.example.android.supportv4.content.SharingSupportProvider;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This example illustrates the use of the ShareCompat feature of the support library.
 * ShareCompat offers several pieces of functionality to assist in sharing content between
 * apps and is especially suited for sharing content to social apps that the user has installed.
 *
 * <p>Two other classes are relevant to this code sample: {@link SharingReceiverSupport} is
 * an activity that has been configured to receive ACTION_SEND and ACTION_SEND_MULTIPLE
 * sharing intents with a type of text/plain. It provides an example of writing a sharing
 * target using ShareCompat features. {@link SharingSupportProvider} is a simple
 * {@link android.content.ContentProvider} that provides access to two text files
 * created by this app to share as content streams.</p>
 */
public class SharingSupport extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.sharing_support);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ShareCompat.IntentBuilder b = ShareCompat.IntentBuilder.from(this);
        b.setType("text/plain").setText("Share from menu");
        MenuItem item = menu.add("Share");
        ShareCompat.configureMenuItem(item, b);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    public void onShareTextClick(View v) {
        ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText("I'm sharing!")
                .startChooser();
    }

    public void onShareFileClick(View v) {
        try {
            // This file will be accessed by the target of the share through
            // the ContentProvider SharingSupportProvider.
            FileWriter fw = new FileWriter(getFilesDir() + "/foo.txt");
            fw.write("This is a file share");
            fw.close();

            ShareCompat.IntentBuilder.from(this)
                    .setType("text/plain")
                    .setStream(Uri.parse(SharingSupportProvider.CONTENT_URI + "/foo.txt"))
                    .startChooser();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onShareMultipleFileClick(View v) {
        try {
            // These files will be accessed by the target of the share through
            // the ContentProvider SharingSupportProvider.
            FileWriter fw = new FileWriter(getFilesDir() + "/foo.txt");
            fw.write("This is a file share");
            fw.close();

            fw = new FileWriter(getFilesDir() + "/bar.txt");
            fw.write("This is another file share");
            fw.close();

            ShareCompat.IntentBuilder.from(this)
                    .setType("text/plain")
                    .addStream(Uri.parse(SharingSupportProvider.CONTENT_URI + "/foo.txt"))
                    .addStream(Uri.parse(SharingSupportProvider.CONTENT_URI + "/bar.txt"))
                    .startChooser();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
