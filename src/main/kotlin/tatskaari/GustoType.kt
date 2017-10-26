package tatskaari

interface GustoType {
  fun getJvmTypeDesc():String
}

object UnknownType: GustoType {
  override fun getJvmTypeDesc(): String {
    throw Exception("Attempted to get JVM type for an unknown type")
  }
}

sealed class PrimitiveType(val jvmTypeDef: String) : GustoType {
  object Number : PrimitiveType("D")
  object Integer : PrimitiveType("I")
  object Text : PrimitiveType("Ljava/lang/String;")
  object Boolean : PrimitiveType("Z")
  object Unit : PrimitiveType("V")

  override fun getJvmTypeDesc(): String {
    return jvmTypeDef
  }

  override fun toString(): String {
    return when(this){
      PrimitiveType.Number -> "number"
      PrimitiveType.Integer -> "integer"
      PrimitiveType.Text -> "text"
      PrimitiveType.Boolean -> "boolean"
      PrimitiveType.Unit -> "unit"
    }
  }
}

data class ListType(val type: GustoType): GustoType {
  override fun equals(other: Any?): Boolean {
    if (other is ListType) {
      if (other.type == type || type == UnknownType || other.type == UnknownType){
        return true
      }
    }
    return false
  }

  override fun getJvmTypeDesc(): String {
    return "[${type.getJvmTypeDesc()}"
  }
}
data class FunctionType(val params: List<GustoType>, val returnType: GustoType): GustoType {
  override fun getJvmTypeDesc(): String {
    //TODO make this return a class that implements this function
    return params.joinToString (separator = ";", transform = { it.getJvmTypeDesc() }, prefix = "(", postfix = ")${returnType.getJvmTypeDesc()}")
  }

  override fun toString(): String {
    return params.joinToString (separator = ", ", transform = { it.toString() }, prefix = "(", postfix = ") ${if (returnType == PrimitiveType.Unit) "" else "-> $returnType"}")
  }
}