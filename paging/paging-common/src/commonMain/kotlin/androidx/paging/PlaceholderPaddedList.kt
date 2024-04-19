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

package androidx.paging

/**
 * Interface to paged list that could contain placeholders.
 *
 * Contains a paged list's snapshot state. For example, in the context of
 * [PagingDataEvent.Refresh] exposed by [PagingDataPresenter], each [PlaceholderPaddedList]
 * represents a generation of paged data whereby a new generation is distinguished with
 * a refresh load.
 */
public interface PlaceholderPaddedList<T> {
    public val placeholdersBefore: Int
    public val placeholdersAfter: Int
    public val size: Int
    public val dataCount: Int
    public fun getItem(index: Int): T
}
