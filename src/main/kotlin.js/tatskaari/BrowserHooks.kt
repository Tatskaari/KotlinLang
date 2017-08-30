package tatskaari

import tatskaari.eval.*
import tatskaari.parsing.Parser
import tatskaari.parsing.TypeChecker
import kotlin.browser.window
external fun error(text: String)
object BrowserHooks {

  @JsName("eval")
  fun eval(program: String){
    try {
      val parser = Parser()
      val eval = Eval(JSHookInputProvider, JSHookOutputProvider)
      val ast = parser.parse(program)
      val typeChecker = TypeChecker()

      if (ast != null){
        typeChecker.checkStatementListTypes(ast, HashMap())
        if (typeChecker.typeMismatches.isEmpty()){
          eval.eval(ast, MutEnv())?.value.toString()
        } else {
          typeChecker.typeMismatches.forEach{
            error(it.message!!)
          }
        }
      } else {
        parser.parserExceptions.forEach{
          error(it.reason)
        }
      }
    } catch (e: Throwable){
      error("Runtime error: " + e.toString())
    }
  }

}