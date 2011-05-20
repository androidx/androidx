package android.support.v13.dreams;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.os.BatteryManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class BasicDream extends Activity {
    /** A simple Dream implementation that can be subclassed to write your own Dreams. Any Activity
     * may be used as a Dream, so this class isn't strictly necessary. However, it does take care of
     * a number of housekeeping tasks that most screensavers will want:
     * <ul>
     * <li>Keep the screen on as long as the device is plugged in
     * <li>Exit (using <code>finish()</code>) as soon as any user activity is detected
     * <li>Hide the system UI (courtesy its inner {@link BasicDreamView} class)
     * </ul>
     * Finally, it exposes an {@link BasicDream#onDraw(Canvas)} method that you can override to do
     * your own drawing. As with a <code>View,</code> call {@link BasicDream#invalidate()} any time
     * to request that a new frame be drawn.
     */
    private final static String TAG = "BasicDream";
    private final static boolean DEBUG = true;

    private View mView;
    private boolean mPlugged = false;

    public class BasicDreamView extends View {
        /** A simple view that just calls back to {@link BasicDream#onDraw(Canvas) onDraw} on its
         * parent BasicDream Activity. It also hides the system UI if this feature is available on
         * the current device.
         */
        public BasicDreamView(Context c) {
            super(c);
        }

        public BasicDreamView(Context c, AttributeSet at) {
            super(c, at);
        }

        @Override
        public void onAttachedToWindow() {
            setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        }

        @Override
        public void onDraw(Canvas c) {
            BasicDream.this.onDraw(c);
        }
    }

    private final BroadcastReceiver mPowerIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                // Only keep the screen on if we're plugged in.
                boolean plugged = (1 == intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));

                if (plugged != mPlugged) {
                    if (DEBUG) Log.d(TAG, "now " + (plugged ? "plugged in" : "unplugged"));

                    mPlugged = plugged;
                    if (mPlugged) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            }
        }
    };
    
    @Override
    public void onStart() {
        super.onStart();
        setContentView(new BasicDreamView(this));
        getWindow().addFlags(
                  WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                );
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mPowerIntentReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Anything that would pause this activity should probably end the screensaver.
        if (DEBUG) Log.d(TAG, "exiting onPause");
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(mPowerIntentReceiver);
    }

    protected View getContentView() {
        return mView;
    }

    @Override
    public void setContentView(View v) {
        super.setContentView(v);
        mView = v;
    }

    protected void invalidate() {
        getContentView().invalidate();
    }

    public void onDraw(Canvas c) {
    }

    public void onUserInteraction() {
        if (DEBUG) Log.d(TAG, "exiting onUserInteraction");
        finish();
    }
}

