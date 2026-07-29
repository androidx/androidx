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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.compose.remote.core.CoreDocument.ShaderControl;
import androidx.compose.remote.creation.RemoteComposeContext;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.integration.view.demos.customviews.AndroidCustomSupport;
import androidx.compose.remote.integration.view.demos.dsl.games.DslGameFlappyDroidKt;
import androidx.compose.remote.integration.view.demos.examples.CubeSphereTransitionShader2Kt;
import androidx.compose.remote.integration.view.demos.examples.CubeSphereTransitionShaderKt;
import androidx.compose.remote.integration.view.demos.examples.DemoParticlesKt;
import androidx.compose.remote.integration.view.demos.examples.ParticleDotsKt;
import androidx.compose.remote.integration.view.demos.examples.ParticleSphereKt;
import androidx.compose.remote.integration.view.demos.examples.Particles3Kt;
import androidx.compose.remote.integration.view.demos.examples.old.FancyClocks;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * ActivityLongRun modified to render 2D and 3D demos square on screen for screenshot capture.
 */
@SuppressWarnings("RestrictedApiAndroidX")
public class ActivityLongRun extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Hide system bars
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        renderDemo(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        renderDemo(intent);
    }

    private void renderDemo(Intent intent) {
        String demoType = intent != null ? intent.getStringExtra("demo_type") : "2d";

        byte[] buffer = null;
        {
            RemoteComposeWriter writer = FancyClocks.fancyClock2();
            buffer = Arrays.copyOf(writer.buffer(), writer.bufferSize());
        }

        String docName = "CubeSphereTransitionShader.rc";
        if ("fancyclock2".equalsIgnoreCase(demoType)) {
            RemoteComposeWriter writer = FancyClocks.fancyClock2();
            buffer = Arrays.copyOf(writer.buffer(), writer.bufferSize());
            docName = "fancyClock2.rc";
        } else if ("particlesphere".equalsIgnoreCase(demoType)) {
            RemoteComposeContext ctx = ParticleSphereKt.particleSphere();
            RemoteComposeWriter writer = ctx.mRemoteWriter;
            buffer = Arrays.copyOf(writer.buffer(), writer.bufferSize());
            docName = "particleSphere.rc";
        } else if ("particledots".equalsIgnoreCase(demoType)) {
            RemoteComposeContext ctx = ParticleDotsKt.particleDots();
            RemoteComposeWriter writer = ctx.mRemoteWriter;
            buffer = Arrays.copyOf(writer.buffer(), writer.bufferSize());
            docName = "particleDots.rc";
        } else if ("fireworks2".equalsIgnoreCase(demoType)) {
            RemoteComposeContext ctx = Particles3Kt.fireworks2();
            RemoteComposeWriter writer = ctx.mRemoteWriter;
            buffer = Arrays.copyOf(writer.buffer(), writer.bufferSize());
            docName = "fireworks2.rc";
        } else if ("pmaze2".equalsIgnoreCase(demoType)) {
            RemoteComposeWriter writer = DemoParticlesKt.pmaze2();
            buffer = Arrays.copyOf(writer.buffer(), writer.bufferSize());
            docName = "pmaze2.rc";
        } else if ("flappydroid".equalsIgnoreCase(demoType) || "flappy".equalsIgnoreCase(
                demoType)) {
            buffer = DslGameFlappyDroidKt.dslGameFlappyDroid();
            docName = "DslGameFlappyDroid.rc";
        } else if ("cubesphere2".equalsIgnoreCase(demoType)) {
            RemoteComposeContext ctx =
                    CubeSphereTransitionShader2Kt.createCubeSphereTransitionShader2();
            RemoteComposeWriter writer = ctx.mRemoteWriter;
            buffer = Arrays.copyOf(writer.buffer(), writer.bufferSize());
            docName = "CubeSphereTransitionShader2.rc";
        } else if ("cubesphere".equalsIgnoreCase(demoType)) {
            RemoteComposeContext ctx =
                    CubeSphereTransitionShaderKt.createCubeSphereTransitionShader();
            RemoteComposeWriter writer = ctx.mRemoteWriter;
            buffer = Arrays.copyOf(writer.buffer(), writer.bufferSize());
            docName = "CubeSphereTransitionShader.rc";
        }

        try {
            File outFile = new File(getExternalFilesDir(null), docName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(buffer);
            }
            android.util.Log.i("ActivityLongRun",
                    "Saved RC doc to: " + outFile.getAbsolutePath() + " (" + buffer.length
                            + " bytes)");
        } catch (Exception e) {
            Log.e("ActivityLongRun", "Failed to save RC doc", e);
        }

        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(Color.WHITE);

        RemoteDocument rcd = new RemoteDocument(buffer);
        RemoteComposePlayer player = new RemoteComposePlayer(this);
        player.setCustomSupport(new AndroidCustomSupport(player));
        player.setShaderControl(new ShaderControl() {
            @Override
            public boolean isShaderValid(@NonNull String shader) {
                return true;
            }
        });
        int widthPixels = Resources.getSystem().getDisplayMetrics().widthPixels;
        int heightPixels = Resources.getSystem().getDisplayMetrics().heightPixels;
        int side = Math.min(widthPixels, heightPixels);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(side, side);
        params.gravity = Gravity.CENTER;
        player.setLayoutParams(params);

        frameLayout.addView(player);
        setContentView(frameLayout);

        player.addOnLayoutChangeListener(new android.view.View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(android.view.View v, int left, int top, int right,
                    int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left > 0 && bottom - top > 0) {
                    player.removeOnLayoutChangeListener(this);
                    player.setDocument(rcd);
                }
            }
        });
    }
}
