package exln;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {
    static boolean hadError;
    Lexer lexer;

    public static void main(String[] args) {
        Main main = new Main();
        Scanner scanner = new Scanner(System.in);

        if (args.length == 0) {
            while(true) {
                System.out.print("EXLN:: ");
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
        List<Token> tokens = lexer.scanTokens();
        if (hadError) {return;}
        for (Token token : tokens) {
            System.out.println(token.toString());
        }
    }

    static void error(int line, String message) {
        System.out.printf("[line %d] Error: %s\n", line, message);
        hadError = true;
    }
}