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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;

/**
 * A screen to demo the use of {@link CarContext#setCarAppResult(int, android.content.Intent)}
 */
public class ResultDemoScreen extends Screen {
    public ResultDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ComponentName callingComponent = getCarContext().getCallingComponent();
        if (callingComponent == null) {
            return new MessageTemplate.Builder("This app was not started for result")
                    .setTitle("Result demo")
                    .setHeaderAction(Action.BACK)
                    .build();
        }

        return new MessageTemplate.Builder("This app was called for result from "
                + callingComponent.getPackageName() + ". Please select the"
                + " result to send back to the caller")
                .setTitle("Result demo")
                .setHeaderAction(Action.BACK)
                .addAction(new Action.Builder()
                        .setTitle("Okay (action = 'foo')")
                        .setOnClickListener(() -> {
                            getCarContext().setCarAppResult(Activity.RESULT_OK,
                                    new Intent("foo"));
                            getCarContext().finishCarApp();
                        })
                        .build())
                .addAction(new Action.Builder()
                        .setTitle("Cancel")
                        .setOnClickListener(() -> {
                            getCarContext().setCarAppResult(Activity.RESULT_CANCELED, null);
                            getCarContext().finishCarApp();
                        })
                        .build())
                .build();
    }
}
