package ast;

public class ExpTabElt extends Expression {

    private final Id tab;
    private final Expression index;

    public ExpTabElt(Position pos, Id tab, Expression index) {
        this.position = pos;
        this.tab = tab;
        this.index = index;
    }

    public Id getTab() {
        return tab;
    }

    public Expression getIndex() {
        return index;
    }

    public String toString() {
        return tab + "[" + index + "]";
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
