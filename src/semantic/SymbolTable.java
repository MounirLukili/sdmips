package semantic;

import ast.Block;
import ast.Type;
import java.util.HashMap;
import java.util.Map;
import support.Errors;

/**
 * Table des symboles.
 *
 * <ul>
 *   <li>chaque nom de fonction est associé à sa signature ;</li>
 *   <li>chaque bloc est associé à sa table locale (nom de variable → type).</li>
 * </ul>
 */
public class SymbolTable {

    private final Errors errors = new Errors();
    private final Map<String, MethodSig> methods = new HashMap<>();
    private final Map<Block, Map<String, Type>> blocks = new HashMap<>();

    public Errors getErrors() {
        return errors;
    }

    public void addMethod(String nom, MethodSig signature) {
        methods.put(nom, signature);
    }

    public MethodSig methodLookup(String nom) {
        return methods.get(nom);
    }

    /** Crée la table locale du bloc si elle n'existe pas encore. */
    public void localTable(Block bloc) {
        blocks.computeIfAbsent(bloc, b -> new HashMap<>());
    }

    public void addLocalVariable(Block bloc, String nom, Type type) {
        Map<String, Type> table = blocks.get(bloc);
        if (table == null) {
            errors.add(bloc, "[table des symboles] bloc inconnu pour la variable " + nom);
        } else {
            table.put(nom, type);
        }
    }

    /**
     * Cherche le type d'une variable dans les blocs en cours de visite.
     *
     * @return le type, ou null si la variable n'est déclarée dans aucun de ces blocs.
     */
    public Type variableLookup(String nom, VisitedBlocks visites) {
        for (Block b : visites.getStack()) {
            Map<String, Type> table = blocks.get(b);
            if (table == null) {
                errors.add(b, "[table des symboles] pas de table locale associée au bloc");
                continue;
            }
            Type t = table.get(nom);
            if (t != null) {
                return t;
            }
        }
        return null;
    }
}
