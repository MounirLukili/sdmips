package ast;

public enum UnOp {
    NOT {
        public String toString() {
            return "!";
        }
    },
    MIN {
        public String toString() {
            return "-";
        }
    },
}
