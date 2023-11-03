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

package androidx.wear.protolayout.material.test;

import static androidx.wear.protolayout.material.RunnerUtils.SCREEN_HEIGHT;
import static androidx.wear.protolayout.material.RunnerUtils.SCREEN_WIDTH;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;

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
import androidx.wear.protolayout.LayoutElementBuilders.Layout;
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId;
import androidx.wear.protolayout.ResourceBuilders.ImageResource;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.renderer.impl.ProtoLayoutViewInstance;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@SuppressWarnings("deprecation")
public class GoldenTestActivity extends Activity {
    private static final String ICON_ID = "icon";
    private static final String ICON_ID_SMALL = "icon_small";
    private static final String AVATAR = "avatar_image";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        byte[] layoutPayload = getIntent().getExtras().getByteArray("layout");
        Layout layout = Layout.fromByteArray(layoutPayload);

        Context appContext = getApplicationContext();
        FrameLayout root = new FrameLayout(appContext);
        root.setBackgroundColor(Color.BLACK);
        root.setLayoutParams(new LayoutParams(SCREEN_WIDTH, SCREEN_HEIGHT));

        ListeningExecutorService mainExecutor = MoreExecutors.newDirectExecutorService();
        Resources resources = generateResources();

        ProtoLayoutViewInstance instance =
                new ProtoLayoutViewInstance(
                        new ProtoLayoutViewInstance.Config.Builder(
                                        appContext,
                                        mainExecutor,
                                        mainExecutor,
                                        "androidx.wear.tiles.extra.CLICKABLE_ID")
                                .setIsViewFullyVisible(true)
                                .build());

        instance.renderAndAttach(checkNotNull(layout).toProto(), resources.toProto(), root);

        View firstChild = root.getChildAt(0);

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
                                                .setResourceId(R.drawable.icon)
                                                .build())
                                .build())
                .addIdToImageMapping(
                        ICON_ID_SMALL,
                        new ImageResource.Builder()
                                .setAndroidResourceByResId(
                                        new AndroidImageResourceByResId.Builder()
                                                .setResourceId(R.drawable.icon_small)
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
