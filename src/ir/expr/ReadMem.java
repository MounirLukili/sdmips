package ir.expr;

import ir.Type;

public class ReadMem extends Expression {

    private final Expression address;

    public ReadMem(Expression address) {
        this.address = address;
    }

    public Expression getAddress() {
        return address;
    }

    @Override
    public Type getType() {
        return Type.INT;
    }

    @Override
    public String toString() {
        return "mem[" + address + "]";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
