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

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

/**
 * A {@link Screen} that allows you to push deeper in the screen stack, or pop to previous marker,
 * or pop to the root {@link Screen}.
 */
public class PopToDemoScreen extends Screen {
    private final int mId;

    public PopToDemoScreen(@NonNull CarContext carContext, int id) {
        super(carContext);
        this.mId = id;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Pop to root")
                        .setOnClickListener(() -> getScreenManager().popToRoot())
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Pop to Misc Demo Marker")
                        .setOnClickListener(() -> getScreenManager().popTo(MiscDemoScreen.MARKER))
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Push further in stack")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new PopToDemoScreen(
                                                                getCarContext(), mId + 1)))
                        .build());

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Pop To " + mId)
                .setHeaderAction(BACK)
                .build();
    }
}
