package ast;

public interface Visitor<T> extends VisitorExp<T> {
    public T visit(Formal f);
    public T visit(Id e);
    public T visit(MethodDecl m);
    public T visit(Program p);
    public T visit(StatAff a);
    public T visit(StatIf s);
    public T visit(StatList s);
    public T visit(StatPrint s);
    public T visit(StatWhile s);
    public T visit(TypePrim t);
    public T visit(StatVarDecl t);
    public T visit(StatReturn t);
    public T visit(Block b);
    public T visit(StatAffTab a);
    public T visit(TypeTab t);
}
