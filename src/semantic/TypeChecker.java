package semantic;

import ast.*;
import java.util.List;
import support.Errors;

/**
 * Vérification des types de l'AST.
 *
 * Contrôles effectués :
 * <ul>
 *   <li>variables déclarées et types compatibles dans les affectations ;</li>
 *   <li>opérandes des opérateurs (arithmétique sur int, logique sur boolean,
 *       comparaisons d'ordre sur int, égalité entre valeurs de même type) ;</li>
 *   <li>conditions des {@code if} et {@code while} de type boolean ;</li>
 *   <li>appels de fonctions : fonction existante, nombre et types des arguments ;</li>
 *   <li>tableaux : indice entier, valeurs entières, variable de type int[] ;</li>
 *   <li>valeur de retour présente sur tous les chemins et du bon type ;</li>
 *   <li>instructions inatteignables (placées après un return).</li>
 * </ul>
 */
public class TypeChecker extends ast.BaseVisitor<Type> {

    /** Nombre maximal de paramètres (passés dans les registres $a0 à $a3). */
    public static final int MAX_PARAMETRES = 4;

    private static final Type STAT = new TypePrim(null, TypePrim.Prim.STAT);
    private static final Type ERREUR = new TypePrim(null, TypePrim.Prim.IND);

    private final SymbolTable table;
    private final VisitedBlocks visitedBlocks = new VisitedBlocks();
    private final Errors errors = new Errors();

    /** Vrai pendant la vérification du corps d'une fonction. */
    private boolean dansFonction;

    /**
     * Type renvoyé par les instructions déjà vues dans la fonction courante :
     * non nul seulement si un return est atteint sur tous les chemins.
     */
    private Type typeRetour;

    public TypeChecker(SymbolTable table) {
        super(STAT);
        this.table = table;
    }

    public VisitedBlocks getVisitedBlocks() {
        return visitedBlocks;
    }

    public Errors getErrors() {
        return errors;
    }

    /* ---------------------------------------------------------------- */
    /*  Outils                                                          */
    /* ---------------------------------------------------------------- */

    private static boolean estInt(Type t) {
        return t instanceof TypePrim p && p.getPrim() == TypePrim.Prim.INT;
    }

    private static boolean estBool(Type t) {
        return t instanceof TypePrim p && p.getPrim() == TypePrim.Prim.BOOL;
    }

    private static boolean estErreur(Type t) {
        return t == null || (t instanceof TypePrim p && p.getPrim() == TypePrim.Prim.IND);
    }

    private static Type intType(Node n) {
        return new TypePrim(n.getPosition(), TypePrim.Prim.INT);
    }

    private static Type boolType(Node n) {
        return new TypePrim(n.getPosition(), TypePrim.Prim.BOOL);
    }

    /** Signale une instruction placée après un return. */
    private void verifierAtteignable(Node n) {
        if (dansFonction && typeRetour != null) {
            errors.add(n, "instruction inatteignable (après un return)");
        }
    }

    private Type typeVariable(Node n, String nom) {
        Type t = table.variableLookup(nom, visitedBlocks);
        if (t == null) {
            errors.add(n, "variable " + nom + " non déclarée");
            return ERREUR;
        }
        return t;
    }

    /* ---------------------------------------------------------------- */
    /*  Fonctions et blocs                                              */
    /* ---------------------------------------------------------------- */

    @Override
    public Type visit(MethodDecl md) {
        String nom = md.getId().getName();
        if (md.getFormal().size() > MAX_PARAMETRES) {
            errors.add(
                md,
                "la fonction " +
                    nom +
                    " a " +
                    md.getFormal().size() +
                    " paramètres (" +
                    MAX_PARAMETRES +
                    " au maximum)"
            );
        }

        dansFonction = true;
        typeRetour = null;
        md.getBlock().accept(this);

        if (typeRetour == null) {
            errors.add(md, "la fonction " + nom + " ne renvoie pas de valeur sur tous les chemins");
        } else if (!estErreur(typeRetour) && !typeRetour.equals(md.getType())) {
            errors.add(
                md,
                "la fonction " + nom + " doit renvoyer " + md.getType() + " (trouvé : " + typeRetour + ")"
            );
        }
        dansFonction = false;
        typeRetour = null;
        return STAT;
    }

    @Override
    public Type visit(Block b) {
        verifierAtteignable(b);
        visitedBlocks.enter(b);
        super.visit(b);
        visitedBlocks.exit();
        return STAT;
    }

