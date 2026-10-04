package ast;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import parser.SdmBaseVisitor;
import parser.SdmParser;
import support.Errors;

/**
 * Construit l'arbre de syntaxe abstraite (AST) à partir de l'arbre
 * d'analyse produit par ANTLR.
 *
 * Quelques constructions sont « désucrées » au passage :
 * <ul>
 *   <li>{@code x++;} devient {@code x = x + 1;}</li>
 *   <li>{@code type x = e;} devient {@code type x; x = e;}</li>
 *   <li>{@code for (init cond; pas) corps} devient
 *       {@code init; while (cond) { corps pas }}</li>
 *   <li>{@code main() s} devient {@code int main() { s return 0; }}</li>
 * </ul>
 */
public class AstBuild extends SdmBaseVisitor<Node> {

    private final Errors errors = new Errors();

    public Errors getErrors() {
        return errors;
    }

    private static Position position(ParserRuleContext ctx) {
        return new Position(ctx.start.getLine(), ctx.start.getCharPositionInLine());
    }

    /* ---------------------------------------------------------------- */
    /*  Programme et fonctions                                          */
    /* ---------------------------------------------------------------- */

    @Override
    public Node visitProgram(SdmParser.ProgramContext ctx) {
        List<MethodDecl> methods = new ArrayList<>();
        for (SdmParser.MethodDeclContext md : ctx.methodDecl()) {
            methods.add((MethodDecl) visit(md));
        }
        methods.add((MethodDecl) visit(ctx.mainMethod()));
        return new Program(position(ctx), methods);
    }

    @Override
    public Node visitMainMethod(SdmParser.MainMethodContext ctx) {
        Position pos = position(ctx);
        List<Statement> body = new ArrayList<>();
        body.add((Statement) visit(ctx.statement()));
        body.add(new StatReturn(pos, new ExpInt(pos, 0)));
        return new MethodDecl(
            pos,
            new TypePrim(pos, TypePrim.Prim.INT),
            new Id(pos, "main"),
            new ArrayList<>(),
            new Block(pos, new StatList(pos, body))
        );
    }

    @Override
    public Node visitMethodDecl(SdmParser.MethodDeclContext ctx) {
        Type type = (Type) visit(ctx.type());
        Id id = new Id(position(ctx), ctx.Id().getText());
        List<Formal> formals = new ArrayList<>();
        for (SdmParser.FormalContext fc : ctx.formal()) {
            formals.add((Formal) visit(fc));
        }
        Statement body = (Statement) visit(ctx.statement());
        return new MethodDecl(position(ctx), type, id, formals, new Block(position(ctx), body));
    }

    @Override
    public Node visitFormal(SdmParser.FormalContext ctx) {
        Type type = (Type) visit(ctx.type());
        Id id = new Id(position(ctx), ctx.Id().getText());
        return new Formal(position(ctx), type, id);
    }

    /* ---------------------------------------------------------------- */
    /*  Types                                                           */
    /* ---------------------------------------------------------------- */

    @Override
    public Node visitIntType(SdmParser.IntTypeContext ctx) {
        return new TypePrim(position(ctx), TypePrim.Prim.INT);
    }

    @Override
    public Node visitBoolType(SdmParser.BoolTypeContext ctx) {
        return new TypePrim(position(ctx), TypePrim.Prim.BOOL);
    }

    @Override
    public Node visitIntArrayType(SdmParser.IntArrayTypeContext ctx) {
        return new TypeTab(position(ctx));
    }

    /* ---------------------------------------------------------------- */
    /*  Instructions                                                    */
    /* ---------------------------------------------------------------- */

    @Override
    public Node visitStatList(SdmParser.StatListContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (SdmParser.StatementContext s : ctx.statement()) {
            statements.add((Statement) visit(s));
        }
        return new StatList(position(ctx), statements);
    }

    @Override
    public Node visitStatIf(SdmParser.StatIfContext ctx) {
        Position pos = position(ctx);
        Expression cond = (Expression) visit(ctx.exp());
        Block thenBlock = new Block(pos, (Statement) visit(ctx.statement(0)));
        Block elseBlock = new Block(pos, (Statement) visit(ctx.statement(1)));
        return new StatIf(pos, cond, thenBlock, elseBlock);
    }

    @Override
    public Node visitStatWhile(SdmParser.StatWhileContext ctx) {
        Position pos = position(ctx);
        Expression cond = (Expression) visit(ctx.exp());
        Block body = new Block(pos, (Statement) visit(ctx.statement()));
        return new StatWhile(pos, cond, body);
    }

    @Override
    public Node visitStatFor(SdmParser.StatForContext ctx) {
        Position pos = position(ctx);
        Statement init = (Statement) visit(ctx.statement(0));
        Expression cond = (Expression) visit(ctx.exp());
        Statement step = (Statement) visit(ctx.statement(1));
        Statement body = (Statement) visit(ctx.statement(2));

        List<Statement> loopBody = new ArrayList<>();
        loopBody.add(body);
        loopBody.add(step);
        StatWhile loop = new StatWhile(pos, cond, new Block(pos, new StatList(pos, loopBody)));

        List<Statement> result = new ArrayList<>();
        result.add(init);
        result.add(loop);
        return new StatList(pos, result);
    }

    @Override
    public Node visitStatPrint(SdmParser.StatPrintContext ctx) {
        return new StatPrint(position(ctx), (Expression) visit(ctx.exp()));
    }

