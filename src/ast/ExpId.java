package ast;

public class ExpId extends Expression {

    private final String value;

    public ExpId(Position position, String value) {
        this.value = value;
        this.position = position;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return value;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
