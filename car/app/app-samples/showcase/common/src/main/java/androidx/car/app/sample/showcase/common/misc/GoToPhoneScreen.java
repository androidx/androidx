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

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

/** A {@link Screen} for controlling UI for allowing user to go to the phone. */
public class GoToPhoneScreen extends Screen {
    public static final String PHONE_COMPLETE_ACTION = "ActionComplete";

    private boolean mIsPhoneFlowComplete;

    public GoToPhoneScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    /** Callback called after the phone flow is completed. */
    public void onPhoneFlowComplete() {
        mIsPhoneFlowComplete = true;
        invalidate();
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Pane.Builder pane = new Pane.Builder();
        if (mIsPhoneFlowComplete) {
            pane.addRow(new Row.Builder().setTitle("The phone task is now complete").build());
        } else {
            getCarContext()
                    .startActivity(
                            new Intent(getCarContext(), OnPhoneActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            pane.addRow(new Row.Builder().setTitle("Please continue on your phone").build());
        }

        return new PaneTemplate.Builder(pane.build())
                .setTitle("Go-to-Phone Screen")
                .setHeaderAction(BACK)
                .build();
    }
}
