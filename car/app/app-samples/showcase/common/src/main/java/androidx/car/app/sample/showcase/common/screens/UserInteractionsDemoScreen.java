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

import static androidx.car.app.model.Action.BACK;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.Item;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.audio.VoiceInteraction;
import androidx.car.app.sample.showcase.common.screens.userinteractions.RequestPermissionMenuDemoScreen;
import androidx.car.app.sample.showcase.common.screens.userinteractions.TaskOverflowDemoScreen;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** A screen demonstrating User Interactions */
public final class UserInteractionsDemoScreen extends Screen {
    private static final int MAX_STEPS_ALLOWED = 4;

    private final int mStep;
    private boolean mIsBackOperation;

    public UserInteractionsDemoScreen(int step, @NonNull CarContext carContext) {
        super(carContext);
        this.mStep = step;
    }

    @Override
    public @NonNull Template onGetTemplate() {

        // Last step must either be a PaneTemplate, MessageTemplate or NavigationTemplate.
        if (mStep == MAX_STEPS_ALLOWED) {
            return templateForTaskLimitReached();
        }

        ItemList.Builder builder = new ItemList.Builder();

        builder.addItem(buildRowForVoiceInteractionDemo());
        builder.addItem(buildRowForRequestPermissionsDemo());
        builder.addItem(buildRowForTaskRestrictionDemo());

        if (mIsBackOperation) {
            builder.addItem(buildRowForAdditionalData());
        }

        return new ListTemplate.Builder()
                .setSingleList(builder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.user_interactions_demo_title))
                        .setStartHeaderAction(BACK)
                        .addEndHeaderAction(new Action.Builder()
                                .setTitle(getCarContext().getString(
                                        R.string.home_caps_action_title))
                                .setOnClickListener(
                                        () -> getScreenManager().popToRoot())
                                .build())
                        .build())
                .build();

    }

    /**
     * Returns the row for VoiceInteraction Demo
     */
    private Item buildRowForVoiceInteractionDemo() {
        return new Row.Builder()
                .setTitle(getCarContext().getString(R.string.voice_access_demo_title))
                .setImage(new CarIcon.Builder(
                        IconCompat.createWithResource(
                                getCarContext(),
                                R.drawable.ic_mic))
                        .build(), Row.IMAGE_TYPE_ICON)
                .setOnClickListener(new VoiceInteraction(getCarContext())::voiceInteractionDemo)
                .build();
    }

    /**
     * Returns the row for TaskRestriction Demo
     */
    private Item buildRowForTaskRestrictionDemo() {
        boolean isInOverflow = mStep >= MAX_STEPS_ALLOWED;
        String title = isInOverflow ? getCarContext().getString(
                R.string.application_overflow_title) :
                getCarContext().getString(R.string.task_step_of_title, mStep,
                        MAX_STEPS_ALLOWED);
        String subTitle = isInOverflow
                ?
                getCarContext().getString(R.string.task_step_of_title, mStep,
                        MAX_STEPS_ALLOWED) :
                getCarContext().getString(R.string.task_step_of_text);
        return new Row.Builder()
                .setTitle(title)
                .addText(subTitle)
                .setImage(new CarIcon.Builder(
                        IconCompat.createWithResource(
                                getCarContext(), R.drawable.baseline_task_24))
                        .build(), Row.IMAGE_TYPE_ICON)
                .setOnClickListener(
                        () -> {
                            if (mStep < MAX_STEPS_ALLOWED) {
                                getScreenManager()
                                        .pushForResult(
                                                new UserInteractionsDemoScreen(
                                                        mStep + 1, getCarContext()),
                                                result -> mIsBackOperation = true);
                            } else {
                                getScreenManager()
                                        .pushForResult(
                                                new TaskOverflowDemoScreen(
                                                        getCarContext()),
                                                result -> mIsBackOperation = true);
                            }
                        }
                )
                .build();
    }

    /**
     * Returns the row for RequestPermissions Demo
     */
    private Item buildRowForRequestPermissionsDemo() {
        return new Row.Builder()
                .setTitle(getCarContext().getString(
                        R.string.request_permission_menu_demo_title))
                .setImage(new CarIcon.Builder(
                        IconCompat.createWithResource(
                                getCarContext(),
                                R.drawable.baseline_question_mark_24))
                        .build(), Row.IMAGE_TYPE_ICON)
                .setOnClickListener(() -> getScreenManager().push(
                        new RequestPermissionMenuDemoScreen(getCarContext())))
                .build();
    }

    /**
     * Returns the row for AdditionalData
     */
    private Item buildRowForAdditionalData() {
        return new Row.Builder()
                .setTitle(getCarContext().getString(R.string.additional_data_title))
                .addText(getCarContext().getString(R.string.additional_data_text))
                .build();
    }

    /**
     * Returns the MessageTemplate with "Task Limit Reached" message after user completes 4 task
     * steps
     */
    private MessageTemplate templateForTaskLimitReached() {
        OnClickListener onClickListener = () ->
                getScreenManager()
                        .pushForResult(
                                new UserInteractionsDemoScreen(
                                        mStep + 1,
                                        getCarContext()),
                                result ->
                                        mIsBackOperation = true);

        return new MessageTemplate.Builder(
                getCarContext().getString(R.string.task_limit_reached_msg))
                .setHeader(new Header.Builder().setTitle(getCarContext()
                                .getString(R.string.latest_feature_title))
                        .setStartHeaderAction(Action.BACK).build())
                .addAction(
                        new Action.Builder()
                                .setTitle(getCarContext().getString(
                                        R.string.try_anyway_action_title))
                                .setOnClickListener(onClickListener)
                                .build())
                .build();
    }

}
