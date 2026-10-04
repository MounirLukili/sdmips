package ast;

public class StatIf extends Statement {

    private Expression expression;
    private Block ib;
    private Block eb;

    public StatIf(Position pos, Expression exp, Block ib, Block eb) {
        this.expression = exp;
        this.ib = ib;
        this.eb = eb;
        this.position = pos;
    }

    public Expression getExpression() {
        return expression;
    }

    public Block getIfBlock() {
        return ib;
    }

    public Block getElseBlock() {
        return eb;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
