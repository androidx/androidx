package foo;
import com.android.support.lifecycle.OnState;
import static com.android.support.lifecycle.Lifecycle.STARTED;
import static com.android.support.lifecycle.Lifecycle.STOPPED;
import com.android.support.lifecycle.LifecycleProvider;
import java.util.HashMap;

class InheritanceOk2Base {
    @OnState(STOPPED)
    public void onStop(LifecycleProvider provider, int prevstate){}
}

class InheritanceOk2Derived extends InheritanceOk2Base {
    @OnState(STOPPED)
    public void onStop2(LifecycleProvider provider, int prevstate){}
}