package edu.oswego.cs.gmaldona
package packets

import opcodes.Opcode.Opcode

import edu.oswego.cs.gmaldona.opcodes.Opcode

/*
               ACK PACKET

  |----------------|----------------|
  |                |                |
  |     opcode     |     Block #    |
  |                |                |
  |----------------|----------------|

       2 Bytes           2 Byte

 */

case class ACK(blockNumber: Int) extends Packet {

    override var opcode: Opcode = Opcode.ACK
    override def getBytes: Array[Byte] = {
        if (blockNumber <= Byte.MaxValue) Array(0.toByte, opcode.id.toByte, 0.toByte, blockNumber.toByte)
        else Array(0.toByte, opcode.id.toByte).:++(BigInt(blockNumber).toByteArray)
    }

}
