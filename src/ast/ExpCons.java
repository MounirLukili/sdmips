package ast;

public class ExpCons extends Expression {

    private final Constant constant;

    public ExpCons(Position position, Constant constant) {
        this.position = position;
        this.constant = constant;
    }

    public Constant getConstant() {
        return constant;
    }

    public String toString() {
        return constant.toString();
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
