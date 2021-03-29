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

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import java.io.IOException;

/** Creates a screen that demonstrate the image loading in the library using a content provider. */
public final class ContentProviderIconsDemoScreen extends Screen {
    private static final String ANDROID_AUTO_PACKAGE_NAME =
            "com.google.android.projection.gearhead";
    private static final String FILE_PROVIDER_AUTHORITY = "com.showcase.fileprovider";

    private static final int[] ICON_DRAWABLES = {
            R.drawable.arrow_right_turn, R.drawable.arrow_straight, R.drawable.ic_i5,
            R.drawable.ic_520
    };

    public ContentProviderIconsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        for (int i = 0; i < ICON_DRAWABLES.length; i++) {
            int resId = ICON_DRAWABLES[i];
            listBuilder.addItem(
                    new Row.Builder()
                            .setImage(
                                    new CarIcon.Builder(
                                            IconCompat.createWithContentUri(
                                                    customIconUri(resId)))
                                            .build())
                            .setTitle("Icon " + i)
                            .build());
        }

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Content Provider Icons Demo")
                .setHeaderAction(BACK)
                .build();
    }

    @Nullable
    @SuppressWarnings("CatchAndPrintStackTrace")
    private Uri customIconUri(int resId) {
        Uri uri = null;
        try {
            uri =
                    DelayedFileProvider.getUriForResource(
                            getCarContext(), FILE_PROVIDER_AUTHORITY, resId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // FileProvider requires the app to grant temporary access to Android Auto for the file. A
        // URI
        // from a content provider may not need to do this if its contents are public.
        getCarContext()
                .grantUriPermission(
                        ANDROID_AUTO_PACKAGE_NAME, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return uri;
    }
}
