package fuji.runtime;

public class BreakSignal extends Signal {
    public static final BreakSignal INSTANCE = new BreakSignal();

    private BreakSignal() {
    }
}
