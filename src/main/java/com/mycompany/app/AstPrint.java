package com.mycompany.app;

public class AstPrint {
  
  public String visitLiteral(Expr.Literal expr) {
    return "Literal(" + expr.value.toString() + ")";
  }

  public String visitSelect(Expr.Select expr){
    return "";
  }

}
