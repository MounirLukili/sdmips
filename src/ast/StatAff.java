package ast;

public class StatAff extends Statement {

    private Id id;
    private Expression expression;

    public StatAff(Position pos, Id id, Expression exp) {
        this.id = id;
        this.expression = exp;
        this.position = pos;
    }

    public Id getId() {
        return id;
    }

    public Expression getExpression() {
        return expression;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
