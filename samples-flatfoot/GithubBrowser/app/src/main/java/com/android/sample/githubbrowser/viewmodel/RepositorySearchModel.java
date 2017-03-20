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
package com.android.sample.githubbrowser.viewmodel;

import android.text.TextUtils;

import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

/**
 * Model for the top-level search.
 */
public class RepositorySearchModel extends ViewModel {
    private LiveData<String> mSearchQuery = new LiveData<>();

    /**
     * Sets new search query. The second parameter should be used to specify whether
     * the currently set query should be overwritten.
     */
    public void setQuery(String query, boolean ignoreIfAlreadySet) {
        if (ignoreIfAlreadySet && !TextUtils.isEmpty(mSearchQuery.getValue())) {
            return;
        }

        mSearchQuery.setValue(query);
    }

    /**
     * Returns the {@LiveData} object that wraps the top-level search query.
     */
    public LiveData<String> getSearchQueryData() {
        return mSearchQuery;
    }
}
