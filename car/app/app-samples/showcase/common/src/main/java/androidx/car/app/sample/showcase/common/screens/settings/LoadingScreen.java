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

package androidx.car.app.sample.showcase.common.screens.settings;

import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;

import org.jspecify.annotations.NonNull;

/** A class that provides a sample template for a loading screen */
public abstract class LoadingScreen {

    private LoadingScreen() {
    }

    /**
    * Returns a sample template to be used for loading a screen
    */
    public static @NonNull Template loadingScreenTemplate(@NonNull CarContext carContext) {
        return new MessageTemplate.Builder(
                carContext.getString(R.string.loading_screen))
                .setLoading(true)
                .setHeader(new Header.Builder().setStartHeaderAction(Action.BACK).build())
                .build();
    }
}
