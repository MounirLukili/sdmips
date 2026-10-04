package ir.translation;

import ast.*;
import ast.Expression;
import ir.Frame;
import ir.Register;
import ir.com.*;
import ir.expr.Binary;
import ir.expr.ReadMem;
import ir.expr.ReadReg;
import ir.expr.Unary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import semantic.MethodSig;
import semantic.SymbolTable;
import semantic.VisitedBlocks;
import support.Errors;
import support.ListTools;
import support.Pair;

/**
 * Traduction de l'AST (déjà vérifié) en code intermédiaire.
 *
 * Chaque fonction devient un fragment : son cadre ({@link Frame}) et sa liste
 * de commandes. Chaque variable est associée à un registre virtuel (qui sera
 * placé dans le cadre de pile lors de la génération MIPS).
 */
public class Translate {

    /** Taille d'une case de tableau, en octets. */
    private static final int TAILLE_INT = 4;

    private static final TypeConverter typeConverter = new TypeConverter();

    private static ir.Type ofType(Type type) {
        return type.accept(typeConverter);
    }

    /** Résultat de la traduction : label du main et fragments (cadre, code). */
    public static final class Resultat {

        public final Label mainLabel;
        public final List<Pair<Frame, List<Command>>> fragments;
        public final Errors errors;

        Resultat(Label mainLabel, List<Pair<Frame, List<Command>>> fragments, Errors errors) {
            this.mainLabel = mainLabel;
            this.fragments = fragments;
            this.errors = errors;
        }
    }

    public static Resultat run(SymbolTable symbolTable, Program program) {
        TranslationVisitor translator = new TranslationVisitor(symbolTable);
        program.accept(translator);
        if (translator.mainLabel == null) {
            translator.errors.add("pas de fonction main");
        }
        return new Resultat(translator.mainLabel, translator.fragments, translator.errors);
    }

    /** Conversion des types de l'AST vers les types de l'IR. */
    private static class TypeConverter extends ast.BaseVisitor<ir.Type> {

        TypeConverter() {
            super(null);
        }

        @Override
        public ir.Type visit(TypePrim type) {
            return switch (type.getPrim()) {
                case INT -> ir.Type.INT;
                case BOOL -> ir.Type.BYTE;
                default -> throw new IllegalStateException("type non traduisible : " + type);
            };
        }

        @Override
        public ir.Type visit(TypeTab type) {
            return ir.Type.INT; // un tableau est représenté par son adresse
        }
    }

    private static class TranslationVisitor extends ast.BaseVisitor<Result> {

        final Errors errors = new Errors();
        final List<Pair<Frame, List<Command>>> fragments = new LinkedList<>();
        Label mainLabel;

        private final SymbolTable symbolTable;
        private final VisitedBlocks visitedBlocks = new VisitedBlocks();

        /** Cadre de chaque fonction (construits avant la traduction des corps). */
        private final Map<String, Frame> frames = new HashMap<>();

        /** Pour chaque bloc : nom de variable → registre. */
        private final Map<Block, Map<String, Register>> varToReg = new HashMap<>();

        /** Cadre de la fonction en cours de traduction. */
        private Frame currentFrame;

        TranslationVisitor(SymbolTable symbolTable) {
            super(null);
            this.symbolTable = symbolTable;
        }

        /* ------------------------------------------------------------ */
        /*  Outils                                                      */
        /* ------------------------------------------------------------ */

        private Register registerLookup(String name) {
            for (Block block : visitedBlocks.getStack()) {
                Register reg = varToReg.get(block).get(name);
                if (reg != null) {
                    return reg;
                }
            }
            errors.add("erreur interne : pas de registre pour la variable " + name);
            return new Register(ir.Type.INT);
        }

        /** Nouveau registre temporaire, ajouté au cadre courant. */
        private Register temporaire(ir.Type type) {
            Register reg = new Register(type);
            currentFrame.addLocal(reg);
            return reg;
        }

        /** Appel de fonction : le résultat est rangé dans un nouveau registre temporaire. */
        private Result makeFunCall(
            ir.Type typeRetour,
            Frame frame,
            List<ir.expr.Expression> args,
            List<Command> code
        ) {
            Register reg = temporaire(typeRetour);
            code.add(new FunCall(reg, frame, args));
            return new Result(new ReadReg(reg), code);
        }

