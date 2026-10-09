package exln;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {
    static boolean hadError;

    public static void main(String[] args) {
        Main main = new Main();
        Scanner scanner = new Scanner(System.in);

        if (args.length == 0) {
            while(!hadError) {
                String line = scanner.nextLine();
                main.run(line);
            }
        } else if (args.length == 1) {
            Path path = Path.of(args[0]);
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    main.run(line);
                    if (hadError) break;
                }
            } catch (IOException e) {
                hadError = true;
            }
        } else {
            System.out.println("USAGE: use the filename as argument or no arguments to enter REPL.");
            return;
        }
    }

    void run(String source) {
        error(1, "test");
    }

    static void error(int line, String message) {
        System.out.printf("[line %d] Error: %s\n", line, message);
        hadError = true;
    }
}