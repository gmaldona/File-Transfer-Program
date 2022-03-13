package edu.oswego.cs.gmaldona
package packets

import opcodes.Opcode.Opcode

trait Packet {
    var opcode: Opcode
    def getBytes: Array[Byte]
}
