package ast;

public abstract class Statement extends Node {

    public abstract <T> T accept(Visitor<T> visitor);
}
