package ir.expr;

import ast.UnOp;
import ir.Type;

public class Unary extends Expression {

    private final Expression exp;
    private final ast.UnOp op;

    @Override
    public Type getType() {
        return switch (op) {
            case NOT -> Type.BYTE;
            case MIN -> Type.INT;
        };
    }

    public Expression getExp() {
        return exp;
    }

    public ast.UnOp getOp() {
        return op;
    }

    public Unary(Expression exp, ast.UnOp op) {
        this.exp = exp;
        this.op = op;
    }

    @Override
    public String toString() {
        return op + "(" + exp + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
