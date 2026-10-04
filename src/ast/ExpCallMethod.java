package ast;

import java.util.List;

public class ExpCallMethod extends Expression {

    private Id method;
    private List<Expression> args;

    public ExpCallMethod(Position position, Id method, List<Expression> args) {
        this.position = position;
        this.method = method;
        this.args = args;
    }

    public Id getMethod() {
        return method;
    }

    public List<Expression> getArgs() {
        return args;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public String toString() {
        StringBuilder res = new StringBuilder(method.toString()).append('(');
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) res.append(", ");
            res.append(args.get(i));
        }
        return res.append(')').toString();
    }
}
