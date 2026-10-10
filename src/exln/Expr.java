package exln

import java.util.List;

public abstract class Expr {
    public interface Visitor<R> {
        R visitAssignExpr(Assign expr);
        R visitBinaryExpr(Binary expr);
        R visitLogicalExpr(Logical expr);
        R visitUnaryExpr(Unary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitVariableExpr(Variable expr);
        R visitCallExpr(Call expr);
        R visitGetExpr(Get expr);
        R visitSetExpr(Set expr);
        R visitThisExpr(This expr);
        R visitSuperExpr(Super expr);
        R visitArrayLiteralExpr(ArrayLiteral expr);
        R visitIndexExpr(Index expr);
        R visitIndexSetExpr(IndexSet expr);
    }
    public static class Assign extends Expr {
        static Token name;
        static Expr value;

        public Assign(Token name, Expr value) {
            this.name =name;
            this.Expr =Expr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }
    }

    public static class Binary extends Expr {
        static Expr left;
        static Token operator;
        static Expr right;

        public Binary(Expr left, Token operator, Expr right) {
            this.left =left;
            this.Token =Token;
            this.Expr =Expr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    public static class Logical extends Expr {
        static Expr left;
        static Token operator;
        static Expr right;

        public Logical(Expr left, Token operator, Expr right) {
            this.left =left;
            this.Token =Token;
            this.Expr =Expr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }

    public static class Unary extends Expr {
        static Token operator;
        static Expr right;

        public Unary(Token operator, Expr right) {
            this.operator =operator;
            this.Expr =Expr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    public static class Grouping extends Expr {
        static Expr expression;

        public Grouping(Expr expression) {
            this.expression =expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    public static class Literal extends Expr {
        static Object value;

        public Literal(Object value) {
            this.value =value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    public static class Variable extends Expr {
        static Token name;

        public Variable(Token name) {
            this.name =name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }

    public static class Call extends Expr {
        static Expr callee;
        static Token paren;
        static List<Expr> arguments;

        public Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee =callee;
            this.Token =Token;
            this.List<Expr> =List<Expr>;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }

    public static class Get extends Expr {
        static Expr object;
        static Token name;

        public Get(Expr object, Token name) {
            this.object =object;
            this.Token =Token;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGetExpr(this);
        }
    }

    public static class Set extends Expr {
        static Expr object;
        static Token name;
        static Expr value;

        public Set(Expr object, Token name, Expr value) {
            this.object =object;
            this.Token =Token;
            this.Expr =Expr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitSetExpr(this);
        }
    }

    public static class This extends Expr {
        static Token keyword;

        public This(Token keyword) {
            this.keyword =keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitThisExpr(this);
        }
    }

    public static class Super extends Expr {
        static Token keyword;
        static Token methodName;

        public Super(Token keyword, Token methodName) {
            this.keyword =keyword;
            this.Token =Token;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitSuperExpr(this);
        }
    }

    public static class ArrayLiteral extends Expr {
        static List<Expr> elements;

        public ArrayLiteral(List<Expr> elements) {
            this.elements =elements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitArrayLiteralExpr(this);
        }
    }

    public static class Index extends Expr {
        static Expr object;
        static Token bracket;
        static Expr index;

        public Index(Expr object, Token bracket, Expr index) {
            this.object =object;
            this.Token =Token;
            this.Expr =Expr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIndexExpr(this);
        }
    }

    public static class IndexSet extends Expr {
        static Expr object;
        static Token bracket;
        static Expr index;
        static Expr value;

        public IndexSet(Expr object, Token bracket, Expr index, Expr value) {
            this.object =object;
            this.Token =Token;
            this.Expr =Expr;
            this.Expr =Expr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIndexSetExpr(this);
        }
    }


    abstract <R> R accept(Visitor<R> visitor);
}
