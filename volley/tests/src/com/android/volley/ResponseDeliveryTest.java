/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley;

import android.test.suitebuilder.annotation.MediumTest;

import com.android.volley.mock.MockRequest;
import com.android.volley.utils.CacheTestUtils;
import com.android.volley.utils.ImmediateResponseDelivery;

import junit.framework.TestCase;

@MediumTest
public class ResponseDeliveryTest extends TestCase {

    private ExecutorDelivery mDelivery;
    private MockRequest mRequest;
    private Response<byte[]> mSuccessResponse;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Make the delivery just run its posted responses immediately.
        mDelivery = new ImmediateResponseDelivery();
        mRequest = new MockRequest();
        mRequest.setSequence(1);
        byte[] data = new byte[16];
        Cache.Entry cacheEntry = CacheTestUtils.makeRandomCacheEntry(data);
        mSuccessResponse = Response.success(data, cacheEntry);
    }

    public void testPostResponse_callsDeliverResponse() {
        mDelivery.postResponse(mRequest, mSuccessResponse);
        assertTrue(mRequest.deliverResponse_called);
        assertFalse(mRequest.deliverError_called);
    }

    public void testPostResponse_suppressesCanceled() {
        mRequest.cancel();
        mDelivery.postResponse(mRequest, mSuccessResponse);
        assertFalse(mRequest.deliverResponse_called);
        assertFalse(mRequest.deliverError_called);
    }

    public void testPostResponse_suppressesDrained() {
        // discardBefore is exclusive, an exact match should be delivered
        MockRequest request1 = new MockRequest();
        request1.setSequence(16);
        mDelivery.discardBefore(16);
        mDelivery.postResponse(request1, mSuccessResponse);
        assertTrue(request1.deliverResponse_called);
        assertFalse(request1.deliverError_called);

        // This one should be eaten though.
        MockRequest request2 = new MockRequest();
        request2.setSequence(20);
        mDelivery.discardBefore(21);
        mDelivery.postResponse(request2, mSuccessResponse);
        assertFalse(request2.deliverResponse_called);
        assertFalse(request2.deliverError_called);
    }

    public void testPostResponse_allowsDrainedButUndrainable() {
        mRequest.setSequence(15);
        mRequest.setDrainable(false);
        mDelivery.discardBefore(100);
        mDelivery.postResponse(mRequest, mSuccessResponse);
        assertTrue(mRequest.deliverResponse_called);
        assertFalse(mRequest.deliverError_called);
    }

    public void testPostError_callsDeliverError() {
        Response<byte[]> errorResponse = Response.error(new ServerError());

        mDelivery.postResponse(mRequest, errorResponse);
        assertTrue(mRequest.deliverError_called);
        assertFalse(mRequest.deliverResponse_called);
    }
}
