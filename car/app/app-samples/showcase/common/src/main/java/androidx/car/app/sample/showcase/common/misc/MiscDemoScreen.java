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

        listBuilder.addItem(createRow("Notification Demo",
                new NotificationDemoScreen(getCarContext())));
        listBuilder.addItem(createRow("PopTo Demo",
                new PopToDemoScreen(getCarContext())));
        listBuilder.addItem(createRow("Loading Demo",
                new LoadingDemoScreen(getCarContext())));
        listBuilder.addItem(createRow("Request Permission Demo",
                new RequestPermissionScreen(getCarContext())));
        listBuilder.addItem(createRow("Pre-seed the Screen backstack on next run Demo",
                new FinishAppScreen(getCarContext())));
        listBuilder.addItem(createRow("Car Hardware Demo",
                new CarHardwareDemoScreen(getCarContext(), mShowcaseSession)));

        ListTemplate.Builder builder = new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Misc Demos")
                .setHeaderAction(BACK);

        return builder.build();
    }

    private Row createRow(String title, Screen screen) {
        return new Row.Builder()
                .setTitle(title)
                .setOnClickListener(() -> getScreenManager().push(screen))
                .build();
    }
}
