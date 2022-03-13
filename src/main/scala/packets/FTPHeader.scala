package edu.oswego.cs.gmaldona
package packets

import opcodes.Opcode.Opcode

/*
                                   FTP HEADER PACKET

    |----------------|-------------------------|---------|--------------------|---------|
    |                |                         |         |                    |         |
    |     opcode     |          String         |    0    |   Encryption Key   |    0    |
    |                |                         |         |                    |         |
    |----------------|-------------------------|---------|--------------------|---------|

          2 Bytes            String              1 Byte           8 Bytes       1 Byte
 */

case class FTPHeader(_opcode: Opcode, filepath: String, encryptionKey: Array[Byte]) extends Packet {

    override var opcode: Opcode = _opcode
    override def getBytes: Array[Byte] = Array(0.toByte, opcode.id.toByte).:++(filepath.getBytes()).:+(0.toByte).:++(encryptionKey).:+(0.toByte)
}