package fuji.util;

public record EnumInstanceValue(String name, EnumValue type) implements Value {
    @Override
    public String toString() {
        return name;
    }
}
