package ast;

/** Type du langage : int, boolean ou int[]. Deux types sont égaux s'ils ont le même nom. */
public abstract class Type extends Node {

    @Override
    public boolean equals(Object o) {
        return o instanceof Type t && t.toString().equals(this.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public abstract String toString();
}
