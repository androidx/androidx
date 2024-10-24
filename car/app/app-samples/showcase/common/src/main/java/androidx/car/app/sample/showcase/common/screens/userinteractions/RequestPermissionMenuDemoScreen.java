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

package androidx.car.app.sample.showcase.common.screens.userinteractions;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.lifecycle.DefaultLifecycleObserver;

import org.jspecify.annotations.NonNull;

/** Screen to list different permission demos */
public final class RequestPermissionMenuDemoScreen extends Screen
        implements DefaultLifecycleObserver {

    public RequestPermissionMenuDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.request_permissions_title))
                        .setOnClickListener(() ->
                                getScreenManager().push(
                                        new RequestPermissionScreen(getCarContext())))
                        .setBrowsable(true)
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.preseed_permission_demo_title))
                        .setOnClickListener(() ->
                                getScreenManager().push(
                                        new PreSeedPermissionScreen(getCarContext())))
                        .setBrowsable(true)
                        .build());
        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext()
                                .getString(R.string.request_permission_menu_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .build())
                .build();
    }
}
