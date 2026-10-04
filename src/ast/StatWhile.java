package ast;

public class StatWhile extends Statement {

    private Expression expression;
    private Block block;

    public StatWhile(Position pos, Expression exp, Block b) {
        this.expression = exp;
        this.block = b;
        this.position = pos;
    }

    public Expression getExpression() {
        return expression;
    }

    public Block getBlock() {
        return block;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
