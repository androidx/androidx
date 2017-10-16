/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.util.paging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TestDataSource extends DataSource<String, User> {
    static final ArrayList<User> sUsers = new ArrayList<>();
    static {
        for (int i = 0; i < 300; i++) {
            sUsers.add(new User(String.format("%03d", i), "Usernr " + i));
        }
    }

    static void verifyRange(PagedList<User> list, int start, int end) {
        assertEquals(end - start, list.size());
        for (int i = 0; i < list.size(); i++) {
            // NOTE: avoid getter, to avoid signaling
            assertSame(sUsers.get(start + i), list.mItems.get(i));
        }
    }

    @Override
    public String getKey(@NonNull User item) {
        return item.name;
    }

    @Nullable
    @Override
    public List<User> loadAfterInitial(@Nullable String itemName, int pageSize) {
        if (itemName == null) {
            itemName = "";
        }
        ArrayList<User> users = new ArrayList<>();
        int index;
        for (index = 0; index < sUsers.size(); index++) {
            if (itemName.compareTo(sUsers.get(index).name) < 0) {
                break;
            }
        }
        for (int i = 0; i < pageSize && index + i < sUsers.size(); i++) {
            users.add(sUsers.get(index + i));
        }
        return users;
    }

    @Nullable
    @Override
    public List<User> loadAfter(@NonNull User currentEndItem, int pageSize) {
        return loadAfterInitial(currentEndItem.name, pageSize);
    }

    @Nullable
    @Override
    public List<User> loadBefore(@NonNull User currentBeginItem, int pageSize) {
        ArrayList<User> users = new ArrayList<>();
        int index;
        for (index = sUsers.size() - 1; index >= 0; index--) {
            if (currentBeginItem.name.compareTo(sUsers.get(index).name) > 0) {
                break;
            }
        }
        for (int i = 0; i < pageSize && index - i >= 0; i++) {
            users.add(sUsers.get(index - i));
        }
        return users;
    }
}
