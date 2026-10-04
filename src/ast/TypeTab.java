package ast;

public class TypeTab extends Type {

    public TypeTab(Position pos) {
        this.position = pos;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }


    public String toString() {
        return "int[]";
    }
}
