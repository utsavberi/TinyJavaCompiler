package grammar;

/**************************************************/
/* OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL  */
/* CSE 505 Assignment 2                           */
/* October 15, 2014                               */
/*                                                */
/* Authors:                                       */
/* Utsav Beri (ubid : utsavber)                   */
/* Abha Khanna (ubid : abhakhan)                  */
/**************************************************/


import java.util.HashMap;

/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL
 
Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'
 
Lexical:   id is a single character; 
	      int_lit is an unsigned integer;
		 equality operator is =, not ==
 
 */

public class Parser {
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex();
		new Program();
		Code.output();
	}
}

class Program {
	 //decls stmts end
	Decls decls;
	Stmts stmts;
	public Program(){
		 try {
			 if(Lexer.nextToken == Token.KEY_INT){
				 decls = new Decls();
			 }
			 stmts = new Stmts();
			 Code.gen("return");
		} catch (ParserStateException e) {
			e.printStackTrace();
		}
		
	}
}

class Decls {
	Idlist idlist ;
	
	public Decls() throws ParserStateException{
			Lexer.lex();
			idlist = new Idlist();
		}
}

class Idlist {
	public Idlist()
	{
		int token = Lexer.nextToken;
		int i = 0;
		while(token != Token.SEMICOLON ){
			if(token!=Token.COMMA){
				Code.symbolTable.put(Lexer.ident,i++);
			}
			Lexer.lex();
			token = Lexer.nextToken;
		}
		Lexer.lex();//Lex semicolon
		
	}
	 
}

class Stmt {
	 //assign | cond | loop
	Assign assign;
	Cond cond;
	Loop loop;
	public Stmt() throws ParserStateException{
//		assign  ->  id = expr ;
//		 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
//		 loop    ->  while '(' rexp ')' cmpdstmt 
		switch(Lexer.nextToken){
		case Token.ID:
			assign = new Assign();
			break;
		case Token.KEY_IF:
			cond = new Cond();
			break;
		case Token.KEY_WHILE:
			loop = new Loop();
			break;
		default :
			throw new ParserStateException(Lexer.nextChar);
		}
		
	}
} 

class Stmts {
	//stmt [ stmts ]
	Stmt s;
	Stmts ss;
	public Stmts() throws ParserStateException
	{
		s = new Stmt();
		if(Lexer.nextToken == Token.KEY_IF ||Lexer.nextToken == Token.KEY_WHILE ||Lexer.nextToken == Token.ID ){
			ss = new Stmts();
		}
		
	}
	 
}

class Assign {
	Expr expr;
	public Assign() throws ParserStateException{
		int id_number;
		if(Code.symbolTable.containsKey(Lexer.ident))
		{
		 id_number= Code.symbolTable.get(Lexer.ident);
		}
		else{
			throw new ParserStateException(Lexer.nextChar);
		}
		Lexer.lex();
		if(Lexer.nextToken == Token.ASSIGN_OP){
			Lexer.lex();
			}
		else throw new ParserStateException(Lexer.nextChar);
		expr= new Expr();
		if(Lexer.nextToken == Token.SEMICOLON){
			Lexer.lex();
			}
		else throw new ParserStateException(Lexer.nextChar);
		if(id_number<=3){
			Code.gen("istore_"+id_number);
			}
		else{
			Code.gen("istore \t"+id_number,2);
			}
	}
}

class Cond {
	//if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
	Rexpr rexpr;
	Cmpdstmt ifBody;
	Cmpdstmt elseBody;
	
	int  condStartCodePtr[] = new int[1];
	int condEndCodePtr;
	int condExpr1start ;
	int elseEndCodePtr ;

	public Cond() throws ParserStateException{
		if(Lexer.nextToken == Token.KEY_IF){
			Lexer.lex();
		}
		if(Lexer.nextToken == Token.LEFT_PAREN){
			Lexer.lex();
		}
		else throw new ParserStateException(Lexer.nextChar);
		rexpr = new Rexpr(condStartCodePtr);
		if(Lexer.nextToken == Token.RIGHT_PAREN){
			Lexer.lex();
		}
		else throw new ParserStateException(Lexer.nextChar);
		if(Lexer.nextToken==Token.LEFT_BRACE){
			Lexer.lex();
		}
		else throw new ParserStateException(Lexer.nextChar);
		ifBody = new Cmpdstmt();
		if(Lexer.nextToken == Token.RIGHT_BRACE){
			Lexer.lex();
		}
		condEndCodePtr = Code.codeptr;
		
		if(Lexer.nextToken == Token.KEY_ELSE){
			int elseGotoCodePtr = Code.codeptr;
			Code.gen("goto",3);
			condEndCodePtr = Code.codeptr;
			Lexer.lex();
			if(Lexer.nextToken==Token.LEFT_BRACE){
				Lexer.lex();
			}
			elseBody = new Cmpdstmt();
			if(Lexer.nextToken==Token.RIGHT_BRACE){
				Lexer.lex();
			}
			elseEndCodePtr = Code.codeptr;
				Code.code[elseGotoCodePtr] = Code.code[elseGotoCodePtr] + "\t"+elseEndCodePtr;
		}
			Code.code[condStartCodePtr[0]] = Code.code[condStartCodePtr[0]]+"\t"+ condEndCodePtr;
	}
}

