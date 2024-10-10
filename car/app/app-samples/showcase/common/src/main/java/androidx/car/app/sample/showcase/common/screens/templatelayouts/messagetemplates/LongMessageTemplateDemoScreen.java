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
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Header;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.versioning.CarAppApiLevels;

import org.jspecify.annotations.NonNull;

/** A screen that demonstrates the long message template. */
public class LongMessageTemplateDemoScreen extends Screen {
    public LongMessageTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        if (getCarContext().getCarAppApiLevel() < CarAppApiLevels.LEVEL_2) {
            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.long_msg_template_not_supported_text))
                    .setHeader(new Header.Builder().setTitle(getCarContext()
                            .getString(R.string.long_msg_template_not_supported_title))
                            .setStartHeaderAction(BACK).build())
                    .build();
        }

        Action.Builder primaryActionBuilder = new Action.Builder()
                .setOnClickListener(
                        ParkedOnlyOnClickListener.create(() -> {
                            getScreenManager().pop();
                            CarToast.makeText(
                                    getCarContext(),
                                    getCarContext().getString(R.string.primary_action_title),
                                    LENGTH_LONG
                            ).show();
                        }))
                .setTitle(getCarContext().getString(R.string.accept_action_title));
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_4) {
            primaryActionBuilder.setFlags(FLAG_PRIMARY);
        }

        return new LongMessageTemplate.Builder(
                getCarContext().getString(R.string.long_msg_template_text))
                .setTitle(getCarContext().getString(R.string.long_msg_template_demo_title))
                .setHeaderAction(BACK)
                .addAction(primaryActionBuilder.build())
                .addAction(new Action.Builder()
                        .setBackgroundColor(CarColor.RED)
                        .setOnClickListener(
                                ParkedOnlyOnClickListener.create(() -> getScreenManager().pop()))
                        .setTitle(getCarContext().getString(R.string.reject_action_title))
                        .build())
                .setActionStrip(new ActionStrip.Builder()
                        .addAction(new Action.Builder()
                                .setTitle(getCarContext().getString(R.string.more_action_title))
                                .setOnClickListener(
                                        () ->
                                                CarToast.makeText(
                                                                getCarContext(),
                                                                getCarContext().getString(
                                                                        R.string.more_toast_msg),
                                                                LENGTH_LONG)
                                                        .show())
                                .build())
                        .build())
                .build();
    }
}
