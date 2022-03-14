package edu.oswego.cs.gmaldona
package util

import RemoteMachine.server
import opcodes.{ ErrorCode, Opcode }
import packets.{ Error, FTPHeader, Packet }

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.file.{ Files, Paths }

/**
 * Handler for Error Packets
 */
object ErrorHandler {

    /**
     * Error Handler for initial FTPHeader RRQ/WRQ Request from Client
     * @param receivedFTPHeader Received initial FTPHeader packet request
     * @param remoteAddress Address from which the initial FTPHeader packet request came from
     * @return Whether the FTPHeader packet was request
     */
    def checkRequestErrors(receivedFTPHeader: FTPHeader, remoteAddress: SocketAddress): Boolean = {

        var errorAck: Packet = null
        var hasError: Boolean = false

        if (receivedFTPHeader.opcode == Opcode.RRQ && !Files.exists(Paths.get(receivedFTPHeader.filepath))) {
            errorAck = Error(ErrorCode.FILE_NF, "Found not found on remote machine.")
            hasError = true
        }

        else if (receivedFTPHeader.opcode == Opcode.WRQ && Files.exists(Paths.get("ReceivedFile/" + receivedFTPHeader.filepath))) {
            errorAck = Error(ErrorCode.FILE_EX, "File already exists.")
            hasError = true
        }

        if (hasError) {
            val buffer: ByteBuffer = ByteBuffer.wrap(errorAck.getBytes)
            server.send(buffer, remoteAddress)
            server.close()
        }
        hasError
    }

    /**
     * Error handler for deconstructing a received Error Packet
     * @param errorPacket Error Packet that will be deconstructed
     * @throws Exception Exception Error
     */
    @throws[Exception]
    def handle(errorPacket: Error): Unit = {
        println("Error Code:\t\t" + errorPacket.errorCode)
        println("Error Message:\t" + errorPacket.errorMessage)
        println("Closing Connection.")
        throw new Exception(errorPacket.errorMessage)
    }



}
