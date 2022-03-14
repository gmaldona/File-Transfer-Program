package edu.oswego.cs.gmaldona
package service
import packets.{ ACK, Data, Error, Packet, PacketFactory }

import edu.oswego.cs.gmaldona.opcodes.Opcode
import edu.oswego.cs.gmaldona.util.Constants.Frame
import edu.oswego.cs.gmaldona.util.{ Constants, ErrorHandler, FTPUtil }

import java.net.{ InetSocketAddress, SocketAddress }
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.concurrent.{ ConcurrentHashMap, ExecutorService, Executors }
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps

case class Server() extends Service {

    val executionService: ExecutorService = Executors.newFixedThreadPool(Constants.MAX_FRAMES)
    val address: SocketAddress = new InetSocketAddress(Constants.HOST, Constants.PORT)
    var datagramChannel: DatagramChannel = null
    var hasReceivedLastPacket = false
    var hasReceivedAllPacket = false
    @volatile var openedThreads = 0
    val lastPacket: AtomicBoolean = new AtomicBoolean(false)
    val allPacket: AtomicBoolean = new AtomicBoolean(false)
    val lastBlockNumber: AtomicInteger = new AtomicInteger(0)

    implicit val ec = ExecutionContext.global

    def start(): Unit = {

        println("Starting Server. Listening for File Data...")
        datagramChannel = DatagramChannel.open().bind(address)
        // not thread safe
        val dataPacketMap: ConcurrentHashMap[Int, Data] = new ConcurrentHashMap[Int, Data]()
        var isLastPacket = false
        var receivedAllPackets = false

        while (! lastPacket.get() && !allPacket.get()) {
            try {
                runWithTimeout(1000) {
                    val byteBuffer = ByteBuffer.allocate(Constants.MAX_PACKET_SIZE)
                    val address: SocketAddress = datagramChannel.receive(byteBuffer)
                    val dataPacketHandler = ReceivedDataPacket(byteBuffer, address, dataPacketMap, lastPacket, lastBlockNumber)
                    new Thread(dataPacketHandler).start()
                }
            } catch {
                case _: Exception =>
            }
            if (lastPacket.get() && dataPacketMap.size() == lastBlockNumber.get()) { allPacket.set(true) }
        }

        println ("FINISH:")
        dataPacketMap.forEach( (key, value) => println(key + ": " + value.getBytes.mkString("Array(", ", ", ")")) )

    }

    def runWithTimeout(timeoutMs: Long)(f: => Unit) : Option[Unit] = {
        Some(Await.result(Future(f), timeoutMs milliseconds))
    }

    def getLastBlockNumber(buffer: Array[Byte]): Int = BigInt(buffer.slice(2, 4)).intValue

}

case class ReceivedDataPacket(_byteBuffer: ByteBuffer, _address: SocketAddress, _dataPacketMap: ConcurrentHashMap[Int, Data], _lastPacket: AtomicBoolean, _lastBlockNumber: AtomicInteger) extends Runnable {
    val byteBuffer = _byteBuffer.flip()
    val address = _address
    val datagramChannel: DatagramChannel = DatagramChannel.open().bind(null)
    val buffer: Array[Byte] = byteBuffer.array().asInstanceOf[Array[Byte]]

    override def run(): Unit = {
        val dataPacket: Data = getDataPacketOrError( parseBufferIntoPacket(buffer) )
        println(dataPacket.blockNumber + " : " + dataPacket.getBytes.length)
        sendACKPacket(dataPacket.blockNumber, address)
        _dataPacketMap.put(dataPacket.blockNumber, dataPacket)
        if (dataPacket.getBytes.length < Constants.MAX_PACKET_SIZE) {
            _lastPacket.set(true)
            _lastBlockNumber.set(dataPacket.blockNumber)
        }
    }

    def sendACKPacket(blockNumber: Int, address: SocketAddress): Unit = {
        val ack = ACK(blockNumber)
        val byteBuffer: ByteBuffer = ByteBuffer.wrap(ack.getBytes)
        datagramChannel.send(byteBuffer, address)
    }

    def getDataPacketOrError(packet: Packet): Data = {
        try {
            packet match {
                case data: Data   => return data
                case error: Error => ErrorHandler.handle(error); return null
                case _: Packet    => return Data(-1, Array())
            }
        } catch {
            case _: Exception => datagramChannel.close(); System.exit(1)
        }
        Data(-1, Array())
    }

    def parseBufferIntoPacket(buffer: Array[Byte]): Packet = PacketFactory.get(buffer)

    def isNotMaxSizePacket: Boolean = if (buffer.length < Constants.MAX_PACKET_SIZE) true else false
}
