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

import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import com.example.android.persistence.codelab.db.AppDatabase;
import com.example.android.persistence.codelab.db.Book;
import com.example.android.persistence.codelab.db.utils.DatabaseInitializer;

import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class TypeConvertersViewModel extends AndroidViewModel {

    private LiveData<List<Book>> mBooks;

    private AppDatabase mDb;

    public TypeConvertersViewModel(Application application) {
        super(application);
        createDb();
    }

    public void createDb() {
        mDb = AppDatabase.getInMemoryDatabase(this.getApplication());

        // Populate it with initial data
        DatabaseInitializer.populateAsync(mDb);

        // Receive changes
        subscribeToDbChanges();
    }

    public LiveData<List<Book>> getBooks() {
        return mBooks;
    }

    private void subscribeToDbChanges() {
        // Books is a LiveData object so updates are observed.
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, -1);
        Date yesterday = calendar.getTime();
        mBooks = mDb.bookModel().findBooksBorrowedByNameAfter("Mike", yesterday);
    }
}
