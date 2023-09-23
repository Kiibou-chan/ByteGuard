package space.kiibou.byteguard.bytecode

import org.opalj.br.PC

trait Location

case object PreCondition extends Location

case object PostCondition extends Location

case class Before(index: PC) extends Location

case class At(index: PC) extends Location

case class After(index: PC) extends Location
