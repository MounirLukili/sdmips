package ir.com;

public abstract class Command {

    public abstract <T> T accept(Visitor<T> visitor);
}
