package edu.oswego.cs.gmaldona
package packets

import opcodes.Opcode.Opcode

import edu.oswego.cs.gmaldona.opcodes.Opcode

/*
                         DATA PACKET

    |----------------|----------------|----------------|---------|
    |                |                |                |         |
    |     opcode     |     Block #    |      Data      |    0    |
    |                |                |                |         |
    |----------------|----------------|----------------|---------|

         2 Bytes           2 Bytes          n Bytes       1 Byte

    A data packet has a max packet size of Constants.MAX_PACKET_SIZE
 */

case class Data(blockNumber: Int, data: Array[Byte]) extends Packet {

    override var opcode: Opcode = Opcode.DATA
    override def getBytes: Array[Byte] = {
        if (blockNumber <= Byte.MaxValue) Array(0.toByte, opcode.id.toByte, 0.toByte, blockNumber.toByte).:++(data) :+ 0.toByte
        else  Array(0.toByte, opcode.id.toByte).:++(BigInt(blockNumber).toByteArray).:++(data) :+ 0.toByte
    }
}
