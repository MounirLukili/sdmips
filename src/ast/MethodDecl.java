package ast;

import java.util.List;

public class MethodDecl extends Node {

    private List<Formal> formalList;
    private Type type;
    private Id id;
    private Block block;

    public MethodDecl(Position pos, Type t, Id i, List<Formal> fl, Block b) {
        this.position = pos;
        this.type = t;
        this.id = i;
        this.formalList = fl;
        this.block = b;
    }

    public List<Formal> getFormal() {
        return formalList;
    }

    public Id getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Block getBlock() {
        return block;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
