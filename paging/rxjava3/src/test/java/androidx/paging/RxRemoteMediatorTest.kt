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

package androidx.paging

import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import androidx.paging.RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
import androidx.paging.rxjava3.RxRemoteMediator
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@RunWith(JUnit4::class)
class RxRemoteMediatorTest {
    @Test
    fun initializeSingle() = runBlockingTest {
        val remoteMediator = object : RxRemoteMediator<Int, Int>() {
            override fun loadSingle(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): Single<MediatorResult> {
                fail("Unexpected call")
            }

            override fun initializeSingle(): Single<InitializeAction> {
                return Single.just(SKIP_INITIAL_REFRESH)
            }
        }

        assertEquals(SKIP_INITIAL_REFRESH, remoteMediator.initialize())
    }

    @Test
    fun initializeSingleDefault() = runBlockingTest {
        val remoteMediator = object : RxRemoteMediator<Int, Int>() {
            override fun loadSingle(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): Single<MediatorResult> {
                fail("Unexpected call")
            }
        }

        assertEquals(LAUNCH_INITIAL_REFRESH, remoteMediator.initialize())
    }
}
