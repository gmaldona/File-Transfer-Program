package edu.oswego.cs.gmaldona
package service

import packets.Data

import util.Constants

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

abstract class Service {

    def isLastDataPacket(packet: Data): Boolean = if (packet.data.length < Constants.MAX_PACKET_SIZE) true else false

    def receivePacket(datagramChannel: DatagramChannel): (Array[Byte], SocketAddress) = {
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(Constants.MAX_PACKET_SIZE)
        val address: SocketAddress = datagramChannel.receive(byteBuffer)
        byteBuffer.flip()
        val buffer: Array[Byte] = byteBuffer.array()
        (buffer, address)
    }

}
