/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.integration.testapp.vo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A unmodifiable POJO that comes from a library.
 */
public class LibraryPojo {

    private final JSONObject mJsonObj = new JSONObject();

    public Long getPrice() {
        return mJsonObj.optLong("price");
    }

    public Long getId() {
        return mJsonObj.optLong("id");
    }

    public String getName() {
        return mJsonObj.optString("name");
    }

    public void setPrice(Long price) {
        try {
            mJsonObj.put("price", price);
        } catch (JSONException e) {
            // ignored
        }
    }

    public void setId(long id) {
        try {
            mJsonObj.put("id", id);
        } catch (JSONException e) {
            // ignored
        }
    }

    public void setName(String name) {
        try {
            mJsonObj.put("name", name);
        } catch (JSONException e) {
            // ignored
        }
    }
}
