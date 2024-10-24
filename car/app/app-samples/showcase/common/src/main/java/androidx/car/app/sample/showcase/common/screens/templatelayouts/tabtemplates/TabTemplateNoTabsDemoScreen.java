/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.tabtemplates;

import static androidx.car.app.model.Action.APP_ICON;

import android.os.Handler;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.TabTemplate;
import androidx.car.app.model.Template;

import org.jspecify.annotations.NonNull;

/**
 * Creates a screen that demonstrates usage of the full screen {@link TabTemplate} without tabs.
 */
public final class TabTemplateNoTabsDemoScreen extends Screen {
    private TabTemplate.Builder mTabTemplateBuilder;

    public TabTemplateNoTabsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        mTabTemplateBuilder = new TabTemplate.Builder(new TabTemplate.TabCallback() {
            @Override
            public void onTabSelected(@NonNull String tabContentId) {
                // No-op
            }
        })
                .setHeaderAction(APP_ICON)
                .setLoading(true);

        new Handler(getCarContext().getMainLooper()).postDelayed(() -> {
            getScreenManager().pop();
            CarToast.makeText(getCarContext(), "Back to previous screen",
                    CarToast.LENGTH_SHORT).show();
        }, 5000);
        return mTabTemplateBuilder.build();
    }

}
