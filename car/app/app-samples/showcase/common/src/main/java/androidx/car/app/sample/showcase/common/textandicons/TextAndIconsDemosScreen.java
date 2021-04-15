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

package androidx.car.app.sample.showcase.common.textandicons;

import static androidx.car.app.model.Action.BACK;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

/** Creates a screen that shows different types of texts and icons. */
public final class TextAndIconsDemosScreen extends Screen {
    public TextAndIconsDemosScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Colored Text Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new ColoredTextDemoScreen(getCarContext())))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Icons Demo")
                        .setOnClickListener(
                                () -> getScreenManager().push(new IconsDemoScreen(getCarContext())))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Content Provider Icons Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new ContentProviderIconsDemoScreen(
                                                                getCarContext())))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Rows with Text and Icons Demo")
                        .setOnClickListener(
                                () -> getScreenManager().push(new RowDemoScreen(getCarContext())))
                        .build());

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Text and Icons Demos")
                .setHeaderAction(BACK)
                .build();
    }
}
