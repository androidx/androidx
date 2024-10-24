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

package androidx.car.app.sample.showcase.common.screens.templatelayouts;

import static androidx.car.app.model.Action.BACK;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.messagetemplates.LongMessageTemplateDemoScreen;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.messagetemplates.ShortMessageTemplateDemoScreen;
import androidx.lifecycle.DefaultLifecycleObserver;

import org.jspecify.annotations.NonNull;

/**
 * Creates a screen that demonstrates usage of the full screen {@link ListTemplate} to display a
 * full-screen list.
 */
public final class MessageTemplateDemoScreen extends Screen implements DefaultLifecycleObserver {

    public MessageTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(buildRowForTemplate(new ShortMessageTemplateDemoScreen(getCarContext()),
                R.string.short_msg_template_demo_title));
        listBuilder.addItem(buildRowForTemplate(new LongMessageTemplateDemoScreen(getCarContext()),
                R.string.long_msg_template_demo_title));
        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.msg_template_demo_title))
                        .setStartHeaderAction(BACK)
                        .build())
                .build();
    }

    private Row buildRowForTemplate(Screen screen, int title) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setOnClickListener(() -> getScreenManager().push(screen))
                .setBrowsable(true)
                .build();
    }
}
