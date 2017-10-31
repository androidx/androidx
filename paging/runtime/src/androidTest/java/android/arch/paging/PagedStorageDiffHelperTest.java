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

package android.arch.paging;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.v7.recyclerview.extensions.DiffCallback;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@SmallTest
@RunWith(JUnit4.class)
public class PagedStorageDiffHelperTest {
    private interface CallbackValidator {
        void validate(ListUpdateCallback callback);
    }

    private static final DiffCallback<String> DIFF_CALLBACK = new DiffCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            // first char means same item
            return oldItem.charAt(0) == newItem.charAt(0);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    };

    public static Page<Integer, String> createPage(String... items) {
        return new Page<>(Arrays.asList(items));
    }

    private static void validateTwoListDiff(PagedStorage<?, String> oldList,
            PagedStorage<?, String> newList,
            CallbackValidator callbackValidator) {
        DiffUtil.DiffResult diffResult = PagedStorageDiffHelper.computeDiff(
                oldList, newList, DIFF_CALLBACK);

        ListUpdateCallback listUpdateCallback = mock(ListUpdateCallback.class);
        PagedStorageDiffHelper.dispatchDiff(listUpdateCallback, oldList, newList, diffResult);

        callbackValidator.validate(listUpdateCallback);
    }

    @Test
    public void sameListNoUpdates() {
        validateTwoListDiff(
                new PagedStorage<>(5, createPage("a", "b", "c"), 5),
                new PagedStorage<>(5, createPage("a", "b", "c"), 5),
                new CallbackValidator() {
                    @Override
                    public void validate(ListUpdateCallback callback) {
                        verifyZeroInteractions(callback);
                    }
                }
        );
    }

    @Test
    public void sameListNoUpdatesPlaceholder() {
        PagedStorage<Integer, String> storageNoPlaceholder =
                new PagedStorage<>(0, createPage("a", "b", "c"), 10);

        PagedStorage<Integer, String> storageWithPlaceholder =
                new PagedStorage<>(0, createPage("a", "b", "c"), 10);
        storageWithPlaceholder.allocatePlaceholders(3, 0, 3,
                /* ignored */ mock(PagedStorage.Callback.class));

        // even though one has placeholders, and null counts are different...
        assertEquals(10, storageNoPlaceholder.getTrailingNullCount());
        assertEquals(7, storageWithPlaceholder.getTrailingNullCount());

        // ... should be no interactions, since content still same
        validateTwoListDiff(
                storageNoPlaceholder,
                storageWithPlaceholder,
                new CallbackValidator() {
                    @Override
                    public void validate(ListUpdateCallback callback) {
                        verifyZeroInteractions(callback);
                    }
                }
        );
    }

    @Test
    public void appendFill() {
        validateTwoListDiff(
                new PagedStorage<>(5, createPage("a", "b"), 5),
                new PagedStorage<>(5, createPage("a", "b", "c"), 4),
                new CallbackValidator() {
                    @Override
                    public void validate(ListUpdateCallback callback) {
                        verify(callback).onRemoved(11, 1);
                        verify(callback).onInserted(7, 1);
                        // NOTE: ideally would be onChanged(7, 1, null)
                        verifyNoMoreInteractions(callback);
                    }
                }
        );
    }

    @Test
    public void prependFill() {
        validateTwoListDiff(
                new PagedStorage<>(5, createPage("b", "c"), 5),
                new PagedStorage<>(4, createPage("a", "b", "c"), 5),
                new CallbackValidator() {
                    @Override
                    public void validate(ListUpdateCallback callback) {
                        verify(callback).onRemoved(0, 1);
                        verify(callback).onInserted(4, 1);
                        //NOTE: ideally would be onChanged(4, 1, null);
                        verifyNoMoreInteractions(callback);
                    }
                }
        );
    }

    @Test
    public void change() {
        validateTwoListDiff(
                new PagedStorage<>(5, createPage("a1", "b1", "c1"), 5),
                new PagedStorage<>(5, createPage("a2", "b1", "c2"), 5),
                new CallbackValidator() {
                    @Override
                    public void validate(ListUpdateCallback callback) {
                        verify(callback).onChanged(5, 1, null);
                        verify(callback).onChanged(7, 1, null);
                        verifyNoMoreInteractions(callback);
                    }
                }
        );
    }

}
