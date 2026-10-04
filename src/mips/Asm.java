package mips;

import java.util.LinkedList;
import java.util.List;

/**
 * Petites fonctions de mise en forme d'instructions MIPS.
 *
 * Convention de pile du compilateur : $sp pointe sur la première case libre,
 * le sommet de la pile est donc en 4($sp).
 */
final class Asm {

    private Asm() {}

    static String command(String command) {
        return "\t" + command;
    }

    static String label(String label) {
        return label + ":";
    }

    static String directive(String directive) {
        return command("." + directive);
    }

    static List<String> exit() {
        List<String> code = new LinkedList<>();
        code.add(command("li $v0, 10"));
        code.add(command("syscall"));
        return code;
    }

    /** Empile le contenu d'un registre. */
    static List<String> push(String register) {
        List<String> code = new LinkedList<>();
        code.add(command("addiu $sp, $sp, -4"));
        code.add(command("sw " + register + ", 4($sp)"));
        return code;
    }

    /** Dépile le sommet de la pile dans un registre. */
    static List<String> pop(String register) {
        List<String> code = new LinkedList<>();
        code.add(command("lw " + register + ", 4($sp)"));
        code.add(command("addiu $sp, $sp, 4"));
        return code;
    }
}
