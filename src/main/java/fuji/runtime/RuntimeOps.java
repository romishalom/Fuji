package fuji.runtime;

import com.romishalom.util.*;
import com.romishalom.util.ClassValue;
import fuji.util.*;

import java.util.*;

public class RuntimeOps {

    // Built ins
    private static final Map<String, Value> GLOBAL_FUNCTIONS = new HashMap<>();

    static {
        // Define println logic
        SignatureValue printlnSig = new SignatureValue(List.of(PrimitiveType.ANY), PrimitiveType.VOID);
        GLOBAL_FUNCTIONS.put("println", new FunctionValue(false, printlnSig, (args) -> {
            System.out.println(args.getFirst());
            return VoidValue.UNIT;
        }));

        // Define input logic
        Scanner sc = new Scanner(System.in);
        SignatureValue inputSig = new SignatureValue(List.of(), PrimitiveType.STRING);
        GLOBAL_FUNCTIONS.put("input", new FunctionValue(false, inputSig, (args) -> new StringValue(sc.next())));

        SignatureValue typeofSig = new SignatureValue(List.of(PrimitiveType.ANY), PrimitiveType.TYPE);
        GLOBAL_FUNCTIONS.put("typeof", new FunctionValue(false, typeofSig, (args) -> args.getFirst().type()));

        SignatureValue arraySig = new SignatureValue(List.of(PrimitiveType.TYPE), PrimitiveType.TYPE);
        GLOBAL_FUNCTIONS.put("array", new FunctionValue(false, arraySig, (args) -> new ArrayTypeValue((TypeValue) args.getFirst())));


        SignatureValue innerFunctionSig = new SignatureValue(List.of(PrimitiveType.TYPE), PrimitiveType.TYPE);
        SignatureValue functionSig = new SignatureValue(List.of(PrimitiveType.TYPE), innerFunctionSig);

        GLOBAL_FUNCTIONS.put("function", new FunctionValue(true, functionSig, (args) -> {
            List<TypeValue> paramTypes = new ArrayList<>();
            for (Value arg: args) {
                paramTypes.add( (TypeValue) arg );
            }

            return new FunctionValue(false, innerFunctionSig, (innerArgs) -> new SignatureValue(paramTypes, (TypeValue) innerArgs.getFirst()));
        }));

        // todo: tuple type function
        SignatureValue tupleSig = new SignatureValue(List.of(PrimitiveType.TYPE), PrimitiveType.TYPE);
        GLOBAL_FUNCTIONS.put("tuple", new FunctionValue(true, tupleSig, (args) -> {
            List<TypeValue> valueTypes = new ArrayList<>();
            for (Value arg: args) {
                valueTypes.add( (TypeValue) arg );
            }

            return new TupleTypeValue(valueTypes);
        }));
    }

    public static Value getBuiltin(String name) {
        Value function = GLOBAL_FUNCTIONS.get(name);
        if (function == null) throw new RuntimeException("Built-in " + name + " not found.");
        return function;
    }

    public static boolean isBuiltin(String name) {
        return GLOBAL_FUNCTIONS.containsKey(name);
    }

    // Binary Ops
    public static Value add(Value left, Value right) {
        if ( (left instanceof StringValue) || (right instanceof StringValue)) {
            return new StringValue(left.toString() + right.toString());
        } else if (left instanceof NumberValue<?> lNV && right instanceof NumberValue<?> rNV) {
            Number lN = lNV.value();
            Number rN = rNV.value();

            if (lN instanceof Integer && rN instanceof Integer) {
                return new IntValue(lN.intValue() + rN.intValue());
            }

            return new FloatValue(lN.doubleValue() + rN.doubleValue());
        }

        throw new UnsupportedOperationException("Cannot evaluate binary operation. Operation '+' is not supported for values type '" + left.type() + "', and '" + right.type() + "'.");
    }

    public static NumberValue<?> subtract(Value left, Value right) {
        if (left instanceof NumberValue<?> lNV && right instanceof NumberValue<?> rNV) {
            Number lN = lNV.value();
            Number rN = rNV.value();

            if (lN instanceof Integer && rN instanceof Integer) {
                return new IntValue(lN.intValue() - rN.intValue());
            }

            return new FloatValue(lN.doubleValue() - rN.doubleValue());
        }

        throw new UnsupportedOperationException("Cannot evaluate binary operation. Operation '-' is not supported for values type '" + left.type() + "', and '" + right.type() + "'.");
    }

