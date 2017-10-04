package foo;

import static android.arch.lifecycle.Lifecycle.Event.ON_START;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.OnLifecycleEvent;

public class DerivedFromJar extends test.library.LibraryBaseObserver {
    @OnLifecycleEvent(ON_START)
    public void doAnother() {
    }
}
