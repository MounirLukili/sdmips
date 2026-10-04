package printers;

import ir.Frame;
import ir.com.Command;
import ir.com.Label;
import ir.translation.Translate;
import java.io.PrintStream;
import java.util.List;
import support.Pair;

/** Affichage lisible du code intermédiaire (option -v). */
public class IrPrinter {

    private final PrintStream out;

    public IrPrinter(PrintStream out) {
        this.out = out;
    }

    public void print(Translate.Resultat ir) {
        out.println("main = " + ir.mainLabel);
        for (Pair<Frame, List<Command>> fragment : ir.fragments) {
            out.println();
            out.println(fragment.getFst());
            for (Command c : fragment.getSnd()) {
                out.println(c instanceof Label ? c + ":" : "    " + c);
            }
        }
    }
}