    public static NumberValue<?> multiply(Value left, Value right) {
        if (left instanceof NumberValue<?> lNV && right instanceof NumberValue<?> rNV) {
            Number lN = lNV.value();
            Number rN = rNV.value();

            if (lN instanceof Integer && rN instanceof Integer) {
                return new IntValue(lN.intValue() * rN.intValue());
            }

            return new FloatValue(lN.doubleValue() * rN.doubleValue());
        }

        throw new UnsupportedOperationException("Cannot evaluate binary operation. Operation '*' is not supported for values type '" + left.type() + "', and '" + right.type() + "'.");
    }

    public static NumberValue<?> divide(Value left, Value right) {
        if (left instanceof NumberValue<?> lNV && right instanceof NumberValue<?> rNV) {
            Number lN = lNV.value();
            Number rN = rNV.value();

            if (lN instanceof Integer && rN instanceof Integer) {
                return new IntValue(lN.intValue() / rN.intValue());
            }

            return new FloatValue(lN.doubleValue() / rN.doubleValue());
        }

        throw new UnsupportedOperationException("Cannot evaluate binary operation. Operation '/' is not supported for values type '" + left.type() + "', and '" + right.type() + "'.");
    }

    public static NumberValue<?> module(Value left, Value right) {
        if (left instanceof NumberValue<?> lNV && right instanceof NumberValue<?> rNV) {
            Number lN = lNV.value();
            Number rN = rNV.value();

            if (lN instanceof Integer && rN instanceof Integer) {
                return new IntValue(lN.intValue() / rN.intValue());
            }

            return new FloatValue(lN.doubleValue() / rN.doubleValue());
        }

        throw new UnsupportedOperationException("Cannot evaluate binary operation. Operation '%' is not supported for values type '" + left.type() + "', and '" + right.type() + "'.");
    }

    public static NumberValue<?> power(Value left, Value right) {
        if (left instanceof NumberValue<?> lNV && right instanceof NumberValue<?> rNV) {
            Number lN = lNV.value();
            Number rN = rNV.value();

            if (lN instanceof Integer && rN instanceof Integer) {
                return new IntValue( (int)  Math.pow(lN.intValue(), rN.intValue()) );
            }

            return new FloatValue(Math.pow(lN.doubleValue(), rN.doubleValue()));
        }

        throw new UnsupportedOperationException("Cannot evaluate binary operation. Operation '^' is not supported for values type '" + left.type() + "', and '" + right.type() + "'.");
    }

    public static BoolValue and(Value left, Value right) {
        return (isTruthy(left) && isTruthy(right)) ? BoolValue.TRUE : BoolValue.FALSE;
    }

    public static BoolValue or(Value left, Value right) {
        return (isTruthy(left) || isTruthy(right)) ? BoolValue.TRUE : BoolValue.FALSE;
    }

    public static TypeIntersectionValue intersection(Value left, Value right) {
        if (left instanceof TypeValue leftType && right instanceof TypeValue rightType) {
            return new TypeIntersectionValue(leftType, rightType);
        }

        throw new UnsupportedOperationException("Cannot evaluate binary operation. Operation '&' is not supported for values type '" + left.type() + "', and '" + right.type() + "'.");
    }

    public static TypeUnionValue union(Value left, Value right) {
        if (left instanceof TypeValue leftType && right instanceof TypeValue rightType) {
            return new TypeUnionValue(leftType, rightType);
        }

        throw new UnsupportedOperationException("Cannot evaluate binary operation. Operation '&' is not supported for values type '" + left.type() + "', and '" + right.type() + "'.");
    }

    public static BoolValue is(Value left, Value right)

    // Unary Ops
    public static boolean isTruthy(Value value) {
        if (value instanceof BoolValue b)
            return b.value();

        return value != EmptyValue.NONE;
    }

