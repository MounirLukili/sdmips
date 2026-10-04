package ast;

public enum Constant {
    TRUE {
        public String toString() {
            return "true";
        }
    },
    FALSE {
        public String toString() {
            return "false";
        }
    },
}
