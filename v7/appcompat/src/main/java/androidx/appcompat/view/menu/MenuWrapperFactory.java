/*
 * Copyright (C) 2012 The Android Open Source Project
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

package androidx.appcompat.view.menu;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.RestrictTo;
import androidx.core.internal.view.SupportMenu;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.internal.view.SupportSubMenu;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class MenuWrapperFactory {
    private MenuWrapperFactory() {
    }

    public static Menu wrapSupportMenu(Context context, SupportMenu supportMenu) {
        return new MenuWrapperICS(context, supportMenu);
    }

    public static MenuItem wrapSupportMenuItem(Context context, SupportMenuItem supportMenuItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return new MenuItemWrapperJB(context, supportMenuItem);
        } else {
            return new MenuItemWrapperICS(context, supportMenuItem);
        }
    }

    public static SubMenu wrapSupportSubMenu(Context context, SupportSubMenu supportSubMenu) {
        return new SubMenuWrapperICS(context, supportSubMenu);
    }
}
