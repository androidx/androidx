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

package androidx.car.app.sample.showcase.common;

import static androidx.car.app.model.Action.BACK;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;
import androidx.core.graphics.drawable.IconCompat;

/** Screen for demonstrating task flow limitations. */
public final class TaskRestrictionDemoScreen extends Screen {

    private static final int MAX_STEPS_ALLOWED = 4;

    private final int mStep;
    private boolean mIsBackOperation = false;
    private boolean mToggleState = false;
    private int mImageType = Row.IMAGE_TYPE_ICON;

    public TaskRestrictionDemoScreen(int step, @NonNull CarContext carContext) {
        super(carContext);

        this.mStep = step;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        // Last step must either be a PaneTemplate, MessageTemplate or NavigationTemplate.
        if (mStep == MAX_STEPS_ALLOWED) {
            OnClickListener onClickListener = () ->
                    getScreenManager()
                            .pushForResult(
                                    new TaskRestrictionDemoScreen(
                                            mStep + 1,
                                            getCarContext()),
                                    result ->
                                            mIsBackOperation = true);

            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.task_limit_reached_msg))
                    .setHeaderAction(BACK)
                    .addAction(
                            new Action.Builder()
                                    .setTitle(getCarContext().getString(
                                            R.string.try_anyway_action_title))
                                    .setOnClickListener(onClickListener)
                                    .build())
                    .build();
        }

        ItemList.Builder builder = new ItemList.Builder();
        builder.addItem(
                        new Row.Builder()
                                .setTitle(getCarContext().getString(R.string.task_step_of_title,
                                        mStep,
                                        MAX_STEPS_ALLOWED))
                                .addText(getCarContext().getString(R.string.task_step_of_text))
                                .setOnClickListener(
                                        () ->
                                                getScreenManager()
                                                        .pushForResult(
                                                                new TaskRestrictionDemoScreen(
                                                                        mStep + 1, getCarContext()),
                                                                result -> mIsBackOperation = true))
                                .build())
                .addItem(
                        new Row.Builder()
                                .setTitle(getCarContext().getString(R.string.toggle_test_title))
                                .addText(getCarContext().getString(R.string.toggle_test_text))
                                .setToggle(
                                        new Toggle.Builder(
                                                checked -> {
                                                    mToggleState = !mToggleState;
                                                    invalidate();
                                                })
                                                .setChecked(mToggleState)
                                                .build())
                                .build())
                .addItem(
                        new Row.Builder()
                                .setTitle(getCarContext().getString(R.string.image_test_title))
                                .addText(getCarContext().getString(R.string.image_test_text))
                                .setImage(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        getCarContext(),
                                                        R.drawable.ic_fastfood_white_48dp))
                                                .build(),
                                        mImageType)
                                .setOnClickListener(
                                        () -> {
                                            mImageType =
                                                    mImageType == Row.IMAGE_TYPE_ICON
                                                            ? Row.IMAGE_TYPE_LARGE
                                                            : Row.IMAGE_TYPE_ICON;
                                            invalidate();
                                        })
                                .build());

        if (mIsBackOperation) {
            builder.addItem(
                    new Row.Builder()
                            .setTitle(getCarContext().getString(R.string.additional_data_title))
                            .addText(getCarContext().getString(R.string.additional_data_text))
                            .build());
        }

        return new ListTemplate.Builder()
                .setSingleList(builder.build())
                .setTitle(getCarContext().getString(R.string.task_restriction_demo_title))
                .setHeaderAction(BACK)
                .setActionStrip(
                        new ActionStrip.Builder()
                                .addAction(
                                        new Action.Builder()
                                                .setTitle(getCarContext().getString(
                                                        R.string.home_caps_action_title))
                                                .setOnClickListener(
                                                        () -> getScreenManager().popToRoot())
                                                .build())
                                .build())
                .build();
    }
}
