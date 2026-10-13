package exln;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {
    static boolean hadError;
    Lexer lexer;
    Parser parser;
    ASTPrinter printer;

    public static void main(String[] args) {
        Main main = new Main();
        Scanner scanner = new Scanner(System.in);

        if (args.length == 0) {
            while(true) {
                System.out.print("EXLN:> ");
                String line = scanner.nextLine();
                if (line.equals("exit") || line.equals("quit") || line.equals("q")) break;
                main.run(line);
                hadError = false;
            }
        } else if (args.length == 1) {
            Path path = Path.of(args[0]);
            try {
                String source =  Files.readString(path);
                main.run(source);
            } catch (IOException e) {
                System.out.println("Error reading the file.");
                hadError = true;
            }
            if (hadError) {System.exit(65);}
        } else {
            System.out.println("USAGE: use the filename as argument or no arguments to enter REPL.");
            return;
        }
    }

    void run(String source) {
        lexer = new Lexer(source);
        parser = new Parser();
        printer = new ASTPrinter();
        List<Token> tokens = lexer.scanTokens();
        List<Stmt> statements = parser.parse(tokens);
        if (hadError) { return; }
        for (Stmt stmt : statements) {
            System.out.println("EXLN:: " + printer.print(stmt));
        }

    }

    static void error(int line, String message) {
        System.out.printf("EXLN:: [line %d] Error: %s\n", line, message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            System.out.printf("EXLN:: [line %d] Error at end: %s\n", token.line, message);
        } else {
            System.out.printf("EXLN:: [line %d] Error at '%s': %s\n", token.line, token.lexeme, message);
        }
        hadError = true;
    }
}