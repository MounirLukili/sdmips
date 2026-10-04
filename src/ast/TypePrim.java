package ast;

public class TypePrim extends Type {

    public enum Prim {
        INT {
            public String toString() {
                return "int";
            }
        },
        BOOL {
            public String toString() {
                return "boolean";
            }
        },
        STAT {
            public String toString() {
                return "statement";
            }
        },
        IND {
            public String toString() {
                return "indéfini";
            }
        },
    }

    private final Prim prim;

    public Prim getPrim() {
        return prim;
    }

    public TypePrim(Position pos, Prim p) {
        this.prim = p;
        this.position = pos;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }


    public String toString() {
        return prim.toString();
    }
}
