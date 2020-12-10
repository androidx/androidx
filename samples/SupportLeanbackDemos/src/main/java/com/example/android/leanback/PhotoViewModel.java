/*
 * Copyright 2020 The Android Open Source Project
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
package com.example.android.leanback;

import android.app.Application;

import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingDataTransforms;
import androidx.paging.PagingLiveData;

import com.example.android.leanback.room.Photo;
import com.example.android.leanback.room.PhotoDatabase;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ViewModel for showing sample usage of PagedListAdapter
 */
public class PhotoViewModel extends AndroidViewModel {

    private LiveData<PagingData<PhotoItem>> mPagingDataLiveData;
    private Executor mExecutor = Executors.newSingleThreadExecutor();

    public PhotoViewModel(Application application) {
        super(application);

        Pager<Integer, Photo> pager = new Pager<Integer, Photo>(
                new PagingConfig(5, 10, false),
                () -> PhotoDatabase.getInstance(
                        application).photoDao().fetchPhotos()
        );

        mPagingDataLiveData = Transformations.map(PagingLiveData.getLiveData(pager),
                (Function<PagingData<Photo>, PagingData<PhotoItem>>) pagingData ->
                        PagingDataTransforms.map(pagingData, mExecutor,
                                (photo) -> new PhotoItem(photo.getTitle(),
                                        photo.getImgResourceId(),
                                        photo.getId()))
        );


    }

    public LiveData<PagingData<PhotoItem>> getPagingDataLiveData() {
        return mPagingDataLiveData;
    }
}
