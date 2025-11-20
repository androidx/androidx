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

package androidx.wear.protolayout.material3.test;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.wear.protolayout.material3.RunnerUtils.SCREEN_SIZE_SMALL;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;

import androidx.wear.protolayout.LayoutElementBuilders.Layout;
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId;
import androidx.wear.protolayout.ResourceBuilders.ImageResource;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.renderer.ProtoLayoutVisibilityState;
import androidx.wear.protolayout.renderer.impl.ProtoLayoutViewInstance;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("deprecation")
public class GoldenTestActivity extends Activity {

    /** Extra to be put in the intent if test should use RTL direction on parent View. */
    public static final String USE_RTL_DIRECTION = "using_rtl";

    private static final String ICON_ID = "icon";
    private static final String ICON_ID_SMALL = "icon_small";
    private static final String AVATAR = "avatar_image";
    private static final int TOP_PADDING = 400;
    public static String VIEW_TAG = "ProtoLayout";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        byte[] layoutPayload = extras.getByteArray("layout");
        Layout layout = Layout.fromByteArray(layoutPayload);

        Context appContext = getApplicationContext();
        LinearLayout root = new LinearLayout(appContext);
        root.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        root.setPadding(0, TOP_PADDING, 0, 0);
        FrameLayout plView = new FrameLayout(appContext);
        plView.setBackgroundColor(Color.BLACK);
        plView.setLayoutParams(new LayoutParams(SCREEN_SIZE_SMALL, SCREEN_SIZE_SMALL));
        root.addView(plView);
        boolean isRtlEnabled = extras.getBoolean(USE_RTL_DIRECTION);
        updateLanguage(this, isRtlEnabled);

        ListeningExecutorService mainExecutor = MoreExecutors.newDirectExecutorService();
        Resources resources = generateResources();

        ProtoLayoutViewInstance instance =
                new ProtoLayoutViewInstance(
                        new ProtoLayoutViewInstance.Config.Builder(
                                        appContext,
                                        mainExecutor,
                                        mainExecutor,
                                        "androidx.wear.tiles.extra.CLICKABLE_ID")
                                .build());
        instance.setLayoutVisibility(ProtoLayoutVisibilityState.VISIBILITY_STATE_FULLY_VISIBLE);

        try {
            instance.renderAndAttach(checkNotNull(layout).toProto(), resources.toProto(), plView)
                    .get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        View firstChild = plView.getChildAt(0);

        // Simulate what the thing outside the renderer should do. Center the contents.
        LayoutParams layoutParams = (LayoutParams) firstChild.getLayoutParams();
        layoutParams.gravity = Gravity.CENTER;

        setContentView(root);
        plView.setContentDescription(VIEW_TAG);
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

    /** Sets language for device to be LTR or RTL. */
    private static void updateLanguage(@NonNull Context context, boolean isRtlDirection) {
        String languageToLoad = isRtlDirection ? "fa" : "en"; // fa = Persian, en = English
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        context.getResources()
                .updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
}
