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

package com.example.android.persistence.codelab.db;


import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;

import java.util.Date;
import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.ABORT;

@Dao
@TypeConverters(DateConverter.class)
public interface LoanDao {

    @Query("SELECT * From Loan")
    LiveData<List<Loan>> findAll();

    @Query("SELECT Loan.id, Book.title, User.name, Loan.startTime, Loan.endTime From Loan " +
        "INNER JOIN Book ON Loan.book_id = Book.id " +
        "INNER JOIN User ON Loan.user_id = User.id ")
    LiveData<List<LoanWithUserAndBook>> findAllWithUserAndBook();

    @Query("SELECT Loan.id, Book.title as title, User.name as name, Loan.startTime, Loan.endTime " +
            "FROM Book " +
            "INNER JOIN Loan ON Loan.book_id = Book.id " +
            "INNER JOIN User on User.id = Loan.user_id " +
            "WHERE User.name LIKE :userName " +
            "AND Loan.endTime > :after "
    )
    public LiveData<List<LoanWithUserAndBook>> findLoansByNameAfter(String userName, Date after);

    @Insert(onConflict = ABORT)
    void insertLoan(Loan loan);

    @Query("DELETE FROM Loan")
    void deleteAll();
}
