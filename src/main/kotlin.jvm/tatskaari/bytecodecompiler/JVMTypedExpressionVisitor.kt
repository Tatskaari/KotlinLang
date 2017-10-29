package tatskaari.bytecodecompiler


import tatskaari.PrimitiveType
import tatskaari.parsing.TypeChecking.ArithmeticOperator
import tatskaari.parsing.TypeChecking.TypedExpression
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.parsing.TypeChecking.BooleanLogicalOperator
import tatskaari.parsing.TypeChecking.NumericLogicalOperator


class JVMTypedExpressionVisitor (private val methodVisitor: InstructionAdapter): ITypedExpressionVisitor {
  override fun visit(expr: TypedExpression.BooleanLogicalOperation) {
    val trueLabel = Label()
    val falseLabel = Label()
    val endLabel = Label()
    when(expr.operator){
      BooleanLogicalOperator.And -> {
        expr.lhs.accept(this)
        methodVisitor.ifeq(falseLabel)
        expr.rhs.accept(this)
        methodVisitor.ifeq(falseLabel)
        methodVisitor.goTo(trueLabel)
      }
      BooleanLogicalOperator.Or -> {
        expr.lhs.accept(this)
        methodVisitor.ifne(trueLabel)
        expr.rhs.accept(this)
        methodVisitor.ifne(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
    }
    methodVisitor.visitLabel(trueLabel)
    methodVisitor.iconst(1)
    methodVisitor.goTo(endLabel)
    methodVisitor.visitLabel(falseLabel)
    methodVisitor.iconst(0)
    methodVisitor.visitLabel(endLabel)

  }

  override fun visit(expr: TypedExpression.IntLogicalOperation) {
    val trueLabel = Label()
    val falseLabel = Label()
    val endLabel = Label()
    expr.lhs.accept(this)
    expr.rhs.accept(this)

    when(expr.operator){
      NumericLogicalOperator.GreaterThan -> {
        methodVisitor.ificmpgt(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.LessThan -> {
        methodVisitor.ificmplt(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.GreaterThanEq -> {
        methodVisitor.ificmpge(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.LessThanEq -> {
        methodVisitor.ificmple(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
    }
    methodVisitor.visitLabel(trueLabel)
    methodVisitor.iconst(1)
    methodVisitor.goTo(endLabel)
    methodVisitor.visitLabel(falseLabel)
    methodVisitor.iconst(0)
    methodVisitor.visitLabel(endLabel)
  }

  override fun visit(expr: TypedExpression.NumLogicalOperation) {
    val trueLabel = Label()
    val falseLabel = Label()
    val endLabel = Label()

    expr.lhs.accept(this)
    if (expr.lhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(I2D)
    }

    expr.rhs.accept(this)
    if (expr.rhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(I2D)
    }

    when(expr.operator){
      NumericLogicalOperator.LessThan -> {
        methodVisitor.visitInsn(DCMPL)
        methodVisitor.iflt(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.GreaterThan -> {
        methodVisitor.visitInsn(DCMPG)
        methodVisitor.ifgt(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.LessThanEq -> {
        methodVisitor.visitInsn(DCMPL)
        methodVisitor.ifle(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.GreaterThanEq -> {
        methodVisitor.visitInsn(DCMPG)
        methodVisitor.ifge(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
    }
    methodVisitor.visitLabel(trueLabel)
    methodVisitor.iconst(1)
    methodVisitor.goTo(endLabel)
    methodVisitor.visitLabel(falseLabel)
    methodVisitor.iconst(0)
    methodVisitor.visitLabel(endLabel)
  }

  override fun visit(expr: TypedExpression.Equals) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.NotEquals) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }


  override fun visit(expr: TypedExpression.NumLiteral) {
    methodVisitor.dconst(expr.expr.value)
  }
  override fun visit(expr: TypedExpression.IntLiteral) {
    methodVisitor.iconst(expr.expr.value)
  }

  override fun visit(expr: TypedExpression.TextLiteral) {
    methodVisitor.aconst(expr.expr.value)
  }

  override fun visit(expr: TypedExpression.BooleanLiteral) {
    if (expr.expr.value){
      methodVisitor.visitInsn(ICONST_1)
    } else {
      methodVisitor.visitInsn(ICONST_0)
    }
  }

  override fun visit(expr: TypedExpression.Identifier) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.NegateNum) {
    expr.rhs.accept(this)
    methodVisitor.visitInsn(DNEG)
  }

  override fun visit(expr: TypedExpression.Not) {
    expr.rhs.accept(this)
    val l1 = Label()
    methodVisitor.visitJumpInsn(IFNE, l1)
    methodVisitor.visitInsn(ICONST_1)
    val l2 = Label()
    methodVisitor.visitJumpInsn(GOTO, l2)
    methodVisitor.visitLabel(l1)
    methodVisitor.visitInsn(ICONST_0)
    methodVisitor.visitLabel(l2)
  }

  override fun visit(expr: TypedExpression.NegateInt) {
    expr.rhs.accept(this)
    methodVisitor.visitInsn(INEG)
  }

  override fun visit(expr: TypedExpression.IntArithmeticOperation) {
    expr.lhs.accept(this)
    expr.rhs.accept(this)
    when(expr.operator){
      ArithmeticOperator.Add -> methodVisitor.visitInsn(IADD)
      ArithmeticOperator.Sub -> methodVisitor.visitInsn(ISUB)
      ArithmeticOperator.Mul -> methodVisitor.visitInsn(IMUL)
      ArithmeticOperator.Div -> methodVisitor.visitInsn(IDIV)
    }
  }

  override fun visit(expr: TypedExpression.NumArithmeticOperation) {
    expr.lhs.accept(this)
    if (expr.lhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(I2D)
    }

    expr.rhs.accept(this)
    if (expr.rhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(I2D)
    }

    when(expr.operator){
      ArithmeticOperator.Add -> methodVisitor.visitInsn(DADD)
      ArithmeticOperator.Sub -> methodVisitor.visitInsn(DSUB)
      ArithmeticOperator.Mul -> methodVisitor.visitInsn(DMUL)
      ArithmeticOperator.Div -> methodVisitor.visitInsn(DDIV)
    }
  }

  override fun visit(expr: TypedExpression.Concatenation) {
    val lhsType = expr.lhs.gustoType
    val rhsType = expr.rhs.gustoType

    methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder")
    methodVisitor.visitInsn(DUP)
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
    expr.lhs.accept(this)
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${lhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
    expr.rhs.accept(this)
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${rhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
  }

  override fun visit(expr: TypedExpression.FunctionCall) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.ListDeclaration) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.Function) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.ListAccess) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}