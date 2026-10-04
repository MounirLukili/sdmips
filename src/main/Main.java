package main;

import ast.AstBuild;
import ast.Program;
import ir.translation.Translate;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import parser.SdmLexer;
import parser.SdmParser;
import printers.AstPrinter;
import printers.IrPrinter;
import semantic.SymbolTable;
import semantic.TableBuilder;
import semantic.TypeChecker;
import support.Errors;

/**
 * Compilateur SDM vers MIPS.
 *
 * <pre>
 * Étapes : analyse lexicale et syntaxique (ANTLR) -> AST -> table des symboles
 *          -> vérification des types -> code intermédiaire -> assembleur MIPS
 * </pre>
 */
public final class Main {

    private static final String USAGE = String.join(
        "\n",
        "Usage : sdmips [options] [fichier.sdm]",
        "",
        "Compile un programme SDM en assembleur MIPS (exécutable avec SPIM ou MARS).",
        "Sans fichier, le programme est lu sur l'entrée standard.",
        "",
        "Options :",
        "  -o FICHIER   fichier assembleur produit",
        "               (défaut : fichier.asm à côté du source, ou out.asm)",
        "  -v           affiche l'AST et le code intermédiaire",
        "  -h           affiche cette aide"
    );

    private static final PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private static final PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);

    private Main() {}

    /** Message d'erreur sur la sortie d'erreur et code de sortie 1. */
    private static void echec(String etape, Errors errors) {
        err.println("Erreur(s) " + etape + " :");
        for (String e : errors.get()) {
            err.println("  " + e);
        }
        System.exit(1);
    }

    /** Les erreurs de syntaxe d'ANTLR sont recueillies au lieu d'être affichées en vrac. */
    private static final class SyntaxErrors extends BaseErrorListener {

        final Errors errors = new Errors();

        @Override
        public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object symbole,
            int ligne,
            int colonne,
            String message,
            RecognitionException e
        ) {
            errors.add("[ligne=" + ligne + ", colonne=" + colonne + "] : " + message);
        }
    }

    private static Program analyser(CharStream source) {
        SyntaxErrors syntaxErrors = new SyntaxErrors();

        SdmLexer lexer = new SdmLexer(source);
        lexer.removeErrorListeners();
        lexer.addErrorListener(syntaxErrors);

        SdmParser parser = new SdmParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxErrors);
        SdmParser.ProgramContext tree = parser.program();

        if (syntaxErrors.errors.hasErrors()) {
            echec("de syntaxe", syntaxErrors.errors);
        }
        AstBuild builder = new AstBuild();
        Program ast = (Program) tree.accept(builder);
        if (builder.getErrors().hasErrors()) {
            echec("de syntaxe", builder.getErrors());
        }
        return ast;
    }

    private static Path fichierSortie(String source, String sortie) {
        if (sortie != null) {
            return Paths.get(sortie);
        }
        if (source == null) {
            return Paths.get("out.asm");
        }
        String nom = source.endsWith(".sdm") ? source.substring(0, source.length() - 4) : source;
        return Paths.get(nom + ".asm");
    }

    public static void main(String[] args) {
        String source = null;
        String sortie = null;
        boolean bavard = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h", "--help" -> {
                    out.println(USAGE);
                    return;
                }
                case "-v" -> bavard = true;
                case "-o" -> {
                    if (i + 1 >= args.length) {
                        err.println("Erreur : -o attend un nom de fichier\n\n" + USAGE);
                        System.exit(2);
                    }
                    sortie = args[++i];
                }
                default -> {
                    if (args[i].startsWith("-") || source != null) {
                        err.println("Erreur : argument inattendu « " + args[i] + " »\n\n" + USAGE);
                        System.exit(2);
                    }
                    source = args[i];
                }
            }
        }

        // 1. Analyse lexicale et syntaxique, construction de l'AST
        CharStream input;
        try {
            input =
                source == null
                    ? CharStreams.fromStream(System.in, StandardCharsets.UTF_8)
                    : CharStreams.fromPath(Paths.get(source), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            err.println("Erreur : fichier introuvable : " + source);
            System.exit(1);
            return;
        } catch (IOException e) {
            err.println("Erreur : lecture impossible : " + e.getMessage());
            System.exit(1);
            return;
        }
        Program ast = analyser(input);
        if (bavard) {
            out.println("---- AST ----");
            ast.accept(new AstPrinter(out));
            out.println("\n");
        }

        // 2. Table des symboles
        TableBuilder tableBuilder = new TableBuilder();
        ast.accept(tableBuilder);
        if (tableBuilder.getErrors().hasErrors()) {
            echec("dans les déclarations", tableBuilder.getErrors());
        }
        SymbolTable table = tableBuilder.getTable();

        // 3. Vérification des types
        TypeChecker typeChecker = new TypeChecker(table);
        ast.accept(typeChecker);
        if (typeChecker.getErrors().hasErrors()) {
            echec("de typage", typeChecker.getErrors());
        }

        // 4. Code intermédiaire
        Translate.Resultat ir = Translate.run(table, ast);
        if (ir.errors.hasErrors()) {
            echec("de traduction en code intermédiaire", ir.errors);
        }
        if (bavard) {
            out.println("---- Code intermédiaire ----");
            new IrPrinter(out).print(ir);
            out.println();
        }

        // 5. Assembleur MIPS
        Path destination = fichierSortie(source, sortie);
        Errors mipsErrors = mips.Program.generate(destination, ir.mainLabel, ir.fragments);
        if (mipsErrors.hasErrors()) {
            echec("de génération MIPS", mipsErrors);
        }
        out.println("Assembleur MIPS écrit dans " + destination);
    }
}
