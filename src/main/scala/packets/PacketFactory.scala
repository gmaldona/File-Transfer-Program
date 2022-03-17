package edu.oswego.cs.gmaldona
package packets

import opcodes.{ ErrorCode, Opcode }
import Opcode.Opcode

import java.math.BigInteger

/**
 * Given an array of bytes, depending on the OPCODE of the byte array, a packet can be generated
 */
object PacketFactory {

    def get(buffer: Array[Byte]): Packet = {
        val opcode: Opcode = Opcode.getOpcode(buffer(1).toInt)

        opcode match {
            case Opcode.RRQ | Opcode.WRQ =>_generateFTPHeaderPacket(buffer)
            case Opcode.DATA => _generateDataPacket(buffer)
            case Opcode.ACK => _generateACKPacket(buffer)
            case Opcode.ERR  => _generateErrorPacket(buffer)
            case Opcode.END => _generateEndPacket(buffer)
            //case Opcode.OACK.id => _
            case _ => println(opcode); _generateDataPacket(buffer)

        }
    }

    def _generateDataPacket(buffer: Array[Byte]): Data = {
        val blockNumber = new BigInteger(buffer.slice(2, 4)).intValue()
        var dataBuffer = buffer.slice(4, buffer.length - 1)
        while (dataBuffer(dataBuffer.length - 1) == 0) {
            dataBuffer = dataBuffer.slice(0, dataBuffer.length - 1)
        }

        Data(blockNumber, dataBuffer)
    }

    def _generateEndPacket(buffer: Array[Byte]): END = {
        val blockNumber = new BigInteger(buffer.slice(2,4)).intValue()

        END(blockNumber)
    }

    def _generateACKPacket(buffer: Array[Byte]): ACK = {
        val blockNumber = new BigInteger(buffer.slice(2, 4)).intValue()

        ACK(blockNumber)
    }

    def _generateFTPHeaderPacket(buffer: Array[Byte]): FTPHeader = {
        val opcodeBuffer: Array[Byte]   = buffer.slice(0, 2)
        val filenameBuffer: Array[Byte] = _findBuffer(buffer, 2)
        val encryptionKey:  Array[Byte] = _findBuffer(buffer, 2 + filenameBuffer.length + 1)
        val FTPrequest: Opcode.Opcode = if (Opcode.WRQ.id == opcodeBuffer(1)) Opcode.WRQ else Opcode.RRQ

        FTPHeader(FTPrequest, new String(filenameBuffer), encryptionKey)
    }

    def _generateErrorPacket(buffer: Array[Byte]): Error = {
        val errorCode: ErrorCode.ErrorCode = ErrorCode.getErrorCode(BigInt(buffer.slice(2, 4)).intValue)
        val errorMessage: String = new String(_findBuffer(buffer, 4))

        Error(errorCode, errorMessage)
    }

    /** Gets the subarray from a given index to the next zero
     *
     * @param buffer The array in which a subarray shall be found
     * @param _startingIndex The index at which the subarray will starting from
     * @return Subarray from _startingIndex (including) to the next found zero (Excluding)
     * @throws ArrayIndexOutOfBoundsException If the array does not end with a zero
     */
    def _findBuffer(buffer: Array[Byte], _startingIndex: Int): Array[Byte] = {
        var index: Int = _startingIndex
        var foundBuffer: Array[Byte] = Array()
        var zeroFound = false
        while (!zeroFound) {
            if (buffer(index) == 0.toByte) { zeroFound = true }
            else {
                foundBuffer = foundBuffer :+ buffer(index)
                index = index.+(1)
            }
        }
        foundBuffer
    }

}
