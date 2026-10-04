/*
 * Grammaire du langage SDM (« Software, Data and Methods »).
 *
 * Un programme est une suite de fonctions suivie de la fonction main().
 * Types : int, boolean, int[] (tableaux d'entiers).
 */
grammar Sdm;

/* ------------------------------------------------------------------ */
/*  Règles syntaxiques                                                */
/* ------------------------------------------------------------------ */

program    : methodDecl* mainMethod EOF ;

mainMethod : 'main' '(' ')' statement ;

methodDecl : type Id '(' (formal (',' formal)*)? ')' statement ;

formal     : type Id ;

type
    : 'int'                 # intType
    | 'boolean'             # boolType
    | 'int' '[' ']'         # intArrayType
    ;

statement
    : '{' statement* '}'                                        # statList
    | 'if' '(' exp ')' statement 'else' statement               # statIf
    | 'while' '(' exp ')' statement                             # statWhile
    | 'for' '(' statement exp ';' statement ')' statement       # statFor
    | 'print' '(' exp ')' ';'                                   # statPrint
    | Id '=' exp ';'                                            # statAff
    | Id '[' exp ']' '=' exp ';'                                # statAffTab
    | Id '++' ';'                                               # statIncr
    | 'return' exp ';'                                          # statReturn
    | type Id ';'                                               # statVarDecl
    | type Id '=' exp ';'                                       # statVarDeclAff
    ;

/*
 * Expressions, de la priorité la plus forte à la plus faible :
 *   unaires (! -)  >  * /  >  + -  >  < <= > >=  >  == !=  >  &&  >  ||
 * Tous les opérateurs binaires sont associatifs à gauche.
 */
exp
    : op=('!' | '-') exp                                # exUnop
    | exp op=('*' | '/') exp                            # exBinop
    | exp op=('+' | '-') exp                            # exBinop
    | exp op=('<' | '<=' | '>' | '>=') exp              # exBinop
    | exp op=('==' | '!=') exp                          # exBinop
    | exp op='&&' exp                                   # exBinop
    | exp op='||' exp                                   # exBinop
    | Int                                               # exInt
    | 'true'                                            # exTrue
    | 'false'                                           # exFalse
    | 'read' '(' ')'                                    # exRead
    | 'new' 'int' '[' exp ']'                           # exNewTab
    | Id '(' (exp (',' exp)*)? ')'                      # exCall
    | Id '[' exp ']'                                    # exTabElt
    | Id                                                # exId
    | '(' exp ')'                                       # exParenthesis
    ;

/* ------------------------------------------------------------------ */
/*  Lexèmes                                                           */
/* ------------------------------------------------------------------ */

Id            : [a-zA-Z] [a-zA-Z0-9_]* ;
Int           : [0-9]+ ;

LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
WS            : [ \t\r\n]+ -> skip ;
