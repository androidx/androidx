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
package androidx.leanback.app;

import static org.junit.Assert.assertSame;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.leanback.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class ProgressBarManagerTest {

    Context mContext;
    ProgressBarManager mProgressBarManager;
    long mWaitShownTimeOutMs;
    long mWaitHideTimeOutMs;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mProgressBarManager = new ProgressBarManager();
            }
        });
        mWaitShownTimeOutMs = Math.max(2000, mProgressBarManager.getInitialDelay() * 3);
        mWaitHideTimeOutMs = 2000;
    }

    @Test
    public void defaultProgressBarView() {
        final ViewGroup rootView = new FrameLayout(mContext);
        mProgressBarManager.setRootView(rootView);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mProgressBarManager.show();
            }
        });
        PollingCheck.waitFor(mWaitShownTimeOutMs,
                new PollingCheck.PollingCheckCondition() {
                    @Override
                    public boolean canProceed() {
                        if (rootView.getChildCount() == 0) return false;
                        return  rootView.getChildAt(0).getVisibility() == View.VISIBLE;
                    }
                });
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mProgressBarManager.hide();
            }
        });
        PollingCheck.waitFor(mWaitHideTimeOutMs,
                new PollingCheck.PollingCheckCondition() {
                    @Override
                    public boolean canProceed() {
                        return rootView.getChildCount() == 0;
                    }
                });
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mProgressBarManager.show();
            }
        });
        PollingCheck.waitFor(mWaitShownTimeOutMs,
                new PollingCheck.PollingCheckCondition() {
                    @Override
                    public boolean canProceed() {
                        if (rootView.getChildCount() == 0) return false;
                        return  rootView.getChildAt(0).getVisibility() == View.VISIBLE;
                    }
                });
    }

    @Test
    public void customProgressBarView() {
        final ViewGroup rootView = new FrameLayout(mContext);
        View customProgressBar = new View(mContext);
        rootView.addView(customProgressBar, 100, 100);
        mProgressBarManager.setProgressBarView(customProgressBar);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mProgressBarManager.show();
            }
        });
        PollingCheck.waitFor(mWaitShownTimeOutMs,
                new PollingCheck.PollingCheckCondition() {
                    @Override
                    public boolean canProceed() {
                        if (rootView.getChildCount() == 0) return false;
                        return  rootView.getChildAt(0).getVisibility() == View.VISIBLE;
                    }
                });
        assertSame(customProgressBar, rootView.getChildAt(0));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mProgressBarManager.hide();
            }
        });
        PollingCheck.waitFor(mWaitHideTimeOutMs,
                new PollingCheck.PollingCheckCondition() {
                    @Override
                    public boolean canProceed() {
                        return  rootView.getChildAt(0).getVisibility() != View.VISIBLE;
                    }
                });
        assertSame(customProgressBar, rootView.getChildAt(0));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mProgressBarManager.show();
            }
        });
        PollingCheck.waitFor(mWaitShownTimeOutMs,
                new PollingCheck.PollingCheckCondition() {
                    @Override
                    public boolean canProceed() {
                        if (rootView.getChildCount() == 0) return false;
                        return  rootView.getChildAt(0).getVisibility() == View.VISIBLE;
                    }
                });
    }
}
