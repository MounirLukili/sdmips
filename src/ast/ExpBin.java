package ast;

public class ExpBin extends Expression {

    private final Expression leftExp;
    private final Expression rightExp;
    private final BinOp binOp;

    public ExpBin(Position position, Expression leftExp, BinOp binOp, Expression rightExp) {
        this.position = position;
        this.leftExp = leftExp;
        this.binOp = binOp;
        this.rightExp = rightExp;
    }

    public Expression getLeftExp() {
        return leftExp;
    }

    public Expression getRightExp() {
        return rightExp;
    }

    public BinOp getOp() {
        return binOp;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public String toString() {
        return "(" + leftExp + " " + binOp + " " + rightExp + ")";
    }
}
