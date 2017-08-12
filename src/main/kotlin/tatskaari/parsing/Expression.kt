package tatskaari.parsing

import tatskaari.tokenising.Operator

sealed class Expression {
  data class Num(val value: Int) : Expression()
  data class Ident(val name : String) : Expression()
  data class Op(val operator : Operator, val lhs: Expression, val rhs: Expression) : Expression()
}