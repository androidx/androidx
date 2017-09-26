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
import org.mockito.Mockito;

@SmallTest
@RunWith(JUnit4.class)
public class ContiguousDiffHelperTest {
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

    private void validateTwoListDiff(StringPagedList oldList, StringPagedList newList,
            CallbackValidator callbackValidator) {
        DiffUtil.DiffResult diffResult = ContiguousDiffHelper.computeDiff(oldList, newList,
                DIFF_CALLBACK, false);

        ListUpdateCallback listUpdateCallback = Mockito.mock(ListUpdateCallback.class);
        ContiguousDiffHelper.dispatchDiff(listUpdateCallback, oldList, newList, diffResult);

        callbackValidator.validate(listUpdateCallback);
    }

    @Test
    public void sameListNoUpdates() {
        validateTwoListDiff(
                new StringPagedList(5, 5, "a", "b", "c"),
                new StringPagedList(5, 5, "a", "b", "c"),
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
                new StringPagedList(5, 5, "a", "b"),
                new StringPagedList(5, 4, "a", "b", "c"),
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
                new StringPagedList(5, 5, "b", "c"),
                new StringPagedList(4, 5, "a", "b", "c"),
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
                new StringPagedList(5, 5, "a1", "b1", "c1"),
                new StringPagedList(5, 5, "a2", "b1", "c2"),
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
