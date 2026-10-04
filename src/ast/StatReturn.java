package ast;

public class StatReturn extends Statement {

    private Expression expression;

    public StatReturn(Position pos, Expression exp) {
        this.expression = exp;
        this.position = pos;
    }

    public Expression getExpression() {
        return expression;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
