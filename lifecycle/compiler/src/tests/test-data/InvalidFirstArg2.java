package foo;

import static android.arch.lifecycle.Lifecycle.Event.ON_ANY;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;

public class InvalidFirstArg2 implements LifecycleObserver {
    @OnLifecycleEvent(ON_ANY)
    public void onStop(Event e2, Event event) {
    }
}
