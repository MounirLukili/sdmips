package ast;

public class StatAffTab extends Statement {

    private final Id tab;
    private final Expression index;
    private final Expression value;

    public StatAffTab(Position pos, Id tab, Expression index, Expression value) {
        this.position = pos;
        this.tab = tab;
        this.index = index;
        this.value = value;
    }

    public Id getTab() {
        return tab;
    }

    public Expression getIndex() {
        return index;
    }

    public Expression getValue() {
        return value;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
