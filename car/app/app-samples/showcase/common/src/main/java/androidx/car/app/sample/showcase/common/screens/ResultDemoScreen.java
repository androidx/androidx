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

package androidx.car.app.sample.showcase.common.screens;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;

import org.jspecify.annotations.NonNull;

/**
 * A screen to demo the use of {@link CarContext#setCarAppResult(int, android.content.Intent)}
 */
public class ResultDemoScreen extends Screen {
    public ResultDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ComponentName callingComponent = getCarContext().getCallingComponent();
        if (callingComponent == null) {
            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.not_started_for_result_msg))
                    .setHeader(new Header.Builder().setStartHeaderAction(Action.BACK)
                            .setTitle(getCarContext().getString(R.string.result_demo_title))
                            .build())
                    .build();
        }

        return new MessageTemplate.Builder(
                getCarContext().getString(R.string.started_for_result_msg,
                        callingComponent.getPackageName()))
                .setHeader(new Header.Builder().setStartHeaderAction(Action.BACK)
                        .setTitle(getCarContext().getString(R.string.result_demo_title))
                        .build())
                .addAction(new Action.Builder()
                        .setTitle("Okay (action = 'foo')")
                        .setOnClickListener(() -> {
                            getCarContext().setCarAppResult(Activity.RESULT_OK,
                                    new Intent("foo"));
                            getCarContext().finishCarApp();
                        })
                        .build())
                .addAction(new Action.Builder()
                        .setTitle(getCarContext().getString(R.string.cancel_action_title))
                        .setOnClickListener(() -> {
                            getCarContext().setCarAppResult(Activity.RESULT_CANCELED, null);
                            getCarContext().finishCarApp();
                        })
                        .build())
                .build();
    }
}
