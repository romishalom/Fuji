package fuji.runtime;

import fuji.util.Value;

public class ReturnSignal extends Signal {
    private final Value returnedValue;

    public ReturnSignal(Value returnedValue) {
        this.returnedValue = returnedValue;
    }

    public Value getReturnedValue() {
        return returnedValue;
    }
}
