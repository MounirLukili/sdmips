package ast;

public abstract class Expression extends Node {

    public abstract String toString();

    public abstract <T> T accept(Visitor<T> visitor);
}
