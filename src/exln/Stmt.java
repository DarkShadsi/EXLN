package exln;

import java.util.List;

public abstract class Stmt {
    public interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitVarStmt(Var stmt);
        R visitIfStmt(If stmt);
        R visitWhileStmt(While stmt);
        R visitDoWhileStmt(DoWhile stmt);
        R visitBreakStmt(Break stmt);
        R visitContinueStmt(Continue stmt);
        R visitSwitchStmt(Switch stmt);
        R visitFunctionStmt(Function stmt);
        R visitReturnStmt(Return stmt);
        R visitClassStmt(Class stmt);
        R visitEnumStmt(Enum stmt);
    }

    public static class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }
    public static class Expression extends Stmt {
        public final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }
    public static class Print extends Stmt {
        public final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }
    public static class Var extends Stmt {
        public final Token name;
        public final Expr initializer;

        public Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }
    public static class If extends Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;

        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }
    public static class While extends Stmt {
        public final Expr condition;
        public final Stmt body;
        public final Stmt increment;

        public While(Expr condition, Stmt body, Stmt increment) {
            this.condition = condition;
            this.body = body;
            this.increment = increment;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }
    }
    public static class DoWhile extends Stmt {
        public final Stmt body;
        public final Expr condition;

        public DoWhile(Stmt body, Expr condition) {
            this.body = body;
            this.condition = condition;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitDoWhileStmt(this);
        }
    }
    public static class Break extends Stmt {
        public final Token keyword;

        public Break(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBreakStmt(this);
        }
    }
    public static class Continue extends Stmt {
        public final Token keyword;

        public Continue(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitContinueStmt(this);
        }
    }
    public static class Switch extends Stmt {
        public final Expr subject;
        public final List<Expr> caseValues;
        public final List<List<Stmt>> caseBodies;
        public final List<Stmt> defaultBody;

        public Switch(Expr subject, List<Expr> caseValues, List<List<Stmt>> caseBodies, List<Stmt> defaultBody) {
            this.subject = subject;
            this.caseValues = caseValues;
            this.caseBodies = caseBodies;
            this.defaultBody = defaultBody;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitSwitchStmt(this);
        }
    }
    public static class Function extends Stmt {
        public final Token name;
        public final List<Token> params;
        public final List<Stmt> body;

        public Function(Token name, List<Token> params, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }
    }
    public static class Return extends Stmt {
        public final Token keyword;
        public final Expr value;

        public Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }
    }
    public static class Class extends Stmt {
        public final Token name;
        public final Expr.Variable superclass;
        public final List<Stmt.Function> methods;

        public Class(Token name, Expr.Variable superclass, List<Stmt.Function> methods) {
            this.name = name;
            this.superclass = superclass;
            this.methods = methods;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }
    }
    public static class Enum extends Stmt {
        public final Token name;
        public final List<Token> members;

        public Enum(Token name, List<Token> members) {
            this.name = name;
            this.members = members;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitEnumStmt(this);
        }
    }

    public abstract <R> R accept(Visitor<R> visitor);
}
