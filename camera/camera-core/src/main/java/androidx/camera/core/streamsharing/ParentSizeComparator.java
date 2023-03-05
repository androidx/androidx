/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.streamsharing;

import static androidx.camera.core.internal.utils.SizeUtil.getArea;

import static java.util.Objects.requireNonNull;

import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A comparator for sorting the {@link StreamSharing} parent Surface sizes.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ParentSizeComparator implements Comparator<Size> {
    final Map<Size, Map<Integer, Integer>> mScores;
    final List<Integer> mSortedPriorities;

    /**
     * Creates a new instance of {@link ParentSizeComparator}.
     *
     * @param scores     parent/child scores, in the form of <parent size, <child priority,
     *                   score>>. Lower score is preferred.
     * @param priorities a collection of child priorities.
     */
    ParentSizeComparator(@NonNull Map<Size, Map<Integer, Integer>> scores,
            @NonNull Collection<Integer> priorities) {
        mScores = scores;
        mSortedPriorities = new ArrayList<>(priorities);
        Collections.sort(mSortedPriorities, (value1, value2) -> value2 - value1);
    }

    @Override
    public int compare(@NonNull Size size1, @NonNull Size size2) {
        // Get the scores for the 2 sizes. The score is in the form of Map<Priority, Score>.
        Map<Integer, Integer> scores1 = requireNonNull(mScores.get(size1));
        Map<Integer, Integer> scores2 = requireNonNull(mScores.get(size2));
        boolean hasValidScore = false;

        // Loop thru all priorities in order, and compare their scores.
        for (int priority : mSortedPriorities) {
            int score1 = getScore(scores1, priority);
            int score2 = getScore(scores2, priority);
            if (score1 != score2) {
                // Return early if the scores are different. Otherwise, continue to compare the
                // next priorities.
                return score1 - score2;
            }
            hasValidScore = hasValidScore || score1 != Integer.MAX_VALUE;
        }
        if (!hasValidScore) {
            // If no valid score is found, prefer larger size in case upscaling is needed.
            return getArea(size2) - getArea(size1);
        }
        // If scores are valid and equal, compare them by size. The smaller size is preferred
        // because it means less memory is used.
        return getArea(size1) - getArea(size2);
    }

    private int getScore(@NonNull Map<Integer, Integer> scores, int priority) {
        Integer score = scores.get(priority);
        if (score == null) {
            return Integer.MAX_VALUE;
        } else {
            return score;
        }
    }

    @VisibleForTesting
    @NonNull
    List<Integer> getSortedPriorities() {
        return mSortedPriorities;
    }
}
