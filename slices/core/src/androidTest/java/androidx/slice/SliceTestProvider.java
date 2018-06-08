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

package androidx.slice;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LIST;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_TITLE;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice.Builder;
import androidx.slice.core.test.R;

public class SliceTestProvider extends androidx.slice.SliceProvider {

    @Override
    public boolean onCreateSliceProvider() {
        getContext().getPackageName();
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        switch (sliceUri.getPath()) {
            case "/set_flag":
                SliceTest.sFlag = true;
                break;
            case "/subslice":
                Builder b = new Builder(sliceUri);
                return b.addSubSlice(new Slice.Builder(b).build(), "subslice").build();
            case "/text":
                return new Slice.Builder(sliceUri).addText("Expected text", "text").build();
            case "/icon":
                return new Slice.Builder(sliceUri).addIcon(
                        IconCompat.createWithResource(getContext(), R.drawable.size_48x48),
                        "icon").build();
            case "/action":
                Builder builder = new Builder(sliceUri);
                Slice subSlice = new Slice.Builder(builder).build();
                PendingIntent broadcast = PendingIntent.getBroadcast(getContext(), 0,
                        new Intent(getContext().getPackageName() + ".action"), 0);
                return builder.addAction(broadcast, subSlice, "action").build();
            case "/int":
                return new Slice.Builder(sliceUri).addInt(0xff121212, "int").build();
            case "/timestamp":
                return new Slice.Builder(sliceUri).addTimestamp(43, "timestamp").build();
            case "/hints":
                return new Slice.Builder(sliceUri)
                        .addHints(HINT_LIST)
                        .addText("Text", null, HINT_TITLE)
                        .addIcon(IconCompat.createWithResource(getContext(), R.drawable.size_48x48),
                                null, HINT_NO_TINT, HINT_LARGE)
                        .build();
        }
        return new Slice.Builder(sliceUri).build();
    }

}
