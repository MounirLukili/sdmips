package ast;

public class StatVarDecl extends Statement {

    private Id id;
    private Type type;

    public StatVarDecl(Position pos, Id id, Type t) {
        this.position = pos;
        this.id = id;
        this.type = t;
    }

    public Id getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
