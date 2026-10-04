package ir.com;

public interface Visitor<T> {
    T visit(Label com);
    T visit(WriteReg com);
    T visit(CJump com);
    T visit(Jump com);
    T visit(FunCall com);

    T visit(WriteMem com);
}