    @Override
    public Node visitStatAff(SdmParser.StatAffContext ctx) {
        Id id = new Id(position(ctx), ctx.Id().getText());
        return new StatAff(position(ctx), id, (Expression) visit(ctx.exp()));
    }

    @Override
    public Node visitStatAffTab(SdmParser.StatAffTabContext ctx) {
        Id id = new Id(position(ctx), ctx.Id().getText());
        Expression index = (Expression) visit(ctx.exp(0));
        Expression value = (Expression) visit(ctx.exp(1));
        return new StatAffTab(position(ctx), id, index, value);
    }

    @Override
    public Node visitStatIncr(SdmParser.StatIncrContext ctx) {
        Position pos = position(ctx);
        String name = ctx.Id().getText();
        Expression plusOne = new ExpBin(pos, new ExpId(pos, name), BinOp.ADD, new ExpInt(pos, 1));
        return new StatAff(pos, new Id(pos, name), plusOne);
    }

    @Override
    public Node visitStatReturn(SdmParser.StatReturnContext ctx) {
        return new StatReturn(position(ctx), (Expression) visit(ctx.exp()));
    }

    @Override
    public Node visitStatVarDecl(SdmParser.StatVarDeclContext ctx) {
        Type type = (Type) visit(ctx.type());
        Id id = new Id(position(ctx), ctx.Id().getText());
        return new StatVarDecl(position(ctx), id, type);
    }

    @Override
    public Node visitStatVarDeclAff(SdmParser.StatVarDeclAffContext ctx) {
        Position pos = position(ctx);
        Type type = (Type) visit(ctx.type());
        Id id = new Id(pos, ctx.Id().getText());
        Expression value = (Expression) visit(ctx.exp());

        List<Statement> statements = new ArrayList<>();
        statements.add(new StatVarDecl(pos, id, type));
        statements.add(new StatAff(pos, id, value));
        return new StatList(pos, statements);
    }

    /* ---------------------------------------------------------------- */
    /*  Expressions                                                     */
    /* ---------------------------------------------------------------- */

    @Override
    public Node visitExBinop(SdmParser.ExBinopContext ctx) {
        Expression left = (Expression) visit(ctx.exp(0));
        Expression right = (Expression) visit(ctx.exp(1));
        BinOp op = switch (ctx.op.getText()) {
            case "+" -> BinOp.ADD;
            case "-" -> BinOp.MIN;
            case "*" -> BinOp.MULT;
            case "/" -> BinOp.DIV;
            case "<" -> BinOp.LT;
            case "<=" -> BinOp.LEQ;
            case ">" -> BinOp.GT;
            case ">=" -> BinOp.GEQ;
            case "==" -> BinOp.EQ;
            case "!=" -> BinOp.NEQ;
            case "&&" -> BinOp.AND;
            case "||" -> BinOp.OR;
            default -> throw new IllegalStateException("opérateur binaire inconnu : " + ctx.op.getText());
        };
        return new ExpBin(position(ctx), left, op, right);
    }

    @Override
    public Node visitExUnop(SdmParser.ExUnopContext ctx) {
        Expression exp = (Expression) visit(ctx.exp());
        UnOp op = switch (ctx.op.getText()) {
            case "!" -> UnOp.NOT;
            case "-" -> UnOp.MIN;
            default -> throw new IllegalStateException("opérateur unaire inconnu : " + ctx.op.getText());
        };
        return new ExpUn(position(ctx), exp, op);
    }

    @Override
    public Node visitExInt(SdmParser.ExIntContext ctx) {
        String text = ctx.Int().getText();
        try {
            return new ExpInt(position(ctx), Integer.parseInt(text));
        } catch (NumberFormatException e) {
            Position pos = position(ctx);
            errors.add(pos + " : l'entier " + text + " dépasse la capacité d'un int (32 bits)");
            return new ExpInt(pos, 0);
        }
    }

    @Override
    public Node visitExTrue(SdmParser.ExTrueContext ctx) {
        return new ExpCons(position(ctx), Constant.TRUE);
    }

    @Override
    public Node visitExFalse(SdmParser.ExFalseContext ctx) {
        return new ExpCons(position(ctx), Constant.FALSE);
    }

    @Override
    public Node visitExRead(SdmParser.ExReadContext ctx) {
        return new ExpRead(position(ctx));
    }

    @Override
    public Node visitExNewTab(SdmParser.ExNewTabContext ctx) {
        return new ExpNewTab(position(ctx), (Expression) visit(ctx.exp()));
    }

    @Override
    public Node visitExCall(SdmParser.ExCallContext ctx) {
        Id method = new Id(position(ctx), ctx.Id().getText());
        List<Expression> args = new ArrayList<>();
        for (SdmParser.ExpContext e : ctx.exp()) {
            args.add((Expression) visit(e));
        }
        return new ExpCallMethod(position(ctx), method, args);
    }

    @Override
    public Node visitExTabElt(SdmParser.ExTabEltContext ctx) {
        Id id = new Id(position(ctx), ctx.Id().getText());
        return new ExpTabElt(position(ctx), id, (Expression) visit(ctx.exp()));
    }

    @Override
    public Node visitExId(SdmParser.ExIdContext ctx) {
        return new ExpId(position(ctx), ctx.Id().getText());
    }

    @Override
    public Node visitExParenthesis(SdmParser.ExParenthesisContext ctx) {
        return visit(ctx.exp());
    }
}
