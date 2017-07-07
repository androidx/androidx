package foo;

import static android.arch.lifecycle.Lifecycle.Event.ON_ANY;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.OnLifecycleEvent;

public class InvalidFirstArg2 {
    @OnLifecycleEvent(ON_ANY)
    public void onStop(Event e2, Event event) {
    }
}
