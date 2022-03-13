package edu.oswego.cs.gmaldona
package packets
import opcodes.Opcode.Opcode

import opcodes.Opcode

/*
               ACK PACKET

  |----------------|----------------|
  |                |                |
  |     opcode     |      Option    |
  |                |                |
  |----------------|----------------|

       2 Bytes           2 Byte

 */

case class OACK() extends Packet {
    override def getBytes: Array[Byte] = {
        Array(0.toByte)
    }

    override var opcode: Opcode = Opcode.OACK
}
