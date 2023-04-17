/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.tiles.material.testapp;

import static androidx.wear.tiles.material.RunnerUtils.SCREEN_HEIGHT;
import static androidx.wear.tiles.material.RunnerUtils.SCREEN_WIDTH;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.Layout;
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId;
import androidx.wear.protolayout.ResourceBuilders.ImageResource;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement;
import androidx.wear.tiles.material.R;
import androidx.wear.tiles.renderer.TileRenderer;

import java.util.concurrent.Executor;

public class GoldenTestActivity extends Activity {
    private static final String ICON_ID = "tile_icon";
    private static final String AVATAR = "avatar_image";

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        byte[] layoutPayload = getIntent().getExtras().getByteArray("layout");

        LayoutElement layoutElementProto;
        try {
            layoutElementProto = LayoutElement.parseFrom(layoutPayload);
        } catch (Exception ex) {
            // It's a test, just rethrow.
            throw new IllegalArgumentException("Could not deserialize layout proto", ex);
        }

        LayoutElementBuilders.LayoutElement rootLayoutElement =
                LayoutElementBuilders.layoutElementFromProto(layoutElementProto);

        Context appContext = getApplicationContext();
        FrameLayout root = new FrameLayout(appContext);
        root.setBackgroundColor(Color.BLACK);
        root.setLayoutParams(new LayoutParams(SCREEN_WIDTH, SCREEN_HEIGHT));

        Layout layout = new Layout.Builder().setRoot(rootLayoutElement).build();

        Executor mainExecutor = ContextCompat.getMainExecutor(getApplicationContext());

        Resources resources = generateResources();
        TileRenderer renderer = new TileRenderer(appContext, mainExecutor, i -> {});

        View firstChild = renderer.inflate(layout, resources, root);

        // Simulate what the thing outside the renderer should do. Center the contents.
        LayoutParams layoutParams = (LayoutParams) firstChild.getLayoutParams();
        layoutParams.gravity = Gravity.CENTER;

        // Set the activity to be full screen so when we crop the Bitmap we don't get time bar etc.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow()
                .setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(root, new ViewGroup.LayoutParams(SCREEN_WIDTH, SCREEN_HEIGHT));
        super.onCreate(savedInstanceState);
    }

    private static Resources generateResources() {
        return new Resources.Builder()
                .addIdToImageMapping(
                        ICON_ID,
                        new ImageResource.Builder()
                                .setAndroidResourceByResId(
                                        new AndroidImageResourceByResId.Builder()
                                                .setResourceId(R.drawable.tile_icon)
                                                .build())
                                .build())
                .addIdToImageMapping(
                        AVATAR,
                        new ImageResource.Builder()
                                .setAndroidResourceByResId(
                                        new AndroidImageResourceByResId.Builder()
                                                .setResourceId(R.drawable.avatar)
                                                .build())
                                .build())
                .build();
    }
}
