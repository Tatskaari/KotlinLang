package tatskaari.bytecodecompiler

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import tatskaari.PrimitiveType
import tatskaari.parsing.BinaryOperators
import tatskaari.parsing.TypeChecker.TypedExpression
import tatskaari.parsing.TypeChecker.TypedStatement

object Compiler {
  fun compileProgram(statements: List<TypedStatement>): ByteArray {
    val classWriter = ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS or org.objectweb.asm.ClassWriter.COMPUTE_FRAMES)
    classWriter.visit(52,ACC_PUBLIC or ACC_SUPER,"GustoMain",null,"java/lang/Object", null)
    val methodVisitor = classWriter.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null)

    statements.forEach({ compileStatement(it, methodVisitor)})

    methodVisitor.visitInsn(Opcodes.RETURN)
    methodVisitor.visitMaxs(0, 0)

    methodVisitor.visitEnd()
    classWriter.visitEnd()

    return classWriter.toByteArray()
  }

  private fun compileStatement(statement: TypedStatement, methodVisitor: MethodVisitor){
    when(statement){
      is TypedStatement.Output -> {
        // put System.out on the operand stack
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        compileExpression(statement.expression, methodVisitor)
        val type = statement.expression.gustoType
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(${type.getJvmTypeDesc()})V", false)
      }
    }
  }

  private fun compileExpression(expression: TypedExpression, methodVisitor: MethodVisitor){
    when (expression) {
      is TypedExpression.IntLiteral -> methodVisitor.visitIntInsn(Opcodes.BIPUSH, expression.expr.value)
      is TypedExpression.NumLiteral -> methodVisitor.visitLdcInsn(expression.expr.value)
      is TypedExpression.TextLiteral -> methodVisitor.visitLdcInsn(expression.expr.value)
      is TypedExpression.BinaryOperator -> compileBinaryOperator(expression, methodVisitor)
    }
  }

  private fun compileBinaryOperator(binaryOperator: TypedExpression.BinaryOperator, methodVisitor: MethodVisitor){
    val lhsType = binaryOperator.lhs.gustoType
    val rhsType = binaryOperator.rhs.gustoType

    if(binaryOperator.expr.operator == BinaryOperators.Add && (lhsType == PrimitiveType.Text || rhsType == PrimitiveType.Text)){
      methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder")
      methodVisitor.visitInsn(DUP)
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
      compileExpression(binaryOperator.lhs, methodVisitor)
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${lhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
      compileExpression(binaryOperator.rhs, methodVisitor)
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${rhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
    } else {
      val integerArithmetic = lhsType == rhsType && rhsType == PrimitiveType.Integer

      compileExpression(binaryOperator.lhs, methodVisitor)
      if (lhsType == PrimitiveType.Integer && !integerArithmetic){
        methodVisitor.visitInsn(I2D)
      }
      compileExpression(binaryOperator.rhs, methodVisitor)
      if (rhsType == PrimitiveType.Integer && !integerArithmetic){
        methodVisitor.visitInsn(I2D)
      }

      when(binaryOperator.expr.operator){
        BinaryOperators.Add -> methodVisitor.visitInsn(if (integerArithmetic) IADD else DADD)
        BinaryOperators.Sub -> methodVisitor.visitInsn(if (integerArithmetic) ISUB else DSUB)
        BinaryOperators.Mul -> methodVisitor.visitInsn(if (integerArithmetic) IMUL else DMUL)
        BinaryOperators.Div -> methodVisitor.visitInsn(if (integerArithmetic) IDIV else DDIV)
        else -> { throw Exception("Unimplemented")}
      }
    }
  }

}