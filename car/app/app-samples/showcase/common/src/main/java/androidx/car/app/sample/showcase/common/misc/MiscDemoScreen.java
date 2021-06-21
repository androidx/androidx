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
import androidx.car.app.sample.showcase.common.ShowcaseSession;

/** Creates a screen that has an assortment of API demos. */
public final class MiscDemoScreen extends Screen {
    static final String MARKER = "MiscDemoScreen";
    @NonNull private final ShowcaseSession mShowcaseSession;

    public MiscDemoScreen(@NonNull CarContext carContext,
            @NonNull ShowcaseSession showcaseSession) {
        super(carContext);
        setMarker(MARKER);
        mShowcaseSession = showcaseSession;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Notification Demo")
                        .setOnClickListener(() -> getScreenManager().push(
                                new NotificationDemoScreen(getCarContext())))
                        .setBrowsable(true)
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("PopTo Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new PopToDemoScreen(getCarContext(), 0)))
                        .setBrowsable(true)
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Loading Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new LoadingDemoScreen(getCarContext())))
                        .setBrowsable(true)
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Request Permission Demo")
                        .setOnClickListener(() ->
                                getScreenManager().push(
                                        new RequestPermissionScreen(getCarContext())))
                        .setBrowsable(true)
                        .build());


        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Pre-seed the Screen backstack on next run Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new FinishAppScreen(
                                                                getCarContext())))
                        .setBrowsable(true)
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Car Hardware Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new CarHardwareDemoScreen(
                                                                getCarContext(), mShowcaseSession)))
                        .setBrowsable(true)
                        .build());

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Misc Demos")
                .setHeaderAction(BACK)
                .build();
    }
}
