/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.persistence.codelab.step5;

import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Transformations;

import com.example.android.persistence.codelab.db.AppDatabase;
import com.example.android.persistence.codelab.db.LoanWithUserAndBook;
import com.example.android.persistence.codelab.db.utils.DatabaseInitializer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CustomResultViewModel extends AndroidViewModel {

    private LiveData<String> mLoansResult;

    private AppDatabase mDb;

    public CustomResultViewModel(Application application) {
        super(application);
    }

    public LiveData<String> getLoansResult() {
        return mLoansResult;
    }

    public void createDb() {
        mDb = AppDatabase.getInMemoryDatabase(getApplication());

        // Populate it with initial data
        DatabaseInitializer.populateAsync(mDb);

        // Receive changes
        subscribeToDbChanges();
    }

    private void subscribeToDbChanges() {
        // TODO: Modify this query to show only recent loans from specific user
        LiveData<List<LoanWithUserAndBook>> loans
                = mDb.loanModel().findAllWithUserAndBook();

        // Instead of exposing the list of Loans, we can apply a transformation and expose Strings.
        mLoansResult = Transformations.map(loans,
                new Function<List<LoanWithUserAndBook>, String>() {
            @Override
            public String apply(List<LoanWithUserAndBook> loansWithUserAndBook) {
                StringBuilder sb = new StringBuilder();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm",
                        Locale.US);

                for (LoanWithUserAndBook loan : loansWithUserAndBook) {
                    sb.append(String.format("%s\n  (Returned: %s)\n",
                            loan.bookTitle,
                            simpleDateFormat.format(loan.endTime)));
                }
                return sb.toString();
            }
        });
    }

    @SuppressWarnings("unused")
    private Date getYesterdayDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, -1);
        return calendar.getTime();
    }
}
