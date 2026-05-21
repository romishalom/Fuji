package fuji.util;

import java.util.*;

public class BuiltInRegistry {
    private static final Map<String, Value> registry = new HashMap<>();
    static {
        // Basic types
        registry.put("Any", PrimitiveType.ANY);
        registry.put("Void", PrimitiveType.VOID);
        registry.put("Empty", PrimitiveType.EMPTY);
        registry.put("Bool", PrimitiveType.BOOL);
        registry.put("Int", PrimitiveType.INT);
        registry.put("Float", PrimitiveType.FLOAT);
        registry.put("String", PrimitiveType.STRING);
        registry.put("Type", PrimitiveType.TYPE);

        SignatureValue arraySig = new SignatureValue(List.of(PrimitiveType.TYPE), PrimitiveType.TYPE);
        registry.put("Array", new FunctionValue(false, arraySig, (args) -> new ArrayTypeValue((TypeValue) args.getFirst())));

        SignatureValue innerFunctionSig = new SignatureValue(List.of(PrimitiveType.TYPE), PrimitiveType.TYPE);
        SignatureValue functionSig = new SignatureValue(List.of(PrimitiveType.TYPE), innerFunctionSig);
        registry.put("Function", new FunctionValue(true, functionSig, (args) -> {
            List<TypeValue> paramTypes = new ArrayList<>();
            for (Value arg: args) {
                paramTypes.add( (TypeValue) arg );
            }

            return new FunctionValue(false, innerFunctionSig, (innerArgs) -> new SignatureValue(paramTypes, (TypeValue) innerArgs.getFirst()));
        }));

        SignatureValue tupleSig = new SignatureValue(List.of(PrimitiveType.TYPE), PrimitiveType.TYPE);
        registry.put("Tuple", new FunctionValue(true, tupleSig, (args) -> {
            List<TypeValue> valueTypes = new ArrayList<>();
            for (Value arg: args) {
                valueTypes.add( (TypeValue) arg );
            }

            return new TupleTypeValue(valueTypes);
        }));

        // Values
        registry.put("unit", VoidValue.UNIT);
        registry.put("none", EmptyValue.NONE);

        registry.put("true", BoolValue.TRUE);
        registry.put("false", BoolValue.FALSE);

        // TODO: Common util libs and math
        // registry.put("Math", );


        // Define println logic
        SignatureValue printlnSig = new SignatureValue(List.of(PrimitiveType.ANY), PrimitiveType.VOID);
        registry.put("println", new FunctionValue(true, printlnSig, (args) -> {
            StringJoiner sj = new StringJoiner(" ");
            for (Value arg : args) {
                sj.add(arg.toString());
            }

            System.out.println(sj);
            return VoidValue.UNIT;
        }));

        SignatureValue printSig = new SignatureValue(List.of(PrimitiveType.ANY), PrimitiveType.VOID);
        registry.put("print", new FunctionValue(true, printSig, (args) -> {
            StringJoiner sj = new StringJoiner(" ");
            for (Value arg : args) {
                sj.add(arg.toString());
            }

            System.out.print(sj);
            return VoidValue.UNIT;
        }));

        SignatureValue printfSig = new SignatureValue(List.of(PrimitiveType.ANY), PrimitiveType.VOID);
        registry.put("printf", new FunctionValue(true, printfSig, (args) -> {
            String format = null;
            Object[] valueStrings = new String[args.size()-1];

            for (int i = 0; i < args.size(); i++) {
                String argString = args.get(i).toString();
                if (i == 0) {
                    format = argString;
                } else {
                    valueStrings[i-1] = argString;
                }
            }

            System.out.printf(format, valueStrings);

            return VoidValue.UNIT;
        }));

        // Define input logic
        Scanner sc = new Scanner(System.in);
        SignatureValue inputSig = new SignatureValue(List.of(), PrimitiveType.STRING);
        registry.put("input", new FunctionValue(false, inputSig, (args) -> new StringValue(sc.next())));

        SignatureValue typeofSig = new SignatureValue(List.of(PrimitiveType.ANY), PrimitiveType.TYPE);
        registry.put("typeof", new FunctionValue(false, typeofSig, (args) -> args.getFirst().type()));
    }

    public static boolean isBuiltIn(String name) {
        return registry.containsKey(name);
    }

    public static Value getBuiltIn(String name) {
        return registry.get(name);
    }
}