    // Type safety
    public static void verifyType(Value val, TypeValue requiredType) {
        if (!requiredType.isAssignableFrom(val.type())) {
            throw new RuntimeException("Type '" + val.type() + "' is not assignable to '" + requiredType + "'");
        }
    }

    public static void assignIndex(Value iterable, Value index, Value newValue) {
        if (!(index instanceof NumberValue<?> indexNum)) {
            throw new RuntimeException("Index access must be a number.");
        }
        int i = indexNum.value().intValue();

        if (iterable instanceof ArrayValue(ArrayTypeValue type, List<Value> value)) {
            if (!type.value().isAssignableFrom(newValue.type())) {
                throw new RuntimeException("Type mismatch in list assignment.");
            }
            value.set(i, newValue);
        } else if (iterable instanceof TupleValue(TupleTypeValue type, List<Value> values)) {
            if (!type.types().get(i).isAssignableFrom(newValue.type())) {
                throw new RuntimeException("Type mismatch in tuple assignment.");
            }
            values.set(i, newValue);
        } else {
            throw new UnsupportedOperationException("Target is not indexable.");
        }
    }

    public static Value access(Value parent, String name) {
        return switch (parent) {
            case ObjectValue(Map<String, Value> value) -> {
                if (value.containsKey(name)) {
                    yield value.get(name);
                }
                throw new RuntimeException("Cannot evaluate access operation. Symbol '" + name + "' not found in the parent scope.");
            }
            case EnumValue enumValue -> {
                if (enumValue.names().contains(name)) {
                    yield  new EnumInstanceValue(name, enumValue);
                }
                throw new RuntimeException("Cannot evaluate access operation. Symbol '" + name + "' not found in the parent scope.");
            }
            case null, default -> throw new UnsupportedOperationException("Cannot evaluate access operation. Operation 'access' is not defined for parent type '" + parent.type() + "'.");
        };
    }

    public static Value invoke(Value callee, List<Value> args) {
        return switch (callee) {
            case FunctionValue(boolean vararg, SignatureValue type, java.util.function.Function<List<Value>, Value> function) -> {
                if (!vararg && type.parameterTypes().size() != args.size()) {
                    throw new RuntimeException("Argument arity " + args.size() + " incompatible with parameter arity " + type.parameterTypes().size());
                }

                for (int i = 0; i < (vararg ? type.parameterTypes().size() - 1 : args.size()); i++) {
                    if (!type.parameterTypes().get(i).isAssignableFrom(args.get(i).type())) {
                        throw new RuntimeException("Argument type '" + args.get(i).type() + "' not assignable to parameter type '" + type.parameterTypes().get(i) + "'.");
                    }
                }

                if (vararg) {
                    TypeValue varargType = type.parameterTypes().getLast();
                    for (int i = type.parameterTypes().size() - 1; i < args.size(); i++) {
                        if (!varargType.isAssignableFrom(args.get(i).type()))
                            throw new RuntimeException("Argument type '" + args.get(i).type() + "' not assignable to parameter type '" + type.parameterTypes().get(i) + "'.");
                    }
                }

                Value result = function.apply(args);

                if (!type.returnType().isAssignableFrom(result.type())) {
                    throw new RuntimeException("Return type '" + result.type() + "' not assignable to '" + type.returnType() + "'.");
                }

                yield result;
            }

            // TODO: Add user defined function support

            case ClassValue(Map<String, TypeValue> value) -> {
                Map<String, Value> instanceBinding = new LinkedHashMap<>();
                List<Map.Entry<String, TypeValue>> fields = List.copyOf(value.entrySet());

                for (int i = 0; i < args.size(); i++) {
                    Value arg = args.get(i);
                    TypeValue fieldType = fields.get(i).getValue();

                    if (fieldType.isAssignableFrom(arg.type())) {
                        instanceBinding.put(fields.get(i).getKey(), arg);
                    } else {
                        throw new RuntimeException("Argument type '" + arg.type() + "' incompatible with field '" + fields.get(i).getKey() + "'.");
                    }
                }
                yield new ObjectValue(instanceBinding);
            }
            case null, default -> throw new UnsupportedOperationException("Call operation not supported for type '" + callee.type() + "'.");
        };
    }
}
