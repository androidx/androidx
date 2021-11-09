package foo;

import static androidx.lifecycle.Lifecycle.Event.ON_ANY;

import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class InvalidFirstArg2 implements LifecycleObserver {
    @OnLifecycleEvent(ON_ANY)
    public void onStop(Event e2, Event event) {
    }
}
