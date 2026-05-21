package fuji;

import fuji.ast.ProgramNode;
import fuji.codegen.JvmClassLoader;
import fuji.codegen.JvmCompiler;
import fuji.parser.Lexer;
import fuji.parser.Parser;
import fuji.parser.Token;
import fuji.runtime.Interpreter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String s = Files.readString(Path.of("C:\\Users\\Romi Shalom\\IdeaProjects\\Fuji\\src\\main\\java\\com\\romishalom\\Test.fuji"));
        Lexer l = new Lexer(s);
        List<Token> tokens = l.lex();
        System.out.println(tokens);
        Parser p = new Parser(tokens);
        ProgramNode program = p.parse();
        System.out.println(program);
        Interpreter i = new Interpreter();
        System.err.println("Interpreter execution:");
        long startTime = System.currentTimeMillis();
        i.interpret(program);
        long endTime = System.currentTimeMillis();

        System.err.println("Runtime: " + (endTime - startTime) / 1000.0 + " seconds.");

        JvmCompiler compiler = new JvmCompiler("MyClass");
        byte[] bytecode = compiler.compile(program);
        /*try (FileOutputStream fos = new FileOutputStream("MyClass")) {
            fos.write(bytecode);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        System.err.println("Compiler execution:");
        long compilerStartTime = System.currentTimeMillis();
        JvmClassLoader.execute("MyClass", bytecode);
        long complierEndTime = System.currentTimeMillis();
        System.err.println("Runtime: " + (complierEndTime - compilerStartTime) / 1000.0 + " seconds.");
    }
}