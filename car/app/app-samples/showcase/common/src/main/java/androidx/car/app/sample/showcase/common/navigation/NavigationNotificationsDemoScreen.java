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

package androidx.car.app.sample.showcase.common.navigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

/** A simple screen that demonstrates how to use navigation notifications in a car app. */
public final class NavigationNotificationsDemoScreen extends Screen {

    public NavigationNotificationsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    // Suppressing 'ObsoleteSdkInt' as this code is shared between APKs with different min SDK
    // levels
    @SuppressLint({"ObsoleteSdkInt"})
    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Start Notification")
                        .setOnClickListener(
                                () -> {
                                    Context context = getCarContext();
                                    Intent intent =
                                            new Intent(
                                                    context, NavigationNotificationService.class);
                                    if (VERSION.SDK_INT >= VERSION_CODES.O) {
                                        context.startForegroundService(intent);
                                    } else {
                                        context.startService(intent);
                                    }
                                })
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Stop Notification")
                        .setOnClickListener(
                                () ->
                                        getCarContext()
                                                .stopService(
                                                        new Intent(
                                                                getCarContext(),
                                                                NavigationNotificationService
                                                                        .class)))
                        .build());

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Navigation Notification Demo")
                .setHeaderAction(Action.BACK)
                .build();
    }
}
