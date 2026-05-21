package fuji.codegen;

import java.lang.reflect.Method;

public class JvmClassLoader extends ClassLoader {
    public Class<?> defineClass(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }

    public static void execute(String className, byte[] byteCode) {
        try {
            JvmClassLoader loader = new JvmClassLoader();
            // 1. Load the class into the JVM
            Class<?> clazz = loader.defineClass(className, byteCode);

            // 2. Find the entry point method
            // Assuming your compiler wrapped the ProgramNode in a 'main' method
            Method method = clazz.getMethod("main", String[].class);

            // 3. Execute!
            // Your project's JAR/classes must be in the classpath so 'Value' can be resolved.
            String[] args = new String[0];
            Object result = method.invoke(null, (Object) args);

            System.err.println("Program exited with: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
