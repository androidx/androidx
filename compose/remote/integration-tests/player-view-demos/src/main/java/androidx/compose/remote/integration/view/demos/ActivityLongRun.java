/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos;

import static androidx.compose.remote.integration.view.demos.examples.CountdownKt.countDown;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.jspecify.annotations.Nullable;

/**
 * This is used to do long run test (typically over night) This insures we are not doing anything
 * that degrades the performance of the device over time.
 */
@SuppressWarnings("RestrictedApiAndroidX")
public class ActivityLongRun extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RemoteComposeWriter doc = countDown().getWriter();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Hide the system bars.
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        // Configure the system bars to behave in a way that allows for immersive content.
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        FrameLayout frameLayout = new FrameLayout(getApplicationContext());
        byte[] buffer = doc.getBuffer().getBuffer().cloneBytes();
        RemoteDocument rcd = new RemoteDocument(buffer);
        RemoteComposePlayer player = new RemoteComposePlayer(getApplicationContext());
        player.setDocument(rcd);
        frameLayout.setBackgroundColor(Color.BLACK);
        int widthPixels = Resources.getSystem().getDisplayMetrics().widthPixels;
        int heightPixels = Resources.getSystem().getDisplayMetrics().heightPixels;
        int w = (int) (heightPixels * 1920 / 1080f);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(w, heightPixels);
        params.gravity = Gravity.CENTER;
        params.leftMargin = 0;
        params.rightMargin = 100;
        player.setLayoutParams(params);
        player.setScaleX((widthPixels - 200) / (float) w);
        frameLayout.addView(player);

        setContentView(frameLayout);
    }
}