        /** Cadre d'une routine prédéfinie (écrite à la main dans runtime/*.asm). */
        private static Frame routine(String nom, int nbParametres) {
            List<Register> params = new LinkedList<>();
            for (int i = 0; i < nbParametres; i++) {
                params.add(new Register(ir.Type.INT));
            }
            return new Frame(
                Label.named("entry" + nom),
                Label.named("exit" + nom),
                params,
                new Register(ir.Type.INT)
            );
        }

        /** Adresse de t[i] (= t + 4*i), rangée dans un registre temporaire. */
        private Register adresseCase(String tableau, Result indice, List<Command> code) {
            ir.expr.Expression decalage = new Binary(
                indice.getExp(),
                new ir.expr.Int(TAILLE_INT),
                BinOp.MULT
            );
            ir.expr.Expression adresse = new Binary(
                new ReadReg(registerLookup(tableau)),
                decalage,
                BinOp.ADD
            );
            Register reg = temporaire(ir.Type.INT);
            code.add(new WriteReg(reg, adresse));
            return reg;
        }

        /* ------------------------------------------------------------ */
        /*  Programme, fonctions, blocs                                 */
        /* ------------------------------------------------------------ */

        @Override
        public Result visit(Program program) {
            // Tous les cadres sont créés d'abord : une fonction peut en appeler
            // une autre définie plus loin (ou s'appeler elle-même).
            for (MethodDecl fun : program.getListMethodDecl()) {
                creerCadre(fun);
            }
            for (MethodDecl fun : program.getListMethodDecl()) {
                fun.accept(this);
            }
            return null;
        }

        private void creerCadre(MethodDecl function) {
            Block body = function.getBlock();
            Map<String, Register> locales = new HashMap<>();
            varToReg.put(body, locales);

            List<Register> parameters = new LinkedList<>();
            for (Formal argument : function.getFormal()) {
                Register reg = new Register(ofType(argument.getType()));
                locales.put(argument.getId().getName(), reg);
                parameters.add(reg);
            }

            Frame frame = new Frame(
                Label.fresh(),
                Label.fresh(),
                parameters,
                new Register(ofType(function.getType()))
            );
            frames.put(function.getId().getName(), frame);
            if (function.getId().getName().equals("main")) {
                mainLabel = frame.getEntryPoint();
            }
        }

        @Override
        public Result visit(MethodDecl md) {
            currentFrame = frames.get(md.getId().getName());
            Result body = md.getBlock().accept(this);
            fragments.add(new Pair<>(currentFrame, body.getCode()));
            currentFrame = null;
            return null;
        }

        @Override
        public Result visit(Block b) {
            varToReg.computeIfAbsent(b, k -> new HashMap<>());
            visitedBlocks.enter(b);
            Result result = b.getStatement().accept(this);
            visitedBlocks.exit();
            return new Result(result.getCode());
        }

        /* ------------------------------------------------------------ */
        /*  Instructions                                                */
        /* ------------------------------------------------------------ */

        @Override
        public Result visit(StatList sl) {
            List<Command> code = new LinkedList<>();
            for (Statement stm : sl.getStatList()) {
                code.addAll(stm.accept(this).getCode());
            }
            return new Result(code);
        }

        @Override
        public Result visit(StatVarDecl stm) {
            // Une variable = un registre, rangé dans la table du bloc courant.
            Register reg = temporaire(ofType(stm.getType()));
            varToReg.get(visitedBlocks.current()).put(stm.getId().getName(), reg);
            return new Result(new LinkedList<>());
        }

        @Override
        public Result visit(StatAff s) {
            Result r = s.getExpression().accept(this);
            List<Command> code = new LinkedList<>(r.getCode());
            code.add(new WriteReg(registerLookup(s.getId().getName()), r.getExp()));
            return new Result(code);
        }

        @Override
        public Result visit(StatAffTab s) {
            Result indice = s.getIndex().accept(this);
            Result valeur = s.getValue().accept(this);
            List<Command> code = new LinkedList<>(indice.getCode());
            code.addAll(valeur.getCode());
            Register adresse = adresseCase(s.getTab().getName(), indice, code);
            code.add(new WriteMem(new ReadReg(adresse), valeur.getExp()));
            return new Result(code);
        }

        @Override
        public Result visit(StatPrint s) {
            Result r = s.getExpression().accept(this);
            // Les booléens sont affichés "true" / "false", les entiers en décimal.
            boolean booleen = r.getExp().getType() == ir.Type.BYTE;
            Frame frame = routine(booleen ? "PrintBool" : "PrintInt", 1);
            return makeFunCall(ir.Type.INT, frame, ListTools.mklist(r.getExp()), r.getCode());
        }

