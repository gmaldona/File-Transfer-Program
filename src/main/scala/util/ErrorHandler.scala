package edu.oswego.cs.gmaldona
package util

import RemoteMachine.server
import opcodes.{ ErrorCode, Opcode }
import packets.{ Error, FTPHeader, Packet }

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.file.{ Files, Paths }

object ErrorHandler {

    def checkRequestErrors(receivedFTPHeader: FTPHeader, remoteAddress: SocketAddress): Boolean = {

        var buffer: ByteBuffer = ByteBuffer.allocate(0)
        var errorAck: Packet = null
        var hasError: Boolean = false

        if (receivedFTPHeader.opcode == Opcode.RRQ && !Files.exists(Paths.get(receivedFTPHeader.filepath))) {
            errorAck = Error(ErrorCode.FILE_NF, "Found not found on remote machine.")
            hasError = true
        }

        else if (receivedFTPHeader.opcode == Opcode.WRQ && Files.exists(Paths.get(receivedFTPHeader.filepath))) {
            errorAck = Error(ErrorCode.FILE_EX, "File already exists.")
            hasError = true
        }

        if (hasError) {
            buffer = ByteBuffer.wrap(errorAck.getBytes)
            server.send(buffer, remoteAddress)
            server.close()
        }
        hasError
    }

    @throws[Exception]
    def handle(errorPacket: Error): Error = {
        println("Error Code:\t\t" + errorPacket.errorCode)
        println("Error Message:\t" + errorPacket.errorMessage)
        println("Closing Connection.")
        throw new Exception(errorPacket.errorMessage)
    }



}
