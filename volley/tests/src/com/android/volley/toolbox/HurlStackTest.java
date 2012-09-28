/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.volley.toolbox;

import com.android.volley.Request.Method;
import com.android.volley.mock.MockHttpURLConnection;
import com.android.volley.mock.TestRequest;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

@SmallTest
public class HurlStackTest extends AndroidTestCase {

    private MockHttpURLConnection mMockConnection;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mMockConnection = new MockHttpURLConnection();
    }

    public void testConnectionForDeprecatedGetRequest() throws Exception {
        TestRequest.DeprecatedGet request = new TestRequest.DeprecatedGet();
        assertEquals(request.getMethod(), Method.DEPRECATED_GET_OR_POST);

        HurlStack.setConnectionParametersForRequest(mMockConnection, request);
        assertEquals("GET", mMockConnection.getRequestMethod());
        assertFalse(mMockConnection.getDoOutput());
    }

    public void testConnectionForDeprecatedPostRequest() throws Exception {
        TestRequest.DeprecatedPost request = new TestRequest.DeprecatedPost();
        assertEquals(request.getMethod(), Method.DEPRECATED_GET_OR_POST);

        HurlStack.setConnectionParametersForRequest(mMockConnection, request);
        assertEquals("POST", mMockConnection.getRequestMethod());
        assertTrue(mMockConnection.getDoOutput());
    }

    public void testConnectionForGetRequest() throws Exception {
        TestRequest.Get request = new TestRequest.Get();
        assertEquals(request.getMethod(), Method.GET);

        HurlStack.setConnectionParametersForRequest(mMockConnection, request);
        assertEquals("GET", mMockConnection.getRequestMethod());
        assertFalse(mMockConnection.getDoOutput());
    }

    public void testConnectionForPostRequest() throws Exception {
        TestRequest.Post request = new TestRequest.Post();
        assertEquals(request.getMethod(), Method.POST);

        HurlStack.setConnectionParametersForRequest(mMockConnection, request);
        assertEquals("POST", mMockConnection.getRequestMethod());
        assertFalse(mMockConnection.getDoOutput());
    }

    public void testConnectionForPostWithBodyRequest() throws Exception {
        TestRequest.PostWithBody request = new TestRequest.PostWithBody();
        assertEquals(request.getMethod(), Method.POST);

        HurlStack.setConnectionParametersForRequest(mMockConnection, request);
        assertEquals("POST", mMockConnection.getRequestMethod());
        assertTrue(mMockConnection.getDoOutput());
    }

    public void testConnectionForPutRequest() throws Exception {
        TestRequest.Put request = new TestRequest.Put();
        assertEquals(request.getMethod(), Method.PUT);

        HurlStack.setConnectionParametersForRequest(mMockConnection, request);
        assertEquals("PUT", mMockConnection.getRequestMethod());
        assertFalse(mMockConnection.getDoOutput());
    }

    public void testConnectionForPutWithBodyRequest() throws Exception {
        TestRequest.PutWithBody request = new TestRequest.PutWithBody();
        assertEquals(request.getMethod(), Method.PUT);

        HurlStack.setConnectionParametersForRequest(mMockConnection, request);
        assertEquals("PUT", mMockConnection.getRequestMethod());
        assertTrue(mMockConnection.getDoOutput());
    }

    public void testConnectionForDeleteRequest() throws Exception {
        TestRequest.Delete request = new TestRequest.Delete();
        assertEquals(request.getMethod(), Method.DELETE);

        HurlStack.setConnectionParametersForRequest(mMockConnection, request);
        assertEquals("DELETE", mMockConnection.getRequestMethod());
        assertFalse(mMockConnection.getDoOutput());
    }
}
