package tatskaari.parsing.typechecking

import tatskaari.GustoType

object TypeComparer {
  private fun compareGenerics(genericType: GustoType.GenericType, exprType: GustoType, genericTypes: HashMap<GustoType.GenericType, GustoType>): Boolean{
    return if (genericTypes.containsKey(genericType)){
      val expectedType = genericTypes.getValue(genericType)

      if (expectedType is GustoType.GenericType){
        compareGenerics(expectedType, exprType, genericTypes)
      } else {
        exprType == expectedType
      }
    } else {
      genericTypes[genericType] = exprType
      true
    }
  }

  private fun comparePrimitiveType(expectedType: GustoType.PrimitiveType, actualType: GustoType): Boolean {
    return expectedType == actualType
  }

  private fun compareFunctionType(expectedType: GustoType.FunctionType, actualType: GustoType, genericTypes: HashMap<GustoType.GenericType, GustoType>): Boolean{
    if (actualType is GustoType.FunctionType){
      expectedType.params.zip(actualType.params).forEach{(expected, actual) ->
        if(!compareTypes(expected, actual, genericTypes)){
          return false
        }
      }
      return compareTypes(expectedType.returnType, actualType.returnType, genericTypes)
    } else {
      return false
    }
  }

  private fun compareListType(expectedType: GustoType.ListType, actualType: GustoType, genericTypes: HashMap<GustoType.GenericType, GustoType>): Boolean{
    if (actualType is GustoType.ListType){
      if (actualType.type == GustoType.UnknownType){
        return true
      }
      return compareTypes(expectedType.type, actualType.type, genericTypes)
    }
    return false
  }

  private fun compareVariantType(expectedType: GustoType.VariantType, actualType: GustoType) : Boolean{
    return if (actualType is GustoType.VariantType){
      actualType == expectedType
    } else {
      actualType is GustoType.VariantMember && expectedType.members.contains(actualType)
    }
  }

  fun compareTypes(expectedType: GustoType, actualType: GustoType, genericTypes: HashMap<GustoType.GenericType, GustoType>): Boolean{
    return when(expectedType){
      is GustoType.FunctionType -> compareFunctionType(expectedType, actualType, genericTypes)
      is GustoType.ListType -> compareListType(expectedType, actualType, genericTypes)
      is GustoType.GenericType -> compareGenerics(expectedType, actualType, genericTypes)
      is GustoType.PrimitiveType -> comparePrimitiveType(expectedType, actualType)
      GustoType.UnknownType -> return true
      is GustoType.VariantMember -> expectedType == actualType
      is GustoType.VariantType -> compareVariantType(expectedType, actualType)
      is GustoType.TupleType -> TODO("Implement tuple types")
    }
  }

  private fun expandGenerics(genericType: GustoType.GenericType, genericTypes: HashMap<GustoType.GenericType, GustoType>): GustoType {
    return if (genericTypes.containsKey(genericType)){
      genericTypes.getValue(genericType)
    } else {
      genericType
    }
  }

  fun expandFunctionType(expectedType: GustoType.FunctionType, genericTypes: HashMap<GustoType.GenericType, GustoType>): GustoType.FunctionType {
    val params = expectedType.params.map {
      expandTypes(it, genericTypes)
    }
    val returnType = expandTypes(expectedType.returnType, genericTypes)
    return GustoType.FunctionType(params, returnType)
  }

  private fun expandListType(expectedType: GustoType.ListType, genericTypes: HashMap<GustoType.GenericType, GustoType>): GustoType.ListType {
    val type = expandTypes(expectedType.type, genericTypes)
    return GustoType.ListType(type)
  }

  private fun expandTypes(expectedType: GustoType, genericTypes: HashMap<GustoType.GenericType, GustoType>): GustoType {
    return when(expectedType){
      is GustoType.FunctionType -> expandFunctionType(expectedType, genericTypes)
      is GustoType.ListType -> expandListType(expectedType, genericTypes)
      is GustoType.GenericType -> expandGenerics(expectedType, genericTypes)
      is GustoType.PrimitiveType -> expectedType
      GustoType.UnknownType -> GustoType.UnknownType
      else -> TODO("Handle type declaration")
    }
  }
}