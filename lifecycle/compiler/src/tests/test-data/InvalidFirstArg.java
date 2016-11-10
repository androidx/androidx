package foo;

import static com.android.support.lifecycle.Lifecycle.ON_STOP;

import com.android.support.lifecycle.OnLifecycleEvent;

public class InvalidFirstArg {
    @OnLifecycleEvent(ON_STOP)
    public void onStop(int lastEvent) {
    }
}