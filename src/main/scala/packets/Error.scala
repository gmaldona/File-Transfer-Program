package edu.oswego.cs.gmaldona
package packets

import opcodes.Opcode.Opcode
import opcodes.{ ErrorCode, Opcode }
import opcodes.ErrorCode.ErrorCode

/*
                    ERROR PACKET

    |----------------|-------------------------|--------------------|--------|
    |                |                         |                    |        |
    |     Opcode     |        ErrorCode        |    ErrorMessage    |    0   |
    |                |                         |                    |        |
    |----------------|-------------------------|--------------------|--------|

          2 Bytes              2 Bytes                String          1 Byte
 */

case class Error(errorCode: ErrorCode, errorMessage: String) extends Packet {

    override var opcode: Opcode = Opcode.ERR
    override def getBytes: Array[Byte] = Array(0.toByte, opcode.id.toByte, 0.toByte, errorCode.id.toByte).:++(errorMessage.getBytes).:+(0)


}
