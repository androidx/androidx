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

package com.example.androidx.slice.demos;

import android.app.slice.Slice;
import android.app.slice.SliceProvider;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;

import androidx.app.slice.builders.MessagingSliceBuilder;

/**
 * Examples of using slice template builders.
 */
@RequiresApi(api = 28)
public class SampleSliceProvider extends SliceProvider {
    public static final Uri MESSAGE =
            Uri.parse("content://com.example.androidx.slice.demos/message");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        String path = sliceUri.getPath();
        switch (path) {
            case "/message":
                return createMessagingSlice(sliceUri);
        }
        throw new IllegalArgumentException("Unknown uri " + sliceUri);
    }

    private Slice createMessagingSlice(Uri sliceUri) {
        // TODO: Remote input.
        return new MessagingSliceBuilder(sliceUri)
                .startMessage()
                        .addText("yo home \uD83C\uDF55, I emailed you the info")
                        .addTimestamp(System.currentTimeMillis() - 20 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(Icon.createWithResource(getContext(), R.drawable.mady))
                        .endMessage()
                .startMessage()
                        .addText("just bought my tickets")
                        .addTimestamp(System.currentTimeMillis() - 10 * DateUtils.MINUTE_IN_MILLIS)
                        .endMessage()
                .startMessage()
                        .addText("yay! can't wait for getContext() weekend!\n"
                                + "\uD83D\uDE00")
                        .addTimestamp(System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(Icon.createWithResource(getContext(), R.drawable.mady))
                        .endMessage()
                .build();

    }
}
