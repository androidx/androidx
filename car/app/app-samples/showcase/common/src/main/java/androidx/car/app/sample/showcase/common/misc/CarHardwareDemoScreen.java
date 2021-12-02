/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.misc;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.ShowcaseSession;
import androidx.car.app.sample.showcase.common.renderer.CarHardwareRenderer;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/** Simple demo of how access car hardware information. */
public final class CarHardwareDemoScreen extends Screen {

    final CarHardwareRenderer mCarHardwareRenderer;

    public CarHardwareDemoScreen(@NonNull CarContext carContext,
            @NonNull ShowcaseSession showcaseSession) {
        super(carContext);
        mCarHardwareRenderer = new CarHardwareRenderer(carContext);
        Lifecycle lifecycle = getLifecycle();
        lifecycle.addObserver(new DefaultLifecycleObserver() {

            @NonNull
            final ShowcaseSession mShowcaseSession = showcaseSession;

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                // When this screen is visible set the SurfaceRenderer to show
                // CarHardware information.
                mShowcaseSession.overrideRenderer(mCarHardwareRenderer);
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                // When this screen is hidden set the SurfaceRenderer to show
                // CarHardware information.
                mShowcaseSession.overrideRenderer(null);
            }
        });
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ActionStrip actionStrip =
                new ActionStrip.Builder()
                        // Add a Button to show the CarHardware info screen
                        .addAction(new Action.Builder()
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        getCarContext(),
                                                        R.drawable.info_gm_grey_24dp))
                                                .build())
                                .setOnClickListener(() -> getScreenManager().push(
                                        new CarHardwareInfoScreen(getCarContext())))
                                .build())
                        .addAction(
                                new Action.Builder()
                                        .setTitle("BACK")
                                        .setOnClickListener(this::finish)
                                        .build())
                        .build();

        return new NavigationTemplate.Builder().setActionStrip(actionStrip).build();
    }
}