class Loop {
	//while '(' rexp ')' cmpdstmt 
	Rexpr rexpr ;
	Cmpdstmt cmpdstmt;
	int  loopStartCodePtr[] = new int[1];
	int loopEndCodePtr;
	int loopExpr1start ;
	
	public Loop() throws ParserStateException{
		if(Lexer.nextToken == Token.KEY_WHILE){
			Lexer.lex();
		}
		if(Lexer.nextToken == Token.LEFT_PAREN){
			Lexer.lex();
		}
		else throw new ParserStateException(Lexer.nextChar);
		
		loopExpr1start = Code.codeptr;
		rexpr = new Rexpr(loopStartCodePtr);
		if(Lexer.nextToken == Token.RIGHT_PAREN){
			Lexer.lex();
		}
		else throw new ParserStateException(Lexer.nextChar);
		
		if(Lexer.nextToken==Token.LEFT_BRACE){
			Lexer.lex();
		}
		else throw new ParserStateException(Lexer.nextChar);
		cmpdstmt = new Cmpdstmt();
		if(Lexer.nextToken==Token.RIGHT_BRACE){
			Lexer.lex();
		}
		else throw new ParserStateException(Lexer.nextChar);
		
		Code.gen("goto\t"+loopExpr1start,3);
		loopEndCodePtr = Code.codeptr;
		Code.code[loopStartCodePtr[0]] =Code.code[loopStartCodePtr[0]] + "\t" + loopEndCodePtr; 
	}
}

class Cmpdstmt {
	 //'{' stmts '}'
	Stmts stmts;
	public Cmpdstmt() throws ParserStateException{
		stmts = new Stmts();
	}
}

class Rexpr {
	 //expr (< | > | =) expr
	Expr expr1 ;
	Expr expr2 ; 
	
	String compOp;
	public Rexpr(int[] loopStartCodePtr) throws ParserStateException{
		
		expr1 = new Expr();
		switch (Lexer.nextToken) {
		case Token.ASSIGN_OP:
			compOp = "if_icmpne";
			Lexer.lex();
			break;
		case Token.NOT_EQ:
			compOp = "if_icmpe";
			Lexer.lex();
			break;
		case Token.LESSER_OP:
			compOp = "if_icmpge";
			Lexer.lex();
			break;
		case Token.GREATER_OP:
			compOp = "if_icmple";
			Lexer.lex();
			break;

		default:
			throw new ParserStateException(Lexer.nextChar);
		}
		expr2 = new Expr();
		loopStartCodePtr[0] = Code.codeptr;
		Code.gen(compOp,3);
		
	}
}

class Expr {  
	Term t;
	Expr e;
	char op;

	public Expr() throws ParserStateException {
		t = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			e = new Expr();
			Code.gen(Code.opcode(op));
		}
	}
}

class Term {  
	Factor f;
	Term t;
	char op;

	public Term() throws ParserStateException {
		f = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			t = new Term();
			Code.gen(Code.opcode(op));
		}
	}
}

class Factor {  
	Expr e;
	int i;

	public Factor() throws ParserStateException {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			Lexer.lex();
			if(i==-1){
				Code.gen("iconst_m1");				
			}
			else if(i>=0 && i <=5){
				Code.gen("iconst_" + i);
			}
			else if(i<=127 && i>=-128){
				Code.gen("bipush" +"\t"+ i,2);
				
			}
			else if(i<=32767 && i>=-32768){
				Code.gen("sipush" +"\t"+ i,3);
				
			}
//			else {
//				Code.gen("ldc" +"\t"+ i,3);
//			}
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			e = new Expr();
			Lexer.lex(); // skip over ')'
			break;
		case Token.ID:
			int id_number;
			if(Code.symbolTable.containsKey(Lexer.ident)){
			 id_number = Code.symbolTable.get(Lexer.ident);}
			else {throw new ParserStateException(Lexer.nextChar);}
			if(id_number<=3)
			{
				Code.gen("iload_"+id_number);
			}
			else{
				Code.gen("iload\t"+id_number,2);
			}
			Lexer.lex();
			break;
		default:
			throw new ParserStateException(Lexer.nextChar);
		}
	}
}

class Code {
	
	static HashMap<Character,Integer> symbolTable = new HashMap<Character, Integer>();
	static String[] code = new String[100];
	static int codeptr = 0;

	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}
	

	public static void gen(String s, int byteLen) {
		code[codeptr] = s;
		codeptr=codeptr+byteLen;
	}
	
	public static String opcode(char op) {
		switch(op) {
		case '+' : return "iadd";
		case '-':  return "isub";
		case '*':  return "imul";
		case '/':  return "idiv";
		default: return "";
		}
	}
	
	public static void output() {
		for (int i=0; i<codeptr; i++)
		{
			if(code[i]!=null)
				System.out.println(i + ": " + code[i]);
		}
		
	}
	 
}

class ParserStateException extends Exception {
	  /**
	 * 
	 */
	
	private static final long serialVersionUID = -7245550927607215477L;
	public ParserStateException() { super(); }
	  public ParserStateException(char tokenCh) { super("Error at character : '" + tokenCh+"'"); }
	  public ParserStateException(String message, Throwable cause) { super(message, cause); }
	  public ParserStateException	(Throwable cause) { super(cause); }
	}