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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates;

import android.net.Uri;

import androidx.car.app.CarContext;
import androidx.car.app.HostInfo;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Creates a screen that demonstrate the image loading in the library using a content provider. */
public final class ContentProviderIconsDemoScreen extends Screen {
    private static final int[] ICON_DRAWABLES = {
            R.drawable.arrow_right_turn, R.drawable.arrow_straight, R.drawable.ic_i5,
            R.drawable.ic_520
    };
    private final @Nullable String mHostPackageName;

    public ContentProviderIconsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);

        HostInfo hostInfo = carContext.getHostInfo();
        mHostPackageName = hostInfo == null ? null : hostInfo.getPackageName();
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        String hostPackageName = mHostPackageName;
        if (hostPackageName == null) {
            // Cannot get the host package name, show an error message.
            listBuilder.setNoItemsMessage(
                    getCarContext().getString(R.string.images_unknown_host_error));
        } else {
            for (int i = 0; i < ICON_DRAWABLES.length; i++) {
                int resId = ICON_DRAWABLES[i];
                Uri uri = DelayedFileProvider.getUriForResource(getCarContext(), hostPackageName,
                        resId);
                listBuilder.addItem(
                        new Row.Builder()
                                .setImage(
                                        new CarIcon.Builder(
                                                IconCompat.createWithContentUri(uri))
                                                .build())
                                .setTitle(
                                        getCarContext().getString(R.string.icon_title_prefix) + " "
                                                + i)
                                .build());
            }
        }


        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext()
                                .getString(R.string.content_provider_icons_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .build())
                .build();
    }
}
