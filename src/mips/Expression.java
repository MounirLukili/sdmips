package mips;

import ir.Register;
import ir.expr.Binary;
import ir.expr.Byte;
import ir.expr.Int;
import ir.expr.ReadMem;
import ir.expr.ReadReg;
import ir.expr.Unary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Traduction des expressions de l'IR en MIPS, sur le modèle d'une machine à pile :
 * le code produit pour une expression laisse sa valeur au sommet de la pile.
 */
class Expression implements ir.expr.Visitor<List<String>> {

    private final Map<Register, Integer> regAddress;

    Expression(Map<Register, Integer> regAddress) {
        this.regAddress = regAddress;
    }

    private static List<String> constante(int valeur) {
        List<String> code = new LinkedList<>();
        code.add(Asm.command("li $t0, " + valeur));
        code.addAll(Asm.push("$t0"));
        return code;
    }

    @Override
    public List<String> visit(Byte exp) {
        return constante(exp.getValue());
    }

    @Override
    public List<String> visit(Int exp) {
        return constante(exp.getValue());
    }

    /** Lecture d'un registre virtuel : on charge sa case du cadre puis on l'empile. */
    @Override
    public List<String> visit(ReadReg exp) {
        List<String> code = new LinkedList<>();
        code.add(Asm.command("lw $t0, " + regAddress.get(exp.getRegister()) + "($fp)"));
        code.addAll(Asm.push("$t0"));
        return code;
    }

    /** Lecture en mémoire (case de tableau). */
    @Override
    public List<String> visit(ReadMem exp) {
        List<String> code = exp.getAddress().accept(this);
        code.addAll(Asm.pop("$t0"));
        code.add(Asm.command("lw $t0, 0($t0)"));
        code.addAll(Asm.push("$t0"));
        return code;
    }

    @Override
    public List<String> visit(Unary exp) {
        List<String> code = exp.getExp().accept(this);
        code.addAll(Asm.pop("$t0"));
        switch (exp.getOp()) {
            case MIN -> code.add(Asm.command("subu $t0, $zero, $t0"));
            case NOT -> code.add(Asm.command("xori $t0, $t0, 1")); // booléens codés 0 / 1
        }
        code.addAll(Asm.push("$t0"));
        return code;
    }

    /**
     * Opération binaire : on calcule l'opérande gauche puis l'opérande droit,
     * on les dépile et on empile le résultat.
     * Les deux opérandes sont toujours évalués (pas d'évaluation paresseuse de && et ||).
     */
    @Override
    public List<String> visit(Binary exp) {
        String op = switch (exp.getOp()) {
            case ADD -> "addu";
            case MIN -> "subu";
            case MULT -> "mul";
            case DIV -> "div";
            case AND -> "and";
            case OR -> "or";
            case LT -> "slt";
            case LEQ -> "sle";
            case GT -> "sgt";
            case GEQ -> "sge";
            case EQ -> "seq";
            case NEQ -> "sne";
        };
        List<String> code = exp.getLeft().accept(this);
        code.addAll(exp.getRight().accept(this));
        code.addAll(Asm.pop("$t1")); // opérande droit
        code.addAll(Asm.pop("$t0")); // opérande gauche
        code.add(Asm.command(op + " $t0, $t0, $t1"));
        code.addAll(Asm.push("$t0"));
        return code;
    }
}
