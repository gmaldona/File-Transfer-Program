package edu.oswego.cs.gmaldona
package opcodes

object Opcode extends Enumeration {
    type Opcode = Value

    val WRQ  = Value(1)
    val RRQ  = Value(2)
    val DATA = Value(3)
    val ACK  = Value(4)
    val ERR  = Value(5)
    val OACK = Value(6)

    def getOpcode(value: Int): Opcode = value match {
        case 1 => WRQ
        case 2 => RRQ
        case 3 => DATA
        case 4 => ACK
        case 5 => ERR
        case 6 => OACK
    }
}
