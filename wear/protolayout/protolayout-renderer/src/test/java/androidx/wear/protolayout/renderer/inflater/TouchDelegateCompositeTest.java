package androidx.wear.protolayout.renderer.inflater;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class TouchDelegateCompositeTest {
    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();
    @Mock
    private TouchDelegate touchDelegate1;
    @Mock
    private TouchDelegate touchDelegate2;
    @Mock
    private TouchDelegate touchDelegate3;

    private TouchDelegateComposite underTest;
    private TouchDelegateComposite delegateComposite2;
    private TouchDelegateComposite delegateComposite3;

    @Before
    public void setUp() {
        // 4*4 hit rects with 12*12 extended rects
        // the extended rect for 1 & 2 overlaps at [8, 0, 12, 12]
        // the extended rect for 1 & 2 & 3overlaps at [8, 8, 12, 12]
        // |----------|------|-----------|
        // |          |      |           |
        // |    ******|      |******     |
        // |    ***1**|      |***2**     |
        // |    ******|      |******     |
        // |---|------|------|----|------|
        // |---|------|------|----|------|
        //     |      ******      |
        //     |      ***3**      |
        //     |      ******      |
        //     |                  |
        //     |------------------|
        underTest =
                new TouchDelegateComposite(
                        new View(getApplicationContext()),
                        new Rect(4, 4, 8, 8), // actual hit bound
                        new Rect(0, 0, 12, 12), // extended bound
                        touchDelegate1);

        delegateComposite2 =
                new TouchDelegateComposite(
                        new View(getApplicationContext()),
                        new Rect(12, 4, 16, 8), // actual hit bound
                        new Rect(8, 0, 20, 12), // extended bound
                        touchDelegate2);

        delegateComposite3 =
                new TouchDelegateComposite(
                        new View(getApplicationContext()),
                        new Rect(8, 12, 12, 16), // actual hit bound
                        new Rect(4, 8, 16, 20), // extended bound
                        touchDelegate3);
    }

    @Test
    public void onTouchEvent_whenInBound_delegateTouchDown() {
        // Touch down event with touch point inside the extended bound.
        MotionEvent event = obtainMotionEvent(MotionEvent.ACTION_DOWN, 1.f, 1.f);
        underTest.onTouchEvent(event);

        verify(touchDelegate1).onTouchEvent(event);
    }

    @Test
    public void onTouchEvent_whenOutsideBound_notDelegateTouchDown() {
        // Touch down event with touch point outside the extended bound.
        underTest.onTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 13.f, 13.f));

        verify(touchDelegate1, never()).onTouchEvent(any(MotionEvent.class));
    }

    @Test
    public void onTouchEvent_alwaysDelegateTouchUp() {
        // Touch up event with touch point inside the extended bound.
        MotionEvent event1 = obtainMotionEvent(MotionEvent.ACTION_UP, 1.f, 1.f);
        underTest.onTouchEvent(event1);

        verify(touchDelegate1).onTouchEvent(event1);

        // Touch up event with touch point outside the extended bound.
        MotionEvent event2 = obtainMotionEvent(MotionEvent.ACTION_UP, 13.f, 13.f);
        underTest.onTouchEvent(event2);

        verify(touchDelegate1).onTouchEvent(event2);
    }

    @Test
    public void onTouchEvent_whenInOverlappedBound_delegateTouchDownToTheClosest() {
        // The extended rect for view 1 & 2 overlaps at [8, 0, 12, 12]
        underTest.mergeFrom(delegateComposite2);

        // Touch down event with point inside both extended bound, and closer to view 1's actual
      // bound.
        MotionEvent event1 = obtainMotionEvent(MotionEvent.ACTION_DOWN, 9.f, 1.f);
        underTest.onTouchEvent(event1);

        verify(touchDelegate1).onTouchEvent(event1);
        verify(touchDelegate2, never()).onTouchEvent(event1);

        // Touch down event with point inside both extended bound, and closer to view 2's actual
      // bound.
        MotionEvent event2 = obtainMotionEvent(MotionEvent.ACTION_DOWN, 11.f, 11.f);
        underTest.onTouchEvent(event2);

        verify(touchDelegate1, never()).onTouchEvent(event2);
        verify(touchDelegate2).onTouchEvent(event2);
    }

    @Test
    public void onTouchEvent_whenInOverlappedBound_alwaysDelegateTouchUp() {
        underTest.mergeFrom(delegateComposite2);

        MotionEvent event = obtainMotionEvent(MotionEvent.ACTION_UP, 9.f, 1.f);
        underTest.onTouchEvent(event);

        verify(touchDelegate1).onTouchEvent(event);
        verify(touchDelegate2).onTouchEvent(event);

        MotionEvent event2 = obtainMotionEvent(MotionEvent.ACTION_UP, 11.f, 11.f);
        underTest.onTouchEvent(event2);

        verify(touchDelegate1).onTouchEvent(event2);
        verify(touchDelegate2).onTouchEvent(event2);
    }

    @Test
    public void onTouchEvent_whenInMultipleOverlappedBound_delegateTouchDownToTheClosest() {
        // The extended rect for 1 & 2 & 3 overlaps at [8, 8, 12, 12]
        underTest.mergeFrom(delegateComposite2);
        underTest.mergeFrom(delegateComposite3);

        // Touch down event with point inside all three extended bounds
        // And closest to view 1's actual bound.
        MotionEvent event = obtainMotionEvent(MotionEvent.ACTION_DOWN, 9.f, 9.f);
        underTest.onTouchEvent(event);

        verify(touchDelegate1).onTouchEvent(event);
        verify(touchDelegate2, never()).onTouchEvent(event);
        verify(touchDelegate3, never()).onTouchEvent(event);

        // Touch down event with point inside all three extended bounds
        // And closest to view 2's actual bound.
        MotionEvent event2 = obtainMotionEvent(MotionEvent.ACTION_DOWN, 11.f, 9.f);
        underTest.onTouchEvent(event2);

        verify(touchDelegate1, never()).onTouchEvent(event2);
        verify(touchDelegate2).onTouchEvent(event2);
        verify(touchDelegate3, never()).onTouchEvent(event);

        // Touch down event with point inside all three extended bounds
        // And closest to view 3's actual bound.
        MotionEvent event3 = obtainMotionEvent(MotionEvent.ACTION_DOWN, 11.f, 11.f);
        underTest.onTouchEvent(event3);

        verify(touchDelegate1, never()).onTouchEvent(event3);
        verify(touchDelegate2, never()).onTouchEvent(event3);
        verify(touchDelegate3).onTouchEvent(event3);
    }

    @Test
    public void onTouchEvent_whenInMultipleOverlappedBound_alwaysDelegateTouchUp() {
        underTest.mergeFrom(delegateComposite2);
        underTest.mergeFrom(delegateComposite3);

        MotionEvent event = obtainMotionEvent(MotionEvent.ACTION_UP, 9.f, 9.f);
        underTest.onTouchEvent(event);

        verify(touchDelegate1).onTouchEvent(event);
        verify(touchDelegate2).onTouchEvent(event);
        verify(touchDelegate3).onTouchEvent(event);

        MotionEvent event2 = obtainMotionEvent(MotionEvent.ACTION_UP, 11.f, 11.f);
        underTest.onTouchEvent(event2);

        verify(touchDelegate1).onTouchEvent(event2);
        verify(touchDelegate2).onTouchEvent(event2);
        verify(touchDelegate3).onTouchEvent(event2);
    }

    private static MotionEvent obtainMotionEvent(int action, float x, float y) {
        long startTime = SystemClock.uptimeMillis();
        return MotionEvent.obtain(
                /* downTime= */ startTime,
                /* eventTime= */ startTime + 10,
                action,
                /* x= */ x,
                /* y= */ y,
                /* metaState= */ 0);
    }
}
