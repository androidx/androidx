/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.slice.core.SliceHints.INFINITY;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.slice.builders.ListBuilder;
import androidx.slice.render.SliceRenderActivity;
import androidx.slice.widget.SliceLiveData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

// TODO: move to slice-builders module?
/**
 * Tests for content validation of the different slice builders.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceBuilderTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    @Before
    public void setup() {
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowForInvalidRangeMax() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, INFINITY);
        Slice slice = lb.addInputRange(new ListBuilder.InputRangeBuilder(lb)
                .setInputAction(getIntent(""))
                .setMax(20)
                .setMin(50))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowForInvalidRangeMin() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, INFINITY);
        Slice slice = lb.addInputRange(new ListBuilder.InputRangeBuilder(lb)
                .setInputAction(getIntent(""))
                .setMax(20)
                .setMin(30))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowForInvalidRangeValue() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, INFINITY);
        Slice slice = lb.addInputRange(new ListBuilder.InputRangeBuilder(lb)
                .setInputAction(getIntent(""))
                .setMax(80)
                .setValue(100)
                .setMin(30))
                .build();
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        intent.setClassName(mContext.getPackageName(), SliceRenderActivity.class.getName());
        return PendingIntent.getActivity(mContext, 0, intent, 0);
    }
}
