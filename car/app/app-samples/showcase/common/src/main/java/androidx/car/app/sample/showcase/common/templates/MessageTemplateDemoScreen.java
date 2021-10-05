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

package androidx.car.app.sample.showcase.common.templates;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.model.Action.BACK;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

/** A screen that demonstrates the message template. */
public class MessageTemplateDemoScreen extends Screen {

    public MessageTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    // TODO(b/201548973): Remove this annotation once set/getFlags are ready
    @OptIn(markerClass = ExperimentalCarApi.class)
    public Template onGetTemplate() {
        return new MessageTemplate.Builder("Message goes here.\nMore text on second line.")
                .setTitle("Message Template Demo")
                .setIcon(
                        new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_emoji_food_beverage_white_48dp))
                                .setTint(CarColor.GREEN)
                                .build())
                .setHeaderAction(BACK)
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(() -> {
                                    CarToast.makeText(
                                            getCarContext(),
                                            "Clicked primary button",
                                            LENGTH_LONG
                                    ).show();
                                })
                                .setTitle("OK")
                                .setFlags(FLAG_PRIMARY)
                                .build())
                .addAction(
                        new Action.Builder()
                                .setBackgroundColor(CarColor.RED)
                                .setTitle("Throw")
                                .setOnClickListener(
                                        () -> {
                                            throw new RuntimeException("Error");
                                        })
                                .build())

                .setActionStrip(
                        new ActionStrip.Builder()
                                .addAction(
                                        new Action.Builder()
                                                .setTitle("Settings")
                                                .setOnClickListener(
                                                        () ->
                                                                CarToast.makeText(
                                                                        getCarContext(),
                                                                        "Clicked Settings",
                                                                        LENGTH_LONG)
                                                                        .show())
                                                .build())
                                .build())
                .build();
    }
}
