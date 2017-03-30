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

package com.example.android.persistence.codelab.step4;

import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;
import com.example.android.persistence.codelab.orm_db.AppDatabase;
import com.example.android.persistence.codelab.orm_db.Book;
import com.example.android.persistence.codelab.orm_db.utils.DatabaseInitializer;

import java.util.List;


public class BooksBorrowedByUserViewModel extends ViewModel {

    private LiveData<List<Book>> mBooks;

    private AppDatabase mDb;

    public void createDb() {
        AppDatabase.destroyInstance();
        mDb = AppDatabase.getInMemoryDatabase(this.getApplication());

        // Populate it with initial data
        DatabaseInitializer.populate(mDb);

        // Receive changes
        subscribeToDbChanges();
    }

    public LiveData<List<Book>> getBooks() {
        return mBooks;
    }

    private void subscribeToDbChanges() {
        // Books is a LiveData object so updates are observed.
        mBooks = mDb.bookModel().findBooksBorrowedByName("Mike");
    }
}