        @Override
        public Result visit(StatReturn stm) {
            Result r = stm.getExpression().accept(this);
            List<Command> code = new LinkedList<>(r.getCode());
            code.add(new WriteReg(currentFrame.getResult(), r.getExp()));
            code.add(new Jump(currentFrame.getExitPoint()));
            return new Result(code);
        }

        @Override
        public Result visit(StatIf stm) {
            Result cond = stm.getExpression().accept(this);
            Result alors = stm.getIfBlock().accept(this);
            Result sinon = stm.getElseBlock().accept(this);

            Label lAlors = Label.fresh();
            Label lSinon = Label.fresh();
            Label lFin = Label.fresh();

            List<Command> code = new LinkedList<>(cond.getCode());
            code.add(new CJump(cond.getExp(), lAlors, lSinon));
            code.add(lAlors);
            code.addAll(alors.getCode());
            code.add(new Jump(lFin));
            code.add(lSinon);
            code.addAll(sinon.getCode());
            code.add(lFin);
            return new Result(code);
        }

        @Override
        public Result visit(StatWhile stm) {
            Label lTest = Label.fresh();
            Label lCorps = Label.fresh();
            Label lFin = Label.fresh();

            Result cond = stm.getExpression().accept(this);
            Result corps = stm.getBlock().accept(this);

            List<Command> code = new LinkedList<>();
            code.add(lTest);
            code.addAll(cond.getCode());
            code.add(new CJump(cond.getExp(), lCorps, lFin));
            code.add(lCorps);
            code.addAll(corps.getCode());
            code.add(new Jump(lTest));
            code.add(lFin);
            return new Result(code);
        }

        /* ------------------------------------------------------------ */
        /*  Expressions                                                 */
        /* ------------------------------------------------------------ */

        @Override
        public Result visit(ExpInt exp) {
            return new Result(new ir.expr.Int(exp.getValue()));
        }

        @Override
        public Result visit(ExpCons exp) {
            byte value = (byte) (exp.getConstant() == Constant.TRUE ? 1 : 0);
            return new Result(new ir.expr.Byte(value));
        }

        @Override
        public Result visit(ExpId exp) {
            return new Result(new ReadReg(registerLookup(exp.getValue())));
        }

        @Override
        public Result visit(ExpBin exp) {
            Result left = exp.getLeftExp().accept(this);
            Result right = exp.getRightExp().accept(this);
            List<Command> code = new LinkedList<>(left.getCode());
            code.addAll(right.getCode());
            return new Result(new Binary(left.getExp(), right.getExp(), exp.getOp()), code);
        }

        @Override
        public Result visit(ExpUn exp) {
            Result r = exp.getExp().accept(this);
            return new Result(new Unary(r.getExp(), exp.getOp()), r.getCode());
        }

        @Override
        public Result visit(ExpRead e) {
            return makeFunCall(ir.Type.INT, routine("ReadInt", 0), new LinkedList<>(), new LinkedList<>());
        }

        @Override
        public Result visit(ExpCallMethod exp) {
            String name = exp.getMethod().getName();
            List<ir.expr.Expression> arguments = new LinkedList<>();
            List<Command> code = new LinkedList<>();
            for (Expression e : exp.getArgs()) {
                Result r = e.accept(this);
                arguments.add(r.getExp());
                code.addAll(r.getCode());
            }
            MethodSig signature = symbolTable.methodLookup(name);
            Frame frame = frames.get(name);
            if (signature == null || frame == null) {
                errors.add("erreur interne : fonction " + name + " inconnue");
                return new Result(new ir.expr.Int(0), code);
            }
            return makeFunCall(ofType(signature.getReturnType()), frame, arguments, code);
        }

        @Override
        public Result visit(ExpNewTab exp) {
            // Allocation de taille*4 octets sur le tas (appel système sbrk).
            Result taille = exp.getSize().accept(this);
            List<Command> code = new LinkedList<>(taille.getCode());
            Register octets = temporaire(ir.Type.INT);
            code.add(
                new WriteReg(octets, new Binary(taille.getExp(), new ir.expr.Int(TAILLE_INT), BinOp.MULT))
            );
            return makeFunCall(ir.Type.INT, routine("Alloc", 1), ListTools.mklist(new ReadReg(octets)), code);
        }

        @Override
        public Result visit(ExpTabElt exp) {
            Result indice = exp.getIndex().accept(this);
            List<Command> code = new LinkedList<>(indice.getCode());
            Register adresse = adresseCase(exp.getTab().getName(), indice, code);
            return new Result(new ReadMem(new ReadReg(adresse)), code);
        }
    }
}
