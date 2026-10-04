package ir.com;

import ir.expr.Expression;

public class WriteMem extends Command {

    private final Expression address;
    private final Expression value;

    public WriteMem(Expression address, Expression value) {
        this.address = address;
        this.value = value;
    }

    public Expression getAddress() {
        return address;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "mem[" + address + "] := " + value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
