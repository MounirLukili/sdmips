package ast;

import java.util.List;

public class StatList extends Statement {

    private final List<Statement> statList;

    public StatList(Position position, List<Statement> statList) {
        this.position = position;
        this.statList = statList;
    }

    public List<Statement> getStatList() {
        return statList;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