    @Override
    public Type visit(StatList sl) {
        verifierAtteignable(sl);
        return super.visit(sl);
    }

    /* ---------------------------------------------------------------- */
    /*  Instructions                                                    */
    /* ---------------------------------------------------------------- */

    @Override
    public Type visit(StatVarDecl s) {
        verifierAtteignable(s);
        return STAT;
    }

    @Override
    public Type visit(StatAff sa) {
        verifierAtteignable(sa);
        Type tVar = typeVariable(sa, sa.getId().getName());
        Type tExp = sa.getExpression().accept(this);
        if (!estErreur(tVar) && !estErreur(tExp) && !tVar.equals(tExp)) {
            errors.add(
                sa,
                "impossible d'affecter " +
                    sa.getExpression() +
                    " (de type " +
                    tExp +
                    ") à la variable " +
                    sa.getId().getName() +
                    " (de type " +
                    tVar +
                    ")"
            );
        }
        return STAT;
    }

    @Override
    public Type visit(StatAffTab s) {
        verifierAtteignable(s);
        String nom = s.getTab().getName();
        Type t = typeVariable(s, nom);
        if (!estErreur(t) && !(t instanceof TypeTab)) {
            errors.add(s, nom + " n'est pas un tableau (type " + t + ")");
        }
        Type ti = s.getIndex().accept(this);
        if (!estErreur(ti) && !estInt(ti)) {
            errors.add(s, "l'indice d'un tableau doit être de type int (trouvé : " + ti + ")");
        }
        Type tv = s.getValue().accept(this);
        if (!estErreur(tv) && !estInt(tv)) {
            errors.add(s, "un tableau ne contient que des int (trouvé : " + tv + ")");
        }
        return STAT;
    }

    @Override
    public Type visit(StatPrint sp) {
        verifierAtteignable(sp);
        Type t = sp.getExpression().accept(this);
        if (!estErreur(t) && !estInt(t) && !estBool(t)) {
            errors.add(sp, "print n'accepte que des int ou des boolean (trouvé : " + t + ")");
        }
        return STAT;
    }

    @Override
    public Type visit(StatReturn sr) {
        verifierAtteignable(sr);
        Type t = sr.getExpression().accept(this);
        typeRetour = estErreur(t) ? ERREUR : t;
        return STAT;
    }

    @Override
    public Type visit(StatIf s) {
        verifierAtteignable(s);
        Type tc = s.getExpression().accept(this);
        if (!estErreur(tc) && !estBool(tc)) {
            errors.add(s, "la condition du if doit être de type boolean (trouvé : " + tc + ")");
        }

        // Chaque branche est vérifiée à partir du même état ; il y a une valeur
        // de retour après le if seulement si les deux branches en ont une.
        s.getIfBlock().accept(this);
        Type retourAlors = typeRetour;
        typeRetour = null;
        s.getElseBlock().accept(this);
        Type retourSinon = typeRetour;

        if (retourAlors != null && retourSinon != null) {
            if (!estErreur(retourAlors) && !estErreur(retourSinon) && !retourAlors.equals(retourSinon)) {
                errors.add(
                    s,
                    "les deux branches du if renvoient des types différents (" +
                        retourAlors +
                        " et " +
                        retourSinon +
                        ")"
                );
                typeRetour = ERREUR;
            } else {
                typeRetour = retourSinon;
            }
        } else {
            typeRetour = null;
        }
        return STAT;
    }

    @Override
    public Type visit(StatWhile s) {
        verifierAtteignable(s);
        Type tc = s.getExpression().accept(this);
        if (!estErreur(tc) && !estBool(tc)) {
            errors.add(s, "la condition du while doit être de type boolean (trouvé : " + tc + ")");
        }
        // Le corps peut ne jamais être exécuté : un return à l'intérieur ne
        // garantit pas que la fonction renvoie une valeur.
        Type avant = typeRetour;
        s.getBlock().accept(this);
        typeRetour = avant;
        return STAT;
    }

    /* ---------------------------------------------------------------- */
    /*  Expressions                                                     */
    /* ---------------------------------------------------------------- */

    @Override
    public Type visit(Id i) {
        return typeVariable(i, i.getName());
    }

    @Override
    public Type visit(ExpId e) {
        return typeVariable(e, e.getValue());
    }

