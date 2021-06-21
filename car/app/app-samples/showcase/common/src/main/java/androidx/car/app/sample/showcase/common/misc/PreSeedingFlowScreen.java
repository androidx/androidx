/*
 * Copyright (C) 2021 The Android Open Source Project
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
import androidx.car.app.model.CarColor;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;

/**
 * A {@link Screen} to be used in a preseeding flow, which adds screens to the back stack on
 * startup.
 */
public class PreSeedingFlowScreen extends Screen {

    public PreSeedingFlowScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return new MessageTemplate.Builder(
                "This screen is displayed before the main screen to allow the app to"
                        + " perform tasks such as granting permissions.")
                .setHeaderAction(Action.APP_ICON)
                .addAction(
                        new Action.Builder()
                                .setBackgroundColor(CarColor.BLUE)
                                .setOnClickListener(
                                        ParkedOnlyOnClickListener.create(
                                                () -> {
                                                    // Finish the screen to go back to "home" but
                                                    // this is where the
                                                    // application start an on phone activity that
                                                    // will request a needed
                                                    // permission, or login.
                                                    finish();
                                                }))
                                .setTitle("Open On Phone")
                                .build())
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(getCarContext()::finishCarApp)
                                .setBackgroundColor(CarColor.RED)
                                .setTitle("Exit App")
                                .build())
                .build();
    }
}
