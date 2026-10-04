package ir.expr;

import ast.BinOp;
import ir.Type;

public class Binary extends Expression {

    private final Expression left;
    private final Expression right;
    private final ast.BinOp op;

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public ast.BinOp getOp() {
        return op;
    }

    public Binary(Expression left, Expression right, ast.BinOp op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public Type getType() {
        // Opérations arithmétiques : entier ; comparaisons et opérations logiques : booléen.
        return switch (op) {
            case ADD, MIN, MULT, DIV -> Type.INT;
            case LT, LEQ, GT, GEQ, EQ, NEQ, AND, OR -> Type.BYTE;
        };
    }

    @Override
    public String toString() {
        return "(" + left + " " + op + " " + right + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
