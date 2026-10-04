package ast;

public class ExpRead extends Expression {

    public ExpRead(Position position) {
        this.position = position;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public String toString() {
        return "read()";
    }
}
