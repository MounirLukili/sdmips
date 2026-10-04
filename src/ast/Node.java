package ast;

public abstract class Node {

    protected Position position;

    public Position getPosition() {
        return position;
    }

    public abstract <T> T accept(Visitor<T> visitor);
}
