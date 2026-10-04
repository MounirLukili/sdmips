package ir.expr;

import ir.Type;

public abstract class Expression {

    public abstract <T> T accept(Visitor<T> visitor);

    public abstract Type getType();
}
