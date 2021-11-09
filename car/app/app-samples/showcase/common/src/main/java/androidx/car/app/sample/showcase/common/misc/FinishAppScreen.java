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

import static androidx.car.app.model.Action.BACK;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.ShowcaseService;

/** A {@link Screen} that provides an action to exit the car app. */
public class FinishAppScreen extends Screen {
    protected FinishAppScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return new MessageTemplate.Builder(
                "This will finish the app, and when you return it will pre-seed a permission "
                        + "screen")
                .setTitle("Finish App Demo")
                .setHeaderAction(BACK)
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(
                                        () -> {
                                            getCarContext()
                                                    .getSharedPreferences(
                                                            ShowcaseService.SHARED_PREF_KEY,
                                                            Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putBoolean(
                                                            ShowcaseService.PRE_SEED_KEY, true)
                                                    .apply();
                                            getCarContext().finishCarApp();
                                        })
                                .setTitle("Exit")
                                .build())
                .build();
    }
}
