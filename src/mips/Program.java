package mips;

import ir.Frame;
import ir.com.Command;
import ir.com.Label;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import support.Errors;
import support.Pair;

/**
 * Production du fichier assembleur MIPS complet :
 * point d'entrée, code de chaque fonction, puis routines d'exécution
 * (affichage, lecture, allocation) lues dans les ressources {@code runtime/}.
 */
public final class Program {

    /** Routines d'exécution écrites à la main, ajoutées à la fin de chaque programme. */
    private static final String[] ROUTINES = {
        "runtime/print_int.asm",
        "runtime/print_bool.asm",
        "runtime/read_int.asm",
        "runtime/alloc.asm",
    };

    private Program() {}

    /** Point d'entrée : appel de la fonction main du programme, puis fin d'exécution. */
    private static List<String> pointEntree(Label mainLabel) {
        List<String> code = new LinkedList<>();
        code.add(Asm.directive("text"));
        code.add(Asm.directive("globl main"));
        code.add(Asm.label("main"));
        code.add(Asm.command("jal " + mainLabel));
        code.addAll(Asm.exit());
        return code;
    }

    private static void copierRoutines(PrintWriter out, Errors errors) {
        ClassLoader loader = Program.class.getClassLoader();
        for (String routine : ROUTINES) {
            InputStream stream = loader.getResourceAsStream(routine);
            if (stream == null) {
                errors.add("routine d'exécution introuvable : " + routine);
                continue;
            }
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            ) {
                out.println();
                String ligne;
                while ((ligne = in.readLine()) != null) {
                    out.println(ligne);
                }
            } catch (IOException e) {
                errors.add("lecture de " + routine + " impossible : " + e.getMessage());
            }
        }
    }

    /**
     * Écrit le programme MIPS dans le fichier {@code path}.
     *
     * @return les erreurs rencontrées (vide si tout s'est bien passé)
     */
    public static Errors generate(Path path, Label mainLabel, List<Pair<Frame, List<Command>>> fragments) {
        Errors errors = new Errors();
        mips.Frame generateur = new mips.Frame(errors);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            out.println("# Code MIPS produit par sdmips");
            for (String ligne : pointEntree(mainLabel)) {
                out.println(ligne);
            }
            for (Pair<Frame, List<Command>> fragment : fragments) {
                out.println();
                for (String ligne : generateur.generate(fragment)) {
                    out.println(ligne);
                }
            }
            copierRoutines(out, errors);
        } catch (IOException e) {
            errors.add("impossible d'écrire " + path + " : " + e.getMessage());
        }
        return errors;
    }
}
