package mips;

import ir.Register;
import ir.com.CJump;
import ir.com.FunCall;
import ir.com.Jump;
import ir.com.Label;
import ir.com.WriteMem;
import ir.com.WriteReg;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Traduction des commandes de l'IR en instructions MIPS.
 *
 * Chaque registre virtuel de l'IR occupe une case du cadre de pile, à un
 * décalage fixe par rapport à $fp (table {@code regAlloc}).
 */
class Command implements ir.com.Visitor<List<String>> {

    private final Expression exprVisitor;
    private final Map<Register, Integer> regAlloc;

    Command(Map<Register, Integer> regAlloc) {
        this.regAlloc = regAlloc;
        this.exprVisitor = new Expression(regAlloc);
    }

    @Override
    public List<String> visit(Label com) {
        List<String> code = new LinkedList<>();
        code.add(Asm.label(com.toString()));
        return code;
    }

    /** reg := exp : on calcule exp (résultat en sommet de pile) puis on l'écrit dans la case de reg. */
    @Override
    public List<String> visit(WriteReg com) {
        List<String> code = new LinkedList<>(com.getExp().accept(exprVisitor));
        code.addAll(Asm.pop("$t0"));
        code.add(Asm.command("sw $t0, " + regAlloc.get(com.getReg()) + "($fp)"));
        return code;
    }

    /** mem[adresse] := valeur. */
    @Override
    public List<String> visit(WriteMem com) {
        List<String> code = new LinkedList<>(com.getValue().accept(exprVisitor));
        code.addAll(com.getAddress().accept(exprVisitor));
        code.addAll(Asm.pop("$t1")); // adresse
        code.addAll(Asm.pop("$t0")); // valeur
        code.add(Asm.command("sw $t0, 0($t1)"));
        return code;
    }

    /** Saut conditionnel : vers le label "vrai" si la condition est non nulle, sinon vers "faux". */
    @Override
    public List<String> visit(CJump com) {
        List<String> code = new LinkedList<>(com.getCondition().accept(exprVisitor));
        code.addAll(Asm.pop("$t0"));
        code.add(Asm.command("bnez $t0, " + com.getTrueLabel()));
        code.add(Asm.command("j " + com.getFalseLabel()));
        return code;
    }

    @Override
    public List<String> visit(Jump com) {
        List<String> code = new LinkedList<>();
        code.add(Asm.command("j " + com.getGotoLabel()));
        return code;
    }

    /**
     * Appel de fonction : les arguments (4 au plus) sont calculés puis placés
     * dans $a0..$a3, on saute à la fonction avec jal, et le résultat ($v0)
     * est rangé dans le registre destination.
     */
    @Override
    public List<String> visit(FunCall com) {
        List<ir.expr.Expression> args = com.getArguments();
        List<String> code = new LinkedList<>();
        for (ir.expr.Expression arg : args) {
            code.addAll(arg.accept(exprVisitor));
        }
        // Le dernier argument calculé est au sommet de la pile.
        for (int i = args.size() - 1; i >= 0; i--) {
            code.addAll(Asm.pop("$a" + i));
        }
        code.add(Asm.command("jal " + com.getFrame().getEntryPoint()));
        code.add(Asm.command("sw $v0, " + regAlloc.get(com.getRegister()) + "($fp)"));
        return code;
    }
}
