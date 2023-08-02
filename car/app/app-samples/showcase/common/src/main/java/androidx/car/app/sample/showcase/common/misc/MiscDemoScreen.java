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
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.ShowcaseSession;

/** Creates a screen that has an assortment of API demos. */
public final class MiscDemoScreen extends Screen {
    static final String MARKER = "MiscDemoScreen";
    private static final int MAX_PAGES = 2;

    private final int mPage;

    @NonNull
    private final ShowcaseSession mShowcaseSession;

    public MiscDemoScreen(@NonNull CarContext carContext,
            @NonNull ShowcaseSession showcaseSession) {
        this(carContext, showcaseSession, 0);
    }

    public MiscDemoScreen(@NonNull CarContext carContext,
            @NonNull ShowcaseSession showcaseSession, int page) {
        super(carContext);
        setMarker(MARKER);
        mShowcaseSession = showcaseSession;
        mPage = page;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        switch (mPage) {
            case 0:
                listBuilder.addItem(createRow(getCarContext().getString(R.string.notification_demo),
                        new NotificationDemoScreen(getCarContext())));
                listBuilder.addItem(createRow(getCarContext().getString(R.string.pop_to_title),
                        new PopToDemoScreen(getCarContext())));
                listBuilder.addItem(
                        createRow(getCarContext().getString(R.string.loading_demo_title),
                                new LoadingDemoScreen(getCarContext())));
                listBuilder.addItem(
                        createRow(getCarContext().getString(R.string.request_permissions_title),
                                new RequestPermissionScreen(getCarContext())));
                listBuilder.addItem(
                        createRow(getCarContext().getString(R.string.finish_app_demo_title),
                                new FinishAppScreen(getCarContext())));
                listBuilder.addItem(
                        createRow(getCarContext().getString(R.string.car_hardware_demo_title),
                                new CarHardwareDemoScreen(getCarContext(), mShowcaseSession)));
                break;
            case 1:
                listBuilder.addItem(
                        createRow(getCarContext().getString(R.string.content_limits_demo_title),
                                new ContentLimitsDemoScreen(getCarContext())));
                listBuilder.addItem(createRow(getCarContext().getString(R.string.color_demo),
                        new ColorDemoScreen(getCarContext())));
                break;

        }

        ListTemplate.Builder builder = new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle(getCarContext().getString(R.string.misc_demo_title))
                .setHeaderAction(BACK);

        if (mPage + 1 < MAX_PAGES) {
            builder.setActionStrip(new ActionStrip.Builder()
                    .addAction(new Action.Builder()
                            .setTitle(getCarContext().getString(R.string.more_action_title))
                            .setOnClickListener(() -> {
                                getScreenManager().push(
                                        new MiscDemoScreen(getCarContext(), mShowcaseSession,
                                                mPage + 1));
                            })
                            .build())
                    .build());
        }

        return builder.build();
    }

    private Row createRow(String title, Screen screen) {
        return new Row.Builder()
                .setTitle(title)
                .setOnClickListener(() -> getScreenManager().push(screen))
                .build();
    }
}
