package edu.oswego.cs.gmaldona
package service

import packets.{ Data, Packet }

import edu.oswego.cs.gmaldona.util.Constants

import java.net.{ InetSocketAddress, SocketAddress }
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

abstract class Service {
    var remoteAddress: String
    var port: Int
    var pathLocation: String

    def isLastDataPacket(packet: Data): Boolean = if (packet.data.length < Constants.MAX_PACKET_SIZE) true else false

    def receivePacket(datagramChannel: DatagramChannel): Tuple2[Array[Byte], SocketAddress] = {
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(Constants.MAX_PACKET_SIZE)
        val address: SocketAddress = datagramChannel.receive(byteBuffer)
        byteBuffer.flip()
        val buffer: Array[Byte] = byteBuffer.array()
        Tuple2(buffer, address)
    }

}
