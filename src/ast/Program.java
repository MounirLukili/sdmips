package ast;

import java.util.List;

public class Program extends Node {

    private List<MethodDecl> listMethodDecl;

    public Program(Position pos, List<MethodDecl> lm) {
        this.position = pos;
        this.listMethodDecl = lm;
    }

    public List<MethodDecl> getListMethodDecl() {
        return listMethodDecl;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
