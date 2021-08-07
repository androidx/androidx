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
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

/**
 * A {@link Screen} that shows examples on how to query for various content limits via the
 * {@lnk ConstraintManager} API.
 */
public class ContentLimitsDemoScreen extends Screen {

    public ContentLimitsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ConstraintManager manager = getCarContext().getCarService(ConstraintManager.class);
        ItemList.Builder listBuilder = new ItemList.Builder();
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("List Limit")
                        .addText(Integer.toString(manager.getContentLimit(
                                ConstraintManager.CONTENT_LIMIT_TYPE_LIST)))
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Grid Limit")
                        .addText(Integer.toString(manager.getContentLimit(
                                ConstraintManager.CONTENT_LIMIT_TYPE_GRID)))
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Pane Limit")
                        .addText(Integer.toString(manager.getContentLimit(
                                ConstraintManager.CONTENT_LIMIT_TYPE_PANE)))
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Place List Limit")
                        .addText(Integer.toString(manager.getContentLimit(
                                ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)))
                        .build());
        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Route List Limit")
                        .addText(Integer.toString(manager.getContentLimit(
                                ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST)))
                        .build());


        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Content Limits")
                .setHeaderAction(BACK)
                .build();
    }
}
