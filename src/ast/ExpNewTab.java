package ast;

public class ExpNewTab extends Expression {

    private final Expression size;

    public ExpNewTab(Position pos, Expression size) {
        this.position = pos;
        this.size = size;
    }

    public Expression getSize() {
        return size;
    }

    public String toString() {
        return "new int[" + size + "]";
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
