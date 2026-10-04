package ir.expr;

import ir.Register;
import ir.Type;

public class ReadReg extends Expression {

    private final Register register;

    public ReadReg(Register register) {
        this.register = register;
    }

    @Override
    public Type getType() {
        return register.getType();
    }

    public Register getRegister() {
        return register;
    }

    @Override
    public String toString() {
        return register.toString();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
