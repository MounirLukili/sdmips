package ir.com;

public class Jump extends Command {

    private final Label gotoLabel;

    public Label getGotoLabel() {
        return gotoLabel;
    }

    public Jump(Label gotoLabel) {
        this.gotoLabel = gotoLabel;
    }

    @Override
    public String toString() {
        return "goto " + gotoLabel;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
