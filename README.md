# TinyJavaCompiler
An object-oriented top-down parser in Java that translates every TinyJavaPL program into an 
equivalent sequence of byte codes for a Java Virtual Machine

## Grammar for a simple programming language, TinyPL
    program -> decls stmts end
    decls -> int idlist ;
    idlist -> id { , id }
    stmts -> stmt [ stmts ]
    cmpdstmt-> '{' stmts '}'
    stmt -> assign | loop | cond
    assign -> id = expr ;
    loop -> while '(' rexp ')' cmpdstmt
    cond -> if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
    rexp -> expr (< | > | = | !=) expr
    expr -> term [ (+ | -) expr ]
    term -> factor [ (* | /) term ]
    factor -> int_lit | id | ‘(‘ expr ‘)’
