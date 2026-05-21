package fuji.runtime;

public class ContinueSignal extends Signal {
    public static final ContinueSignal INSTANCE = new ContinueSignal();

    private ContinueSignal() {
    }
}
