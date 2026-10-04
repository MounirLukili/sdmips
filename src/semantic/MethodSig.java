package semantic;

import ast.Formal;
import ast.MethodDecl;
import ast.Type;
import java.util.List;

public class MethodSig {

    private List<Formal> params;
    private Type returnType;

    public MethodSig(List<Formal> p, Type t) {
        this.params = p;
        this.returnType = t;
    }

    public List<Formal> getParams() {
        return params;
    }

    public Type getReturnType() {
        return returnType;
    }

    public static MethodSig signatureOf(MethodDecl md) {
        return new MethodSig(md.getFormal(), md.getType());
    }
}
