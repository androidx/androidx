package foo;
import com.android.support.lifecycle.OnState;
import static com.android.support.lifecycle.Lifecycle.STARTED;
import static com.android.support.lifecycle.Lifecycle.STOPPED;
import com.android.support.lifecycle.LifecycleProvider;
import java.util.HashMap;

class Base1 {
    @OnState(STOPPED)
    public void onStop(LifecycleProvider provider, int prevstate){}
}

class Proxy extends Base1 { }

class Derived1 extends Proxy {
    @OnState(STOPPED)
    public void onStop2(LifecycleProvider provider, int prevstate){}
}

class Derived2 extends Proxy {
    @OnState(STOPPED)
    public void onStop2(LifecycleProvider provider, int prevstate){}
}

class Base2 {
    @OnState(STOPPED)
    public void onStop(LifecycleProvider provider, int prevstate){}
}

class Derived3 extends Base2 {
    @OnState(STOPPED)
    public void onStop2(LifecycleProvider provider, int prevstate){}
}