    @Override
    public Type visit(ExpInt e) {
        return intType(e);
    }

    @Override
    public Type visit(ExpRead e) {
        return intType(e);
    }

    @Override
    public Type visit(ExpCons e) {
        return boolType(e);
    }

    @Override
    public Type visit(ExpBin e) {
        Type tg = e.getLeftExp().accept(this);
        Type td = e.getRightExp().accept(this);
        if (estErreur(tg) || estErreur(td)) {
            return ERREUR;
        }
        BinOp op = e.getOp();
        switch (op) {
            case ADD, MIN, MULT, DIV:
                if (estInt(tg) && estInt(td)) return intType(e);
                errors.add(e, "l'opérateur " + op + " attend deux int (trouvé : " + tg + " et " + td + ")");
                return ERREUR;
            case LT, LEQ, GT, GEQ:
                if (estInt(tg) && estInt(td)) return boolType(e);
                errors.add(e, "l'opérateur " + op + " attend deux int (trouvé : " + tg + " et " + td + ")");
                return ERREUR;
            case AND, OR:
                if (estBool(tg) && estBool(td)) return boolType(e);
                errors.add(
                    e,
                    "l'opérateur " + op + " attend deux boolean (trouvé : " + tg + " et " + td + ")"
                );
                return ERREUR;
            case EQ, NEQ:
                if (tg.equals(td)) return boolType(e);
                errors.add(
                    e,
                    "l'opérateur " +
                        op +
                        " compare deux valeurs de même type (trouvé : " +
                        tg +
                        " et " +
                        td +
                        ")"
                );
                return ERREUR;
            default:
                throw new IllegalStateException("opérateur non géré : " + op);
        }
    }

    @Override
    public Type visit(ExpUn e) {
        Type t = e.getExp().accept(this);
        if (estErreur(t)) {
            return ERREUR;
        }
        switch (e.getOp()) {
            case NOT:
                if (estBool(t)) return boolType(e);
                errors.add(e, "l'opérateur ! attend un boolean (trouvé : " + t + ")");
                return ERREUR;
            case MIN:
                if (estInt(t)) return intType(e);
                errors.add(e, "l'opérateur - attend un int (trouvé : " + t + ")");
                return ERREUR;
            default:
                throw new IllegalStateException("opérateur non géré : " + e.getOp());
        }
    }

    @Override
    public Type visit(ExpCallMethod ec) {
        String nom = ec.getMethod().getName();
        List<Expression> args = ec.getArgs();

        MethodSig sig = table.methodLookup(nom);
        if (sig == null) {
            errors.add(ec, "fonction " + nom + " non définie");
            for (Expression a : args) a.accept(this);
            return ERREUR;
        }

        List<Formal> params = sig.getParams();
        if (params.size() != args.size()) {
            errors.add(
                ec,
                "la fonction " +
                    nom +
                    " attend " +
                    params.size() +
                    " argument(s) (trouvé : " +
                    args.size() +
                    ")"
            );
            return ERREUR;
        }
        for (int i = 0; i < params.size(); i++) {
            Type attendu = params.get(i).getType();
            Type trouve = args.get(i).accept(this);
            if (!estErreur(trouve) && !attendu.equals(trouve)) {
                errors.add(
                    ec,
                    "argument " +
                        (i + 1) +
                        " de " +
                        nom +
                        " : " +
                        attendu +
                        " attendu (trouvé : " +
                        trouve +
                        ")"
                );
            }
        }
        return sig.getReturnType();
    }

    @Override
    public Type visit(ExpNewTab e) {
        Type t = e.getSize().accept(this);
        if (!estErreur(t) && !estInt(t)) {
            errors.add(e, "la taille d'un tableau doit être de type int (trouvé : " + t + ")");
        }
        return new TypeTab(e.getPosition());
    }

    @Override
    public Type visit(ExpTabElt e) {
        String nom = e.getTab().getName();
        Type t = typeVariable(e, nom);
        Type ti = e.getIndex().accept(this);
        if (estErreur(t)) {
            return ERREUR;
        }
        if (!(t instanceof TypeTab)) {
            errors.add(e, nom + " n'est pas un tableau (type " + t + ")");
            return ERREUR;
        }
        if (!estErreur(ti) && !estInt(ti)) {
            errors.add(e, "l'indice d'un tableau doit être de type int (trouvé : " + ti + ")");
        }
        return intType(e);
    }
}
