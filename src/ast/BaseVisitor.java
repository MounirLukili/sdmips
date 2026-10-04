package ast;

public abstract class BaseVisitor<T> implements ast.Visitor<T> {

    private final T defaultValue;

    public T getDefaultValue() {
        return defaultValue;
    }

    public BaseVisitor(T val) {
        this.defaultValue = val;
    }

    public T visit(StatVarDecl s) {
        s.getType().accept(this);
        return s.getId().accept(this);
    }

    public T visit(Formal f) {
        f.getType().accept(this);
        return f.getId().accept(this);
    }

    public T visit(Id e) {
        return defaultValue;
    }

    public T visit(StatReturn s) {
        return s.getExpression().accept(this);
    }

    public T visit(MethodDecl m) {
        T val = defaultValue;
        m.getId().accept(this);
        m.getType().accept(this);
        for (Formal f : m.getFormal()) {
            f.accept(this);
        }
        return m.getBlock().accept(this);
    }

    public T visit(Program p) {
        T res = defaultValue;
        for (MethodDecl md : p.getListMethodDecl()) {
            md.accept(this);
        }
        return res;
    }

    public T visit(StatAff a) {
        a.getId().accept(this);
        return a.getExpression().accept(this);
    }

    public T visit(StatIf s) {
        s.getExpression().accept(this);
        s.getIfBlock().accept(this);
        return s.getElseBlock().accept(this);
    }

    public T visit(StatList s) {
        T val = defaultValue;
        for (Statement st : s.getStatList()) {
            val = st.accept(this);
        }
        return val;
    }

    public T visit(StatPrint s) {
        return s.getExpression().accept(this);
    }

    public T visit(StatWhile s) {
        s.getExpression().accept(this);
        return s.getBlock().accept(this);
    }

    public T visit(Block b) {
        return b.getStatement().accept(this);
    }

    public T visit(TypePrim t) {
        return defaultValue;
    }

    public T visit(ExpUn e) {
        return e.getExp().accept(this);
    }

    public T visit(ExpBin e) {
        e.getLeftExp().accept(this);
        return e.getRightExp().accept(this);
    }

    public T visit(ExpRead e) {
        return defaultValue;
    }

    public T visit(ExpCallMethod e) {
        for (Expression ex : e.getArgs()) {
            ex.accept(this);
        }
        return e.getMethod().accept(this);
    }

    public T visit(ExpCons e) {
        return defaultValue;
    }

    public T visit(ExpId e) {
        return defaultValue;
    }

    public T visit(ExpInt e) {
        return defaultValue;
    }

    public T visit(TypeTab t) {
        return defaultValue;
    }

    public T visit(ExpNewTab e) {
        return e.getSize().accept(this);
    }

    public T visit(ExpTabElt e) {
        return e.getIndex().accept(this);
    }

    public T visit(StatAffTab s) {
        s.getIndex().accept(this);
        return s.getValue().accept(this);
    }
}
