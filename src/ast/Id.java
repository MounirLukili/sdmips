package ast;

public class Id extends Node {

    private final String id;

    public Id(Position pos, String id) {
        this.id = id;
        this.position = pos;
    }

    public String getName() {
        return id;
    }

    public String toString() {
        return id;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
