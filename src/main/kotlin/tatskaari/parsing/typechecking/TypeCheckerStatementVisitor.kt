package tatskaari.parsing.typechecking

import tatskaari.*
import tatskaari.parsing.IStatementVisitor
import tatskaari.parsing.Statement
import tatskaari.GustoType.*

class TypeCheckerStatementVisitor(val env: TypeEnv, val typeErrors: Errors, val expectedReturnType: GustoType?) : IStatementVisitor<TypedStatement> {
  override fun visit(typeDeclaration: Statement.TypeDeclaration): TypedStatement {
    val members = typeDeclaration.members.associate { Pair(it.name, it.toGustoType(env.types)) }
    val variantType = VariantType(typeDeclaration.identifier.name, members.values.toList())

    env.types.putAll(members)
    env.types[typeDeclaration.identifier.name] = variantType

    return TypedStatement.TypeDeclaration(typeDeclaration, variantType)
  }


  private val exprVisitor = TypeCheckerExpressionVisitor(env, typeErrors)

  override fun visit(statement: Statement.ExpressionStatement): TypedStatement {
    val expr = statement.expression.accept(exprVisitor)
    return TypedStatement.ExpressionStatement(statement, expr)
  }

  override fun visit(statement: Statement.ValDeclaration): TypedStatement {
    val expression = statement.expression.accept(exprVisitor)
    val expressionType = expression.gustoType
    val variableType = statement.type.toGustoType(env.types)
    if (expressionType != UnknownType){
      when(variableType){
        GustoType.UnknownType -> env[statement.identifier.name] = expressionType
        else -> {
          if (!TypeComparer.compareTypes(variableType, expressionType, HashMap())) {
            typeErrors.addTypeMissmatch(statement, expressionType, statement.type.toGustoType(HashMap()))
          }
          env[statement.identifier.name] = variableType
        }
      }
    }


    return TypedStatement.ValDeclaration(statement, expression)
  }

  override fun visit(statement: Statement.CodeBlock): TypedStatement {
    val blockStatementVisitor = TypeCheckerStatementVisitor(TypeEnv(env), typeErrors, expectedReturnType)

    var returnType: GustoType? = null
    val body = ArrayList<TypedStatement>()
    statement.statementList.forEach {
      val typedStatement = it.accept(blockStatementVisitor)
      body.add(typedStatement)
      if (returnType == null) {
        returnType = typedStatement.returnType
      }
    }

    return TypedStatement.CodeBlock(statement, body, returnType)
  }

  override fun visit(statement: Statement.Assignment): TypedStatement {
    val expression = statement.expression.accept(exprVisitor)
    val expectedType = env.getValue(statement.identifier.name)
    if (!TypeComparer.compareTypes(expectedType, expression.gustoType, HashMap())) {
      typeErrors.addTypeMissmatch(statement, expectedType, expression.gustoType)
    }
    return TypedStatement.Assignment(statement, expression)
  }

  override fun visit(statement: Statement.ListAssignment): TypedStatement {
    val listType = env.getValue(statement.identifier.name)
    val listExpr = statement.expression.accept(exprVisitor)
    val indexExpr = statement.indexExpression.accept(exprVisitor)

    if (listType is ListType){
      val expressionType = listExpr.gustoType
      val indexType = indexExpr.gustoType
      if (expressionType != listType.type){
        typeErrors.addTypeMissmatch(statement, listType.type, expressionType)

      }
      if (indexType != PrimitiveType.Integer){
        typeErrors.addTypeMissmatch(statement, PrimitiveType.Integer, indexType)

      }
    } else {
      //TODO make this error message better
      typeErrors.add(statement, "Expected list, found $listType")
    }
    return TypedStatement.ListAssignment(statement, indexExpr, listExpr)
  }

  override fun visit(statement: Statement.If): TypedStatement {
    val typedConditionExpr = statement.condition.accept(exprVisitor)
    if (typedConditionExpr.gustoType != PrimitiveType.Boolean) {
      typeErrors.addTypeMissmatch(statement, PrimitiveType.Boolean, typedConditionExpr.gustoType)
    }
    val typedBody = statement.body.accept(this) as TypedStatement.CodeBlock
    return TypedStatement.If(statement, typedBody , typedConditionExpr)
  }

  override fun visit(statement: Statement.IfElse): TypedStatement {
    val typedCondition = statement.condition.accept(exprVisitor)
    val conditionType = typedCondition.gustoType
    if (conditionType != PrimitiveType.Boolean) {
      typeErrors.addTypeMissmatch(statement, PrimitiveType.Boolean, conditionType)
    }
    val typedIfBody = statement.ifBody.accept(this) as TypedStatement.CodeBlock
    val typedElseBody = statement.elseBody.accept(this) as TypedStatement.CodeBlock
    return TypedStatement.IfElse(statement, typedIfBody, typedElseBody, typedCondition)
  }

  override fun visit(statement: Statement.Input): TypedStatement {
    env.put(statement.identifier.name, PrimitiveType.Text)
    return TypedStatement.Input(statement)
  }

  override fun visit(statement: Statement.Output): TypedStatement {
    return TypedStatement.Output(statement, statement.expression.accept(exprVisitor))
  }

  override fun visit(statement: Statement.While): TypedStatement {
    val typedConditionExpr = statement.condition.accept(exprVisitor)
    if (typedConditionExpr.gustoType != PrimitiveType.Boolean) {
      typeErrors.addTypeMissmatch(statement, PrimitiveType.Boolean, typedConditionExpr.gustoType)
    }
    val bodyStatement = statement.body.accept(this)
    return TypedStatement.While(statement, bodyStatement, typedConditionExpr)
  }

  override fun visit(statement: Statement.FunctionDeclaration): TypedStatement {
    //TODO pass in type definition map
    val functionEnv = TypeEnv(env)
    functionEnv.putAll(statement.function.paramTypes.mapKeys { it.key.name }.mapValues { it.value.toGustoType(HashMap()) })

    val functionType = FunctionType(statement.function.params.map { statement.function.paramTypes.getValue(it).toGustoType(HashMap()) }, statement.function.returnType.toGustoType(HashMap()))

    functionEnv[statement.identifier.name] = functionType
    val body = statement.function.body.accept(TypeCheckerStatementVisitor(functionEnv, typeErrors, functionType.returnType)) as TypedStatement.CodeBlock

    env[statement.identifier.name] = functionType

    if (body.body.isEmpty() && functionType.returnType != PrimitiveType.Unit){
      typeErrors.add(statement, "Missing return")
    }

    ReturnTypeChecker(typeErrors).codeblock(body, functionType.returnType != PrimitiveType.Unit)

    return TypedStatement.FunctionDeclaration(statement, body, functionType)
  }

  override fun visit(statement: Statement.Return): TypedStatement {
    val expr= statement.expression.accept(exprVisitor)
    if (expectedReturnType != expr.gustoType){
      typeErrors.add(statement.expression, "Expected return type is $expectedReturnType however the body of the function returns ${expr.gustoType}")
    }
    return TypedStatement.Return(statement, expr)
  }
}