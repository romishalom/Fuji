package fuji.codegen;

import com.romishalom.ast.*;
import com.romishalom.ast.expression.*;
import com.romishalom.ast.controlflow.*;
import com.romishalom.util.*;
import fuji.ast.*;
import fuji.ast.controlflow.*;
import fuji.ast.expression.*;
import fuji.runtime.RuntimeOps;
import fuji.util.*;
import org.objectweb.asm.*;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class JvmCompiler implements Visitor<Void> {

    private final ClassWriter cw;
    private MethodVisitor mv;
    private final String className;

    // Tracks local variables: Name -> Index in JVM stack frame
    private final Map<String, Integer> localVariables = new HashMap<>();
    private int nextLocalIndex = 1; // 0 is usually 'this' or 'args'

    private int lambdaCount = 0;

    private Label currentBreakLabel = null;
    private Label currentContinueLabel = null;

    public JvmCompiler(String className) {
        this.className = className;
        this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    }

    public byte[] compile(ProgramNode program) {
        // Class header
        cw.visit(V21, ACC_PUBLIC, className, null, "java/lang/Object", null);

        // Create a 'main' method as the entry point
        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();

        program.accept(this);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();

        return cw.toByteArray();
    }

    @Override
    public Void visit(LiteralNode node) {
        Value value = node.value();

        // 1. Handle Integers (IntValue)
        switch (value) {
            case IntValue(Integer intVal) -> {
                mv.visitTypeInsn(NEW, "fuji/util/IntValue");
                mv.visitInsn(DUP);
                // Load the primitive int onto the stack
                mv.visitLdcInsn(intVal);
                // Box the primitive int into a java.lang.Integer
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                // Call IntValue constructor: new IntValue(Integer)
                mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/IntValue", "<init>", "(Ljava/lang/Integer;)V", false);
            }

            // 2. Handle Floats/Doubles (FloatValue)
            case FloatValue(Double doubleVal) -> {
                mv.visitTypeInsn(NEW, "fuji/util/FloatValue");
                mv.visitInsn(DUP);
                // Load the primitive double onto the stack
                mv.visitLdcInsn(doubleVal);
                // Box the primitive double into a java.lang.Double
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                // Call FloatValue constructor: new FloatValue(Double)
                mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/FloatValue", "<init>", "(Ljava/lang/Double;)V", false);
            }

            // 3. Handle Strings (StringValue)
            case StringValue(String stringVal) -> {
                mv.visitTypeInsn(NEW, "fuji/util/StringValue");
                mv.visitInsn(DUP);
                // Load the Java String literal
                mv.visitLdcInsn(stringVal);
                // Call StringValue constructor: new StringValue(String)
                mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/StringValue", "<init>", "(Ljava/lang/String;)V", false);
            }

            // 4. Handle Booleans (BoolValue)
            case BoolValue(Boolean boolVal) -> {
                // Use the static constants TRUE/FALSE defined in your BoolValue class
                String fieldName = boolVal ? "TRUE" : "FALSE";
                mv.visitFieldInsn(GETSTATIC, "fuji/util/BoolValue", fieldName, "Lcom/romishalom/common/BoolValue;");
            }

            // 5. Handle Unit/Void (VoidValue)
            case VoidValue voidValue ->
                // Usually a singleton: VoidValue.UNIT
                    mv.visitFieldInsn(GETSTATIC, "fuji/util/VoidValue", "UNIT", "Lcom/romishalom/common/VoidValue;");


            // 6. Handle None/Empty (EmptyValue)
            case EmptyValue emptyValue ->
                // Usually a singleton: EmptyValue.NONE
                    mv.visitFieldInsn(GETSTATIC, "fuji/util/EmptyValue", "NONE", "Lcom/romishalom/common/EmptyValue;");


            // 7. Handle Type Literals (PrimitiveType / TypeValue)
            // In your interpreter, types are first-class values (e.g., used in 'is' or 'as' operations)
            case PrimitiveType primType ->
                // Assuming PrimitiveType is an enum or has static fields for INT, STRING, etc.
                    mv.visitFieldInsn(GETSTATIC, "fuji/util/PrimitiveType", primType.name(), "Lcom/romishalom/common/PrimitiveType;");

            case null, default -> {}
        }

        return null;
    }

    @Override
    public Void visit(UnaryOperationNode node) {
        // TODO: implement
        return null;
    }

    @Override
    public Void visit(DeclarationNode node) {
        // 1. Evaluate the Initializer
        if (node.initializer() != null) {
            node.initializer().accept(this);
        } else {
            // If no initializer, default to EmptyValue.NONE as seen in the interpreter logic
            mv.visitFieldInsn(GETSTATIC, "fuji/util/EmptyValue", "NONE", "Lcom/romishalom/common/EmptyValue;");
        }

        // Stack now has the [Value] to be assigned

        // 2. Handle the Target
        switch (node.target()) {
            case ReferenceNode(String name) -> handleLocalDeclaration(name, node);
            case AccessNode accessNode -> handleExtensionDeclaration(accessNode);
            default ->
                    throw new RuntimeException("Unsupported declaration target: " + node.target().getClass().getSimpleName());
        }

        return null;
    }

    private void handleLocalDeclaration(String name, DeclarationNode node) {
        // Check if the symbol already exists in our compile-time local map
        if (localVariables.containsKey(name)) {
            throw new RuntimeException("Cannot execute declaration. Symbol '" + name + "' already exists");
        }

        // In your interpreter, declarations also involve type checking.
        // If a type is explicitly provided, we should ideally validate it at runtime.
        if (node.type() != null) {
            // Duplicate the values for the check
            mv.visitInsn(DUP);
            // Evaluate the TypeNode to get a TypeValue on the stack
            node.type().accept(this);
            // Call Runtime check: TypeValue.isAssignableFrom(Value.type())
            mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps", "verifyType",
                    "(Lcom/romishalom/common/Value;Lcom/romishalom/common/TypeValue;)V", false);
        }

        // Store the Value in a new JVM local slot
        int index = nextLocalIndex++;
        localVariables.put(name, index);
        mv.visitVarInsn(ASTORE, index);
    }

    private void handleExtensionDeclaration(AccessNode target) {
        // This handles: parent.name = values (where 'name' is a new property)

        // The initializer values is currently at the top of the stack.
        // We need to keep it, but evaluate the parent first.
        // Use a temporary local to hold the values so we can evaluate the parent.
        int tempValueIdx = nextLocalIndex++;
        mv.visitVarInsn(ASTORE, tempValueIdx);

        // Evaluate parent (must evaluate to an ObjectValue)
        target.parent().accept(this);
        mv.visitTypeInsn(CHECKCAST, "fuji/util/ObjectValue");

        // Get the internal Map: values()
        mv.visitMethodInsn(INVOKEVIRTUAL, "fuji/util/ObjectValue", "values", "()Ljava/util/Map;", false);

        // Push the property name (String)
        mv.visitLdcInsn(target.name());

        // Reload the initializer values
        mv.visitVarInsn(ALOAD, tempValueIdx);

        // Map.put(name, values)
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitInsn(POP); // Pop previous values returned by put()
    }

    @Override
    public Void visit(AssignmentNode node) {
        // 1. Evaluate the RHS values first
        node.value().accept(this);

        // We need to keep this values on the stack to return it at the end,
        // but we also need it for the assignment logic. Let's store it in a temp local.
        int rhsIdx = nextLocalIndex++;
        mv.visitVarInsn(ASTORE, rhsIdx);

        // 2. Handle the target based on its type
        switch (node.target()) {
            case ReferenceNode(String name) -> handleVariableAssignment(name, rhsIdx);
            case AccessNode access -> handlePropertyAssignment(access, rhsIdx);
            case IndexAccessNode indexAccess -> handleIndexAssignment(indexAccess, rhsIdx);
            case null, default -> throw new UnsupportedOperationException("Assignment target not defined.");
        }

        // 3. Assignment expressions return the values that was assigned
        mv.visitVarInsn(ALOAD, rhsIdx);
        return null;
    }

    private void handleVariableAssignment(String name, int rhsIdx) {
        Integer localIdx = localVariables.get(name);
        if (localIdx == null) {
            throw new RuntimeException("Cannot execute assignment. Symbol '" + name + "' not found.");
        }

        // Note: To be 100% faithful to your interpreter, we'd need a way to
        // track 'isConstant' at compile-time to throw the "Symbol is a constant" error.
        mv.visitVarInsn(ALOAD, rhsIdx);
        mv.visitVarInsn(ASTORE, localIdx);
    }

    private void handlePropertyAssignment(AccessNode access, int rhsIdx) {
        // Evaluate the parent object
        access.parent().accept(this);
        mv.visitTypeInsn(CHECKCAST, "fuji/util/ObjectValue");

        // Get the internal Map: values() (Assumes ObjectValue has a values() method)
        mv.visitMethodInsn(INVOKEVIRTUAL, "fuji/util/ObjectValue", "values", "()Ljava/util/Map;", false);

        // Push the key and the values
        mv.visitLdcInsn(access.name());
        mv.visitVarInsn(ALOAD, rhsIdx);

        // Map.put(key, values)
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitInsn(POP); // Pop the previous values returned by put()
    }

    private void handleIndexAssignment(IndexAccessNode indexAccess, int rhsIdx) {
        // Evaluating the iterable, the index, and the values to set.
        // Because your interpreter has complex logic (List vs Tuple + Type Assignability),
        // we delegate this to a Runtime helper to keep the bytecode clean.

        indexAccess.list().accept(this);   // The iterable (List or Tuple)
        indexAccess.index().accept(this);  // The index (NumberValue)
        mv.visitVarInsn(ALOAD, rhsIdx);    // The values to assign

        // Invoke RuntimeOps.assignIndex(iterable, index, values)
        mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps", "assignIndex",
                "(Lcom/romishalom/common/Value;Lcom/romishalom/common/Value;Lcom/romishalom/common/Value;)V", false);
    }

    @Override
    public Void visit(ReturnStatementNode node) {
        return null;
    }

    @Override
    public Void visit(BreakNode node) {
        if (currentBreakLabel == null) throw new RuntimeException("Break outside of loop");
        mv.visitJumpInsn(GOTO, currentBreakLabel);
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        if (currentContinueLabel == null) throw new RuntimeException("Continue outside of loop");
        mv.visitJumpInsn(GOTO, currentContinueLabel);
        return null;
    }

    @Override
    public Void visit(ReferenceNode node) {
        String name = node.name();

        if (localVariables.containsKey(name)) {
            // Standard local variable load
            mv.visitVarInsn(ALOAD, localVariables.get(name));
        } else if (RuntimeOps.isBuiltin(name)) {
            // Call RuntimeOps to get the built-in function object
            mv.visitLdcInsn(name);
            mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps",
                    "getBuiltin", "(Ljava/lang/String;)Lcom/romishalom/common/Value;", false);
        } else {
            throw new RuntimeException("Symbol '" + name + "' not found.");
        }
        return null;
    }

    @Override
    public Void visit(ArrayNode node) {
        // 1. Prepare to create the ListValue
        mv.visitTypeInsn(NEW, "fuji/util/ListValue");
        mv.visitInsn(DUP);

        // TODO: find a way to load the ListTypeValue

        // 2. Create the ArrayList for bindings
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);

        // 3. Populate the List
        for (ExpressionNode item :node.items()) {
            mv.visitInsn(DUP); // Duplicate Map reference for the 'add' call

            // Push Value (Evaluate the expression node)
            item.accept(this);

            // Call List.add(Object values)
            // Note: List.add returns a boolean, so we must POP it
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add",
                    "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
        }

        // 4. Initialize ListValue with the populated list
        // Constructor signature: ListValue(ListTypeValue type, List<Value> values)
        mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/ListValue", "<init>",
                "(Lcom/romishalom/common/ListTypeValue;Ljava/util/List;)V", false);

        return null;
    }

    @Override
    public Void visit(BinaryOperationNode node) {
        // Load Left and Right operands onto the stack
        node.leftOperand().accept(this);
        node.rightOperand().accept(this);

        // Instead of writing massive switch-cases in Bytecode,
        // we call a Runtime dispatcher that mimics your Interpreter's logic.
        String descriptor = "(Lcom/romishalom/common/Value;Lcom/romishalom/common/Value;)Lcom/romishalom/common/Value;";

        switch (node.operator()) {
            case PLUS -> mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps", "add", descriptor, false);
            case MINUS -> mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps", "sub", descriptor, false);
            case STAR -> mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps", "mul", descriptor, false);
            case EQUALS -> mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps", "eq", descriptor, false);
            // ... Add other operators
        }
        return null;
    }

    @Override
    public Void visit(IfExpressionNode node) {
        Label elseLabel = new Label();
        Label endLabel = new Label();

        // Condition
        node.condition().accept(this);
        // Call isTruthy helper
        mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps", "isTruthy", "(Lcom/romishalom/common/Value;)Z", false);
        mv.visitJumpInsn(IFEQ, elseLabel);

        // If body
        node.pass().accept(this);
        mv.visitJumpInsn(GOTO, endLabel);

        // Else body
        mv.visitLabel(elseLabel);
        if (node.fail() != null) {
            node.fail().accept(this);
        }

        mv.visitLabel(endLabel);

        return null;
    }

    @Override
    public Void visit(IndexAccessNode node) {
        return null;
    }

    @Override
    public Void visit(StructBlockNode node) {
        mv.visitTypeInsn(NEW, "fuji/util/StructValue");
        mv.visitInsn(DUP);

        mv.visitTypeInsn(NEW, "java/util/LinkedHashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "()V", false);

        for (ParameterNode field : node.fields()) {
            mv.visitInsn(DUP); // Duplicate Map reference for the 'put' call

            // Push Key (String)
            mv.visitLdcInsn(field.name());

            // Push Value (Evaluate the expression node)
            field.type().accept(this);

            // Call Map.put(Object key, Object values)
            // Map.put returns the previous values, so we must POP it
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
        }

        // Init
        mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/StructValue", "<init>",
                "(Ljava/util/Map;)V", false);

        return null;
    }

    @Override
    public Void visit(EnumBlockNode node) {
        mv.visitTypeInsn(NEW, "fuji/util/EnumValue");
        mv.visitInsn(DUP);

        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);

        for (String name: node.names()) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(name);

            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add",
                    "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
        }

        // Init
        mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/EnumValue", "<init>",
                "(Ljava/util/List;)V", false);

        return null;
    }

    /*@Override
    public Void visit(SignatureNode node) {
        mv.visitTypeInsn(NEW, "com/romishalom/common/SignatureValue");
        mv.visitInsn(DUP);

        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);

        for (ExpressionNode parameterType : node.parameterTypes()) {
            mv.visitInsn(DUP);
            parameterType.accept(this);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP); // Pop the boolean result of add()
        }

        node.returnType().accept(this);

        mv.visitMethodInsn(INVOKESPECIAL, "com/romishalom/common/SignatureValue", "<init>",
                "(Ljava/util/List;Lcom/romishalom/common/TypeValue;)V", false);

        return null;
    }

    @Override
    public Void visit(ListTypeNode node) {
        mv.visitTypeInsn(NEW, "com/romishalom/common/ListTypeValue");
        mv.visitInsn(DUP);

        node.arrayType().accept(this);

        mv.visitMethodInsn(INVOKESPECIAL, "com/romishalom/common/ListTypeValue", "<init>",
                "(Lcom/romishalom/common/TypeValue;)V", false);

        return null;
    }

    @Override
    public Void visit(TupleTypeNode node) {
        mv.visitTypeInsn(NEW, "com/romishalom/common/TupleTypeValue");
        mv.visitInsn(DUP);

        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);

        for (ExpressionNode type : node.types()) {
            mv.visitInsn(DUP); // Duplicate list reference
            type.accept(this);  // Evaluate arg
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP); // Pop the boolean result of add()
        }

        mv.visitMethodInsn(INVOKESPECIAL, "com/romishalom/common/TupleTypeValue", "<init>",
                "(Ljava/util/List;)V", false);

        return null;
    }*/

    @Override
    public Void visit(TupleNode node) {
        // 1. Prepare to create the ListValue
        mv.visitTypeInsn(NEW, "fuji/util/TupleValue");
        mv.visitInsn(DUP);

        // 2. Create the ArrayList for bindings
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);

        // 3. Populate the List
        for (ExpressionNode value : node.values()) {
            mv.visitInsn(DUP); // Duplicate Map reference for the 'add' call

            // Push Value (Evaluate the expression node)
            value.accept(this);

            // Call List.add(Object values)
            // Note: List.add returns a boolean, so we must POP it
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add",
                    "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
        }

        // 4. Initialize ListValue with the populated list
        // Constructor signature: ListValue(ListTypeValue type, List<Value> values)
        mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/ListValue", "<init>",
                "(Lcom/romishalom/common/ListTypeValue;Ljava/util/List;)V", false);

        return null;
    }

    @Override
    public Void visit(IfStatementNode node) {
        Label elseLabel = new Label();
        Label endLabel = new Label();

        // Condition
        node.condition().accept(this);
        // Call isTruthy helper
        mv.visitMethodInsn(INVOKESTATIC, "fuji/runtime/RuntimeOps", "isTruthy", "(Lcom/romishalom/common/Value;)Z", false);
        mv.visitJumpInsn(IFEQ, elseLabel);

        // If body
        node.pass().accept(this);
        mv.visitJumpInsn(GOTO, endLabel);

        // Else body
        mv.visitLabel(elseLabel);
        if (node.fail() != null) {
            node.fail().accept(this);
        }

        mv.visitLabel(endLabel);
        return null;
    }

    @Override
    public Void visit(CallNode node) {
        // 1. Evaluate the Callee (the function or struct being called)
        node.callee().accept(this); // Stack: [Value (callee)]

        // 2. Prepare an ArrayList to hold evaluated arguments
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        // Stack: [Value (callee), List (args)]

        // 3. Evaluate each argument and add it to the list
        for (ExpressionNode argument : node.arguments()) {
            mv.visitInsn(DUP); // Duplicate List reference
            argument.accept(this); // Evaluate argument -> Stack: [..., List, List, Value (arg)]
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP); // Pop the boolean result of List.add()
        }

        // 4. Call the Runtime Helper to execute the call
        // Descriptor: (Value callee, List args) -> Value
        mv.visitMethodInsn(
                INVOKESTATIC,
                "fuji/runtime/RuntimeOps",
                "invoke",
                "(Lcom/romishalom/common/Value;Ljava/util/List;)Lcom/romishalom/common/Value;",
                false
        );

        // The result of the call is now on top of the stack.
        return null;
    }

    @Override
    public Void visit(FunctionNode node) {
        // --- 1. Instantiate SignatureValue ---
        // We must NEW the SignatureValue FIRST so it's at the bottom of the args on the stack
        mv.visitTypeInsn(NEW, "fuji/util/SignatureValue");
        mv.visitInsn(DUP);

        // Create the parameter types list: List<TypeValue>
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);

        for (ParameterNode parameter : node.parameters()) {
            mv.visitInsn(DUP); // Duplicate ArrayList ref for the 'add' call
            parameter.type().accept(this); // Push TypeValue
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP); // Pop boolean result of add()
        }

        // Evaluate the return type
        if (node.returnType() != null) {
            node.returnType().accept(this);
        } else {
            mv.visitFieldInsn(GETSTATIC, "fuji/util/PrimitiveType", "ANY", "Lcom/romishalom/common/PrimitiveType;");
        }

        // Stack is now: [Uninit SignatureValue, Uninit SignatureValue, ArrayList, ReturnType]
        // Call: SignatureValue.<init>(List, TypeValue)
        mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/SignatureValue", "<init>",
                "(Ljava/util/List;Lcom/romishalom/common/TypeValue;)V", false);

        // Store the resulting SignatureValue in a temp local to clear the stack for InvokeDynamic
        int sigIdx = nextLocalIndex++;
        mv.visitVarInsn(ASTORE, sigIdx);

        // --- 2. Generate the Lambda Function ---
        String lambdaName = "lambda$" + (lambdaCount++);

        Handle bootstrap = new Handle(
                H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false
        );

        // Generate java.util.function.Function
        mv.visitInvokeDynamicInsn(
                "apply",
                "()Ljava/util/function/Function;",
                bootstrap,
                Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;"), // SAM Method
                new Handle(H_INVOKESTATIC, className, lambdaName, "(Ljava/util/List;)Lcom/romishalom/common/Value;", false), // Implementation
                Type.getMethodType("(Ljava/util/List;)Lcom/romishalom/common/Value;") // Instantiated signature
        );

        // Store the Function in a temp local
        int funcIdx = nextLocalIndex++;
        mv.visitVarInsn(ASTORE, funcIdx);

        // --- 3. Instantiate NativeLambdaValue ---
        mv.visitTypeInsn(NEW, "fuji/util/NativeLambdaValue");
        mv.visitInsn(DUP);
        mv.visitInsn(node.vararg()? ICONST_1 : ICONST_0);
        mv.visitVarInsn(ALOAD, sigIdx);  // Load SignatureValue
        mv.visitVarInsn(ALOAD, funcIdx); // Load Function

        // Call: NativeLambdaValue.<init>(SignatureValue, Function)
        mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/NativeLambdaValue", "<init>",
                "(ZLcom/romishalom/common/SignatureValue;Ljava/util/function/Function;)V", false);

        // --- 4. Compile the Lambda Body (as before) ---
        compileLambdaBody(lambdaName, node);

        return null;
    }

    private void compileLambdaBody(String name, FunctionNode node) {
        // Save current state
        MethodVisitor oldMv = this.mv;
        Map<String, Integer> oldLocals = new HashMap<>(localVariables);
        int oldNextIdx = nextLocalIndex;

        // Create a new method for the lambda body
        // Signature: Value lambda(List<Value> args)
        mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, name, "(Ljava/util/List;)Lcom/romishalom/common/Value;", null, null);
        mv.visitCode();

        // Reset local variable mapping for the new scope
        localVariables.clear();
        nextLocalIndex = 1; // Slot 0 is the 'List' argument

        // Map parameters from the List into JVM local variables
        for (int i = 0; i < node.parameters().size(); i++) {
            String paramName = node.parameters().get(i).name();
            mv.visitVarInsn(ALOAD, 0); // Load the list
            mv.visitLdcInsn(i);        // Load index
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "fuji/util/Value");

            int localIdx = nextLocalIndex++;
            localVariables.put(paramName, localIdx);
            mv.visitVarInsn(ASTORE, localIdx);
        }

        // Compile the body expression
        node.body().accept(this);

        // Final Return
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Restore state
        this.mv = oldMv;
        this.localVariables.putAll(oldLocals);
        this.nextLocalIndex = oldNextIdx;
    }

    // --- Unimplemented Boilerplate for brevity ---
    @Override
    public Void visit(ProgramNode node) {
        for (StatementNode s : node.statements()) {
            s.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(ParameterNode node) {
        return null;
    }

    @Override
    public Void visit(PassNode node) {
        return null;
    }

    @Override
    public Void visit(PropertyNode node) {
        return null;
    }

    @Override public Void visit(BlockNode node) {
        for (StatementNode s : node.body()) {
            s.accept(this);
            if (s instanceof ReturnStatementNode) break;
        }

        return null;
    }

    @Override
    public Void visit(ObjectBlockNode node) {
        // 1. Prepare to create the ObjectValue
        mv.visitTypeInsn(NEW, "fuji/util/ObjectValue");
        mv.visitInsn(DUP);

        // 2. Create the LinkedHashMap for bindings
        mv.visitTypeInsn(NEW, "java/util/LinkedHashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "()V", false);

        // 3. Populate the Map
        for (PropertyNode property : node.properties()) {
            mv.visitInsn(DUP); // Duplicate Map reference for the 'put' call

            // Push Key (String)
            mv.visitLdcInsn(property.name());

            // Push Value (Evaluate the expression node)
            property.value().accept(this);

            // Call Map.put(Object key, Object values)
            // Note: LinkedHashMap.put returns the previous values, so we must POP it
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
        }

        // 4. Initialize ObjectValue with the populated Map
        // Constructor signature: ObjectValue(Map<String, Value> values)
        mv.visitMethodInsn(INVOKESPECIAL, "fuji/util/ObjectValue", "<init>",
                "(Ljava/util/Map;)V", false);

        return null;
    }

    @Override
    public Void visit(AccessNode node) {
        // 1. Evaluate the parent expression (e.g., the 'obj' in 'obj.prop')
        // This pushes a com.romishalom.common.Value onto the stack.
        node.parent().accept(this);

        // 2. Load the property name as a String literal onto the stack.
        mv.visitLdcInsn(node.name());

        // 3. Call the Runtime Helper to perform the lookup.
        // Descriptor: (Value parent, String name) -> Value
        mv.visitMethodInsn(
                INVOKESTATIC,
                "fuji/runtime/RuntimeOps",
                "access",
                "(Lcom/romishalom/common/Value;Ljava/lang/String;)Lcom/romishalom/common/Value;",
                false
        );

        // The result (the property values or the Enum instance) is now on top of the stack.
        return null;
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        node.expression().accept(this);
        mv.visitInsn(POP);
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node) {
        Label startLabel = new Label();
        Label bodyLabel = new Label();
        Label endLabel = new Label();

        // In ASM, we need to track the labels for 'break' and 'continue'
        // so nested loops know where to jump.
        Label oldBreak = this.currentBreakLabel;
        Label oldContinue = this.currentContinueLabel;

        this.currentBreakLabel = endLabel;
        this.currentContinueLabel = startLabel;

        // 1. Mark the start of the loop
        mv.visitLabel(startLabel);

        // 2. Handle Condition Logic
        if (node.isPostCondition()) {
            // If it's a post-condition (do-while), go straight to body first time
            mv.visitJumpInsn(GOTO, bodyLabel);
        } else {
            // Evaluate condition -> Stack: [BoolValue]
            node.condition().accept(this);

            // Convert Value to primitive boolean for jumping
            mv.visitMethodInsn(INVOKESTATIC, "com/romishalom.runtime.Interpreter", "isTruthy", "(Lcom/romishalom/common/Value;)Z", false);

            // If false (0), jump to end
            mv.visitJumpInsn(IFEQ, endLabel);
        }

        // 3. Loop Body
        mv.visitLabel(bodyLabel);
        node.body().accept(this);

        // 4. Repeat
        // If it was a post-condition, we check the condition AFTER the body
        if (node.isPostCondition()) {
            node.condition().accept(this);
            mv.visitMethodInsn(INVOKESTATIC, "com/romishalom.runtime.Interpreter", "isTruthy", "(Lcom/romishalom/common/Value;)Z", false);
            mv.visitJumpInsn(IFNE, bodyLabel); // If true, go back to body
        } else {
            mv.visitJumpInsn(GOTO, startLabel);
        }

        // 5. End of Loop
        mv.visitLabel(endLabel);

        // Restore labels for outer loops
        this.currentBreakLabel = oldBreak;
        this.currentContinueLabel = oldContinue;

        return null;
    }

    @Override
    public Void visit(ForLoopNode node) {
        Label startLabel = new Label();
        Label endLabel = new Label();
        Label continueLabel = new Label(); // Continue jumps to the next iteration check

        // Save old labels for nested loop support
        Label oldBreak = this.currentBreakLabel;
        Label oldContinue = this.currentContinueLabel;
        this.currentBreakLabel = endLabel;
        this.currentContinueLabel = continueLabel;

        // 1. Evaluate the iterable (must result in a ListValue)
        node.iterable().accept(this);
        // Stack: [ListValue]

        // 2. Extract the java.util.List from ListValue
        // Accessing the 'list' field via its getter or field access
        mv.visitMethodInsn(INVOKEVIRTUAL, "fuji/util/ListValue", "values", "()Ljava/util/List;", false);
        // Stack: [List]

        // 3. Get the Iterator
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;", true);
        int iteratorIdx = nextLocalIndex++;
        mv.visitVarInsn(ASTORE, iteratorIdx);
        // Stack: []

        // 4. Start of Loop
        mv.visitLabel(startLabel);
        mv.visitLabel(continueLabel);

        // Check if iterator has next
        mv.visitVarInsn(ALOAD, iteratorIdx);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        mv.visitJumpInsn(IFEQ, endLabel); // If !hasNext(), jump to end

        // 5. Get next element and store in the iteration symbol
        mv.visitVarInsn(ALOAD, iteratorIdx);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
        mv.visitTypeInsn(CHECKCAST, "fuji/util/Value");

        // Declare/Store in the local variable slot for node.iterationSymbolName()
        int symbolIdx = nextLocalIndex++;
        localVariables.put(node.iterationSymbolName(), symbolIdx);
        mv.visitVarInsn(ASTORE, symbolIdx);

        // 6. Visit Loop Body
        node.body().accept(this);

        // 7. Loop back
        mv.visitJumpInsn(GOTO, startLabel);

        // 8. End of Loop
        mv.visitLabel(endLabel);

        // Restore labels
        this.currentBreakLabel = oldBreak;
        this.currentContinueLabel = oldContinue;

        return null;
    }
}
