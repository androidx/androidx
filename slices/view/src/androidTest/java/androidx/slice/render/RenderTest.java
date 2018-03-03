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

package androidx.slice.render;

import static androidx.slice.render.SliceRenderer.SCREENSHOT_DIR;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RenderTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    @Test
    public void testRender() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                latch.countDown();
            }
        };
        mContext.registerReceiver(receiver,
                new IntentFilter(SliceRenderActivity.ACTION_RENDER_DONE));
        mContext.startActivity(new Intent(mContext, SliceRenderActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        latch.await(30000, TimeUnit.MILLISECONDS);
        String path = mContext.getDataDir().toString() + "/" + SCREENSHOT_DIR;
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "mv " + path + " " + "/sdcard/");
    }
}
