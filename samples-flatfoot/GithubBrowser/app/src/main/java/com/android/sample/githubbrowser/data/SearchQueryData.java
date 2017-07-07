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
package com.android.sample.githubbrowser.data;

import com.android.support.room.Entity;

/**
 * Contains information about locally persisted data on a single paginable query.
 */
@Entity(primaryKeys = {"searchQuery", "searchKind"})
public class SearchQueryData {
    public static final int GENERAL_REPOSITORIES = 0;
    public static final int REPOSITORY_CONTRIBUTORS = 1;

    public String searchQuery;
    public int searchKind;
    public long timestamp;
    public int indexOfLastFetchedPage;
    public int numberOfFetchedItems;
    public boolean hasNoMoreData;

    public SearchQueryData() {
    }
}
