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

package androidx.room.integration.testapp.dao;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public interface WithClauseDao {
    @Query("WITH RECURSIVE factorial(n, fact) AS \n"
            + "(SELECT 0, 1 \n"
            + "   UNION ALL \n"
            + " SELECT n+1, (n+1)*fact FROM factorial \n"
            + "   WHERE n < :maxIndexInclusive)\n"
            + " SELECT fact FROM factorial;")
    List<Integer> getFactorials(int maxIndexInclusive);

    @Query("WITH RECURSIVE factorial(n, fact) AS \n"
            + "(SELECT 0, 1 \n"
            + "   UNION ALL \n"
            + " SELECT n+1, (n+1)*fact FROM factorial \n"
            + "   WHERE n < :maxIndexInclusive)\n"
            + " SELECT mName FROM User WHERE User.mId IN (Select fact FROM factorial);")
    List<String> getUsersWithFactorialIds(int maxIndexInclusive);

    @Query("WITH RECURSIVE factorial(n, fact) AS \n"
            + "(SELECT 0, 1 \n"
            + "   UNION ALL \n"
            + " SELECT n+1, (n+1)*fact FROM factorial \n"
            + "   WHERE n < :maxIndexInclusive)\n"
            + " SELECT mName FROM User WHERE User.mId IN (Select fact FROM factorial);")
    LiveData<List<String>> getUsersWithFactorialIdsLiveData(int maxIndexInclusive);
}
