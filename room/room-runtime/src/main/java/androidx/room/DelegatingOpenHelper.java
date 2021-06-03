/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.room;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

/**
 * Package private interface for OpenHelpers which delegate to other open helpers.
 *
 * TODO(b/175612939): delete this interface once implementations are merged.
 */
interface DelegatingOpenHelper {

    /**
     * Returns the delegate open helper (which may itself be a DelegatingOpenHelper) so
     * configurations on specific instances can be applied.
     *
     * @return the delegate
     */
    @NonNull
    SupportSQLiteOpenHelper getDelegate();
}
