package ast;

public interface VisitorExp<T> {
    T visit(ExpBin e);
    T visit(ExpCallMethod e);
    T visit(ExpCons e);
    T visit(ExpId e);
    T visit(ExpInt e);
    T visit(ExpUn e);
    T visit(ExpRead e);
    T visit(ExpNewTab e);
    T visit(ExpTabElt e);
}
