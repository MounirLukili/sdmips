package mips;

import ir.Register;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import support.Errors;
import support.Pair;

/**
 * Génération du code MIPS d'une fonction : prologue, corps et épilogue.
 *
 * Aucun registre MIPS n'est alloué aux registres virtuels de l'IR : chacun
 * occupe une case du cadre de pile. Disposition du cadre (N = nombre de
 * registres virtuels, taille = 4 * (N + 2)) :
 *
 * <pre>
 *            adresses hautes
 *   $fp  ->  registre virtuel 0          0($fp)
 *            registre virtuel 1         -4($fp)
 *            ...
 *            registre virtuel N-1   -4(N-1)($fp)
 *            ancien $fp                -4N($fp)
 *            ancienne adresse $ra   -4(N+1)($fp)
 *   $sp  ->  (première case libre)
 *            adresses basses
 * </pre>
 *
 * Les registres virtuels sont, dans l'ordre : les paramètres, les variables
 * et temporaires, puis le registre du résultat.
 */
class Frame {

    /** Taille d'une case du cadre (un mot MIPS). */
    static final int TAILLE_CASE = 4;

    private final Errors errors;

    Frame(Errors errors) {
        this.errors = errors;
    }

    /** Associe à chaque registre virtuel son décalage par rapport à $fp et fixe la taille du cadre. */
    private Map<Register, Integer> allouerCases(ir.Frame frame) {
        List<Register> registres = new LinkedList<>(frame.getParameters());
        registres.addAll(frame.getLocals());
        registres.add(frame.getResult());

        Map<Register, Integer> regAlloc = new HashMap<>();
        int decalage = 0;
        for (Register r : registres) {
            regAlloc.put(r, decalage);
            decalage -= TAILLE_CASE;
        }
        frame.setSize(TAILLE_CASE * (registres.size() + 2));
        return regAlloc;
    }

    /**
     * Prologue : sauvegarde de $ra et $fp, nouveau cadre ($fp = ancien $sp,
     * $sp descend de la taille du cadre), copie des arguments $a0..$a3 dans
     * les cases des paramètres.
     */
    private List<String> prologue(Map<Register, Integer> regAlloc, ir.Frame frame) {
        List<String> code = new LinkedList<>();
        code.add(Asm.label(frame.getEntryPoint().toString()));
        code.add(Asm.command("move $t0, $ra"));
        code.add(Asm.command("move $t1, $fp"));
        code.add(Asm.command("move $fp, $sp"));
        code.add(Asm.command("addiu $sp, $sp, -" + frame.getSize()));
        code.add(Asm.command("sw $t0, 4($sp)"));
        code.add(Asm.command("sw $t1, 8($sp)"));
        List<Register> params = frame.getParameters();
        for (int i = 0; i < params.size(); i++) {
            code.add(Asm.command("sw $a" + i + ", " + regAlloc.get(params.get(i)) + "($fp)"));
        }
        return code;
    }

    /**
     * Épilogue (point de sortie de la fonction) : résultat dans $v0,
     * restauration de $ra, $fp et $sp, retour à l'appelant.
     */
    private List<String> epilogue(Map<Register, Integer> regAlloc, ir.Frame frame) {
        int decalageRa = -(frame.getSize() - 4);
        int decalageFp = -(frame.getSize() - 8);

        List<String> code = new LinkedList<>();
        code.add(Asm.label(frame.getExitPoint().toString()));
        code.add(Asm.command("lw $v0, " + regAlloc.get(frame.getResult()) + "($fp)"));
        code.add(Asm.command("lw $ra, " + decalageRa + "($fp)"));
        code.add(Asm.command("lw $t2, " + decalageFp + "($fp)"));
        code.add(Asm.command("addiu $sp, $sp, " + frame.getSize()));
        code.add(Asm.command("move $fp, $t2"));
        code.add(Asm.command("jr $ra"));
        return code;
    }

    List<String> generate(Pair<ir.Frame, List<ir.com.Command>> fragment) {
        ir.Frame frame = fragment.getFst();
        if (frame.getParameters().size() > 4) {
            errors.add("fonction " + frame.getEntryPoint() + " : plus de 4 paramètres, non supporté");
            return new LinkedList<>();
        }
        Map<Register, Integer> regAlloc = allouerCases(frame);
        Command commandes = new Command(regAlloc);

        List<String> code = new LinkedList<>(prologue(regAlloc, frame));
        for (ir.com.Command cmd : fragment.getSnd()) {
            code.addAll(cmd.accept(commandes));
        }
        code.addAll(epilogue(regAlloc, frame));
        return code;
    }
}
