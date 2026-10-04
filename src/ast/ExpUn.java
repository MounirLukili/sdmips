package ast;

public class ExpUn extends Expression {

    private Expression expression;
    private UnOp unOp;

    public Expression getExp() {
        return expression;
    }

    public UnOp getOp() {
        return unOp;
    }

    public ExpUn(Position position, Expression expression, UnOp unOp) {
        this.position = position;
        this.expression = expression;
        this.unOp = unOp;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public String toString() {
        return unOp.toString() + expression.toString();
    }
}
