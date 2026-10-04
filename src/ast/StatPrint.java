package ast;

public class StatPrint extends Statement {

    private Expression expression;

    public StatPrint(Position pos, Expression exp) {
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
