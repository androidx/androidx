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

package com.android.sample.githubbrowser.db;

import com.android.sample.githubbrowser.data.ContributorData;
import com.android.sample.githubbrowser.data.ContributorSearchData;
import com.android.sample.githubbrowser.data.GeneralRepoSearchData;
import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.data.SearchQueryData;
import com.android.support.lifecycle.LiveData;
import com.android.support.room.Dao;
import com.android.support.room.Insert;
import com.android.support.room.OnConflictStrategy;
import com.android.support.room.Query;

import java.util.List;

/**
 * Data access object for github data table.
 */
@Dao
public interface GithubDao {
    /**
     * Load full data for a person based on the login.
     */
    @Query("select * from persondata where login = ?")
    LiveData<PersonData> loadPerson(String login);

    /**
     * Insert or update full data for a person.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplacePerson(PersonData personData);

    @Query("UPDATE PersonData SET email = :email, location = :location WHERE login = :login")
    void updateUser(String login, String email, String location);

    /** Load search data for the specified query. */
    @Query("select * from searchquerydata where searchQuery = :searchQuery"
            + " AND searchKind = :searchKind")
    SearchQueryData getSearchQueryData(String searchQuery, int searchKind);

    /** Updates search data. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(SearchQueryData searchQueryData);

    /** Inserts or updates metadata for results of repository search. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GeneralRepoSearchData[] generalRepoSearchDataArray);

    /** Inserts or updates the repository data objects. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RepositoryData[] repoDataArray);

    /** Insert or update full data for a repository. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceRepository(RepositoryData repositoryData);

    /** Loads full data for a repository. */
    @Query("select * from repositorydata where id = ?")
    LiveData<RepositoryData> loadRepository(String id);

    /** Inserts or updates metadata for results of contributor search. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ContributorSearchData[] contributorSearchDataArray);

    /** Inserts or updates the person data objects. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PersonData[] personDataArray);

    /**
     * Loads repository results for the specified query.
     */
    @Query("SELECT r.*, MIN(qr.resultIndex) as resultIndex from repositorydata r, "
            + "generalreposearchdata qr, searchquerydata q"
            + "    WHERE q.searchQuery = qr.searchQuery"
            + "          AND q.searchKind = " + SearchQueryData.GENERAL_REPOSITORIES
            + "          AND r.id = qr.repoId"
            + "          AND q.searchQuery = ?"
            + "          GROUP BY r.id"
            + "          ORDER BY resultIndex")
    LiveData<List<RepositoryData>> getRepositories(String searchQuery);

    /**
     * Loads contributor results for the specified repository.
     */
    @Query("SELECT p.*, qr.contributions, MIN(qr.resultIndex) as resultIndex from persondata p, "
            + "contributorsearchdata qr, searchquerydata q"
            + "    WHERE q.searchQuery = qr.searchQuery"
            + "          AND q.searchKind = " + SearchQueryData.REPOSITORY_CONTRIBUTORS
            + "          AND p.id = qr.contributorId"
            + "          AND q.searchQuery = ?"
            + "          GROUP BY p.id"
            + "          ORDER BY resultIndex")
    LiveData<List<ContributorData>> getContributors(String repoName);
}
