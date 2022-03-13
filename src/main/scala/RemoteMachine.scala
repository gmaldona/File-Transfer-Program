package edu.oswego.cs.gmaldona

import edu.oswego.cs.gmaldona.opcodes.{ ErrorCode, Opcode }
import edu.oswego.cs.gmaldona.packets.{ Data, Error, FTPHeader, Packet, PacketFactory }
import edu.oswego.cs.gmaldona.service.{ Client, Server, Service }
import edu.oswego.cs.gmaldona.util.{ Constants, ErrorHandler, FTPUtil }

import java.net.{ InetSocketAddress, SocketAddress }
import java.nio.ByteBuffer
import java.nio.file.{ Files, Paths }
import java.nio.channels.DatagramChannel
import scala.util.Random
import scala.util.control.Breaks._

object RemoteMachine {

    var server: DatagramChannel = null
    val remoteKey: Int = math.abs(Random.nextInt())
    var localKey: Int = 0

    def main(args: Array[String]): Unit = {
        var hasError = false
        while (! hasError) {
            try {
                server = DatagramChannel.open().bind(new InetSocketAddress("localhost", Constants.PORT))


                println("Listening for TFP Requests ...")

                var buffer: ByteBuffer = ByteBuffer.allocate(Constants.MAX_PACKET_SIZE)
                val remoteAddress: SocketAddress = server.receive(buffer)
                val receivedFTPHeader: FTPHeader = PacketFactory.get(FTPUtil.extractPacket(buffer)).asInstanceOf[FTPHeader]

                println(receivedFTPHeader)

                if (ErrorHandler.checkRequestErrors(receivedFTPHeader, remoteAddress)) hasError = true

                localKey = BigInt(receivedFTPHeader.encryptionKey).intValue

                val service: Service = if (receivedFTPHeader.opcode == Opcode.WRQ) Server() else Client(receivedFTPHeader.filepath)

                val FTPHeaderAck = Data(0, BigInt(remoteKey).toByteArray)
                buffer = ByteBuffer.wrap(FTPHeaderAck.getBytes)
                server.send(buffer, remoteAddress)


                server.disconnect()
                server.close()

                service match {
                    case s: Server => s.start()
                    case c: Client => c.start()
                }

            } finally  {
                if (server != null) server.close()
            }
        }
    }




}
