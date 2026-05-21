package fuji.runtime;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, Symbol> localSymbols;

    public Scope(Scope parent) {
        this.parent = parent;
        this.localSymbols = new HashMap<>();
    }

    public Scope() {
        this(null);
    }

    public void declare(Symbol symbol) {
        localSymbols.put(symbol.getName(), symbol);
    }

    public Symbol resolveLocal(String name) {
        return localSymbols.get(name);
    }

    public Symbol resolve(String name) {
        Symbol symbol = resolveLocal(name);

        if (symbol == null && parent != null)
            symbol = parent.resolve(name);

        return symbol;
    }

    public boolean hasLocal(String name) {
        return localSymbols.containsKey(name);
    }

    public boolean has(String name) {
        boolean has = hasLocal(name);

        if (!has && parent != null)
            has = parent.has(name);

        return has;
    }
}
