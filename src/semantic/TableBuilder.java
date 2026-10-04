package semantic;

import ast.*;
import support.Errors;

/**
 * Construit la table des symboles : signature de chaque fonction et
 * table locale de chaque bloc (paramètres et variables déclarées).
 *
 * Erreurs détectées : fonction définie deux fois, paramètre en double,
 * variable déclarée deux fois (y compris dans un bloc englobant : le
 * masquage de variables n'est pas autorisé).
 */
public class TableBuilder extends ast.BaseVisitor<Void> {

    private final SymbolTable symbolTable = new SymbolTable();
    private final VisitedBlocks visitedBlocks = new VisitedBlocks();
    private final Errors errors = new Errors();

    public TableBuilder() {
        super(null);
    }

    public SymbolTable getTable() {
        return symbolTable;
    }

    /** Erreurs de construction de la table (y compris celles de la table elle-même). */
    public Errors getErrors() {
        Errors all = new Errors();
        all.merge(symbolTable.getErrors());
        all.merge(errors);
        return all;
    }

    @Override
    public Void visit(MethodDecl md) {
        String nom = md.getId().getName();
        if (symbolTable.methodLookup(nom) != null) {
            errors.add(md, "la fonction " + nom + " est définie plusieurs fois");
        }
        symbolTable.addMethod(nom, MethodSig.signatureOf(md));

        // Les paramètres sont rangés dans la table du bloc de la fonction.
        Block b = md.getBlock();
        symbolTable.localTable(b);
        visitedBlocks.enter(b);
        for (Formal f : md.getFormal()) {
            String param = f.getId().getName();
            if (symbolTable.variableLookup(param, visitedBlocks) != null) {
                errors.add(md, "le paramètre " + param + " apparaît plusieurs fois dans " + nom);
            }
            symbolTable.addLocalVariable(b, param, f.getType());
        }
        super.visit(b);
        visitedBlocks.exit();
        return null;
    }

    @Override
    public Void visit(Block b) {
        symbolTable.localTable(b);
        visitedBlocks.enter(b);
        super.visit(b);
        visitedBlocks.exit();
        return null;
    }

    @Override
    public Void visit(StatVarDecl vd) {
        String nom = vd.getId().getName();
        Type dejaDeclare = symbolTable.variableLookup(nom, visitedBlocks);
        if (dejaDeclare != null) {
            errors.add(vd, "variable " + nom + " déjà déclarée");
        }
        symbolTable.addLocalVariable(visitedBlocks.current(), nom, vd.getType());
        return null;
    }
}
