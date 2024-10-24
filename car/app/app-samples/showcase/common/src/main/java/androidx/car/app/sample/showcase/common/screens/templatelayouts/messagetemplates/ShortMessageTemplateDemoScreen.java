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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.messagetemplates;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.model.Action.BACK;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** A screen that demonstrates the message template. */
public class ShortMessageTemplateDemoScreen extends Screen {

    public ShortMessageTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        Action.Builder primaryActionBuilder = new Action.Builder()
                .setOnClickListener(() -> {
                    CarToast.makeText(
                            getCarContext(),
                            getCarContext().getString(R.string.primary_action_title),
                            LENGTH_LONG
                    ).show();
                })
                .setTitle(getCarContext().getString(R.string.ok_action_title));
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_4) {
            primaryActionBuilder.setFlags(FLAG_PRIMARY);
        }

        Action settings = new Action.Builder()
                .setTitle(getCarContext().getString(
                        R.string.settings_action_title))
                .setOnClickListener(
                        () -> CarToast.makeText(
                                        getCarContext(),
                                        getCarContext().getString(
                                                R.string.settings_toast_msg),
                                        LENGTH_LONG)
                                .show())
                .build();

        return new MessageTemplate.Builder(
                getCarContext().getString(R.string.msg_template_demo_text))
                .setHeader(new Header.Builder().setTitle(getCarContext()
                                .getString(R.string.msg_template_demo_title))
                        .setStartHeaderAction(BACK)
                        .addEndHeaderAction(settings).build())
                .setIcon(
                        new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_emoji_food_beverage_white_48dp))
                                .setTint(CarColor.GREEN)
                                .build())
                .addAction(primaryActionBuilder.build())
                .addAction(
                        new Action.Builder()
                                .setBackgroundColor(CarColor.RED)
                                .setTitle(getCarContext().getString(R.string.throw_action_title))
                                .setOnClickListener(
                                        () -> {
                                            throw new RuntimeException("Error");
                                        })
                                .build())
                .build();
    }
}
