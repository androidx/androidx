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

package com.example.android.persistence.codelab.step4_solution;

import android.app.Application;
import android.support.annotation.NonNull;

import com.android.support.lifecycle.AndroidViewModel;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.Observer;
import com.example.android.persistence.codelab.orm_db.AppDatabase;
import com.example.android.persistence.codelab.orm_db.LoanWithUserAndBook;
import com.example.android.persistence.codelab.orm_db.utils.DatabaseInitializer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ShowUserViewModel extends AndroidViewModel {

    private LiveData<List<LoanWithUserAndBook>> mLoans;

    private LiveData<String> mLoansResult;

    private AppDatabase mDb;

    private final Observer<List<LoanWithUserAndBook>> mObserver =
            new Observer<List<LoanWithUserAndBook>>() {
                @Override
                public void onChanged(
                        @NonNull final List<LoanWithUserAndBook> loansWithUserAndBook) {
                    StringBuilder sb = new StringBuilder();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm",
                            Locale.US);

                    for (LoanWithUserAndBook loan : loansWithUserAndBook) {
                        sb.append(String.format("%s\n  (Returned: %s)\n",
                                loan.bookTitle,
                                simpleDateFormat.format(loan.endTime)));

                    }
                    mLoansResult.setValue(sb.toString());
                }
            };

    public ShowUserViewModel(Application application) {
        super(application);
    }

    public LiveData<String> getLoansResult() {
        return mLoansResult;
    }

    public void createDb() {
        AppDatabase.destroyInstance();
        mDb = AppDatabase.getInMemoryDatabase(getApplication());

        // Populate it with initial data
        DatabaseInitializer.populate(mDb);

        // Receive changes
        subscribeToDbChanges();
    }

    private void subscribeToDbChanges() {
        // Books is a LiveData object so updates are observed.

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, -1);
        Date yesterday = calendar.getTime();
        removeObserver();
        mLoans = mDb.loanModel().findLoansByNameAfter("Mike", yesterday);

        mLoansResult = new LiveData<>();

        mLoans.observeForever(mObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeObserver();
    }

    private void removeObserver() {
        if (mLoans != null) {
            mLoans.removeObserver(mObserver);
        }
    }
}
