package support;

public class Pair<U, V> {

    private final U fst;
    private final V snd;

    public U getFst() {
        return fst;
    }

    public V getSnd() {
        return snd;
    }

    public Pair(U fst, V snd) {
        this.fst = fst;
        this.snd = snd;
    }

    @Override
    public boolean equals(Object o) {
        return (
            o instanceof Pair<?, ?> p &&
            java.util.Objects.equals(fst, p.fst) &&
            java.util.Objects.equals(snd, p.snd)
        );
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(fst, snd);
    }
}
