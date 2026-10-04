package ir.com;

import ir.expr.Expression;

public class CJump extends Command {

    private final Expression condition;
    private final Label trueLabel;
    private final Label falseLabel;

    public Expression getCondition() {
        return condition;
    }

    public Label getTrueLabel() {
        return trueLabel;
    }

    public Label getFalseLabel() {
        return falseLabel;
    }

    public CJump(Expression condition, Label trueLabel, Label falseLabel) {
        this.condition = condition;
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
    }

    @Override
    public String toString() {
        return "CJump (" + condition + ", " + trueLabel + ", " + falseLabel + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
