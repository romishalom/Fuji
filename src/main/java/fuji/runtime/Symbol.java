package fuji.runtime;

import fuji.util.TypeValue;
import fuji.util.Value;

import java.util.Objects;

public class Symbol {
    private final boolean isConstant;
    private final String name;
    private final TypeValue declaredType;
    private Value value;

    public Symbol(boolean isConstant, String name, TypeValue declaredType, Value value) {
        this.isConstant = isConstant;
        this.name = name;
        this.declaredType = declaredType;
        this.value = value;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public String getName() {
        return name;
    }

    public TypeValue getDeclaredType() {
        return declaredType;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Symbol) obj;
        return this.isConstant == that.isConstant &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.declaredType, that.declaredType) &&
                Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isConstant, name, declaredType, value);
    }

    @Override
    public String toString() {
        return "Symbol[" +
                "isConstant=" + isConstant + ", " +
                "name=" + name + ", " +
                "type=" + declaredType + ", " +
                "values=" + value + ']';
    }

}
