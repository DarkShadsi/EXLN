package tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];

        // LIST ALL EXPRESSION TYPES
        List<String> types = Arrays.asList(
                "Assign: Token name, Expr value",
                "Binary: Expr left, Token operator, Expr right",
                "Logical: Expr left, Token operator, Expr right",
                "Unary: Token operator, Expr right",
                "Grouping: Expr expression",
                "Literal: Object value",
                "Variable: Token name",
                "Call: Expr callee, Token paren, List<Expr> arguments",
                "Get: Expr object, Token name",
                "Set: Expr object, Token name, Expr value",
                "This: Token keyword",
                "Super: Token keyword, Token methodName",
                "ArrayLiteral: List<Expr> elements",
                "Index: Expr object, Token bracket, Expr index",
                "IndexSet: Expr object, Token bracket, Expr index, Expr value"
        );

        // CREATE THE AST
        defineAst(outputDir, "Expr", types);
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        // IMPORTS AND PACKAGE
        writer.println("package exln");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("public abstract class " + baseName + " {");

        // CREATE THE VISITOR INTERFACE
        defineVisitor(writer, baseName, types);

        // CREATE THE SUBCLASSES FOR EACH TYPES
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // ABSTRACT ACCEPT METHOD
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();

    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    public interface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("    public static class " + className + " extends " + baseName + " {");

        String fields[];
        if (fieldList.isEmpty()) {
            fields = new String[0];
        } else {
            fields = fieldList.split(",");
        }

        // DECLARE STATIC FIELDS
        for (String field : fields) {
            writer.println("        static " + field.trim() + ";");
        }

        // CREATE THE CONSTRUCTOR
        writer.println();
        writer.println("        public " + className + "(" + fieldList + ") {");
        for (String field : fields) {
            writer.println("            this." + field.split(" ")[1] + " =" + field.split(" ")[1] + ";");
        }
        writer.println("        }");

        writer.println();
        writer.println("        @Override");
        writer.println("        public <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
        writer.println();
    }
}