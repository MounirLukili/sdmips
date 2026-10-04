package ir;

public class Register {

    private final int id;
    private final Type type;
    private static int lastRegister;

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "reg" + id;
    }

    public Register(Type type) {
        this.type = type;
        this.id = lastRegister;
        lastRegister += 1;
    }
}
