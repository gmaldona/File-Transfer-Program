package edu.oswego.cs.gmaldona

import opcodes.Opcode
import packets.{ Data, FTPHeader, Packet, PacketFactory }
import service.{ Client, Server, Service }
import util.{ Constants, ErrorHandler, FTPUtil }

import java.net.{ InetSocketAddress, SocketAddress }
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import scala.util.Random
import sys.process._
import scala.language.postfixOps

object RemoteMachine {
    "clear" !

    var server: DatagramChannel = null
    val remoteKey: Int = math.abs(Random.nextInt())
    var localKey: Int = 0
    var localRemoteKey: Long = 0.toLong
    var drop = false

    def main(args: Array[String]): Unit = {
        var hasError = false
        try {

            server = DatagramChannel.open().bind(new InetSocketAddress(Constants.HOST, Constants.PORT))
            println(new InetSocketAddress(Constants.HOST, Constants.PORT))
            println("Listening for TFP Requests ...")

            var buffer: ByteBuffer = ByteBuffer.allocate(Constants.MAX_PACKET_SIZE)
            val remoteAddress: SocketAddress = server.receive(buffer)
            val receivedPacket: Packet = PacketFactory.get(FTPUtil.extractPacket(buffer))

            val receivedFTPHeader: FTPHeader = receivedPacket.asInstanceOf[FTPHeader]

            println(receivedFTPHeader)

            if (ErrorHandler.checkRequestErrors(receivedFTPHeader, remoteAddress)) hasError = true
            localKey = BigInt(receivedFTPHeader.encryptionKey).intValue
            if (localKey < 0) {
                localKey = localKey.*(-1)
                println("Dropping 1% of Packets.")
                drop = true
            }
            localRemoteKey = FTPUtil.localRemoteXORKey(localKey, remoteKey)

            val service: Service = if (receivedFTPHeader.opcode == Opcode.WRQ) Server(receivedFTPHeader.filepath, BigInt(localRemoteKey).toByteArray, drop) else Client(receivedFTPHeader.filepath, remoteAddress, BigInt(localRemoteKey).toByteArray)

            val FTPHeaderAck = Data(0, BigInt(remoteKey).toByteArray)
            buffer = ByteBuffer.wrap(FTPHeaderAck.getBytes)
            server.send(buffer, remoteAddress)


            println("Server Key: " + localRemoteKey)

            server.disconnect()
            server.close()

            service match {
                case s: Server => s.start()
                case c: Client => c.start()
            }
        } finally  {
            if (server != null) {
                //                    server.disconnect()
                //                    server.close()
            }
        }
    }


}
