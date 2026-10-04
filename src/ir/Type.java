package ir;

public enum Type {
    BYTE {
        @Override
        public String toString() {
            return "byte";
        }
    },
    INT {
        @Override
        public String toString() {
            return "int";
        }
    },
}
