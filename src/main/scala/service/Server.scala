package edu.oswego.cs.gmaldona
package service
import packets.{ ACK, Data, END, Error, Packet, PacketFactory }
import util.{ Constants, ErrorHandler, FTPUtil }

import java.io.FileOutputStream
import java.net.{ InetSocketAddress, SocketAddress, InetAddress }
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.concurrent.{ ConcurrentHashMap, ExecutorService, Executors, ThreadLocalRandom }
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps

case class Server(filepath: String, localRemoteKey: Array[Byte], drop: Boolean) extends Service {

    val executionService: ExecutorService = Executors.newFixedThreadPool(Constants.WINDOW_SIZE)
    var address: SocketAddress = null
    var datagramChannel: DatagramChannel = null
    @volatile var openedThreads = 0
    @volatile var lastPacket: AtomicBoolean = new AtomicBoolean(false)
    @volatile var allPacket: AtomicBoolean = new AtomicBoolean(false)
    @volatile var lastBlockNumber: AtomicInteger = new AtomicInteger(0)

    implicit val ec = ExecutionContext.global

    def start(): Unit = {

        val hostname: String = InetAddress.getLocalHost.getHostName
        address = new InetSocketAddress(hostname + ".cs.oswego.edu", Constants.PORT)
        try {
            datagramChannel = DatagramChannel.open().bind(address)
        } catch {
            case _: Exception => {
                address = new InetSocketAddress("localhost", Constants.PORT)
                datagramChannel = DatagramChannel.open().bind(address)
            }
        }

        val dataPacketMap: ConcurrentHashMap[Int, Data] = new ConcurrentHashMap[Int, Data]()

        while (! lastPacket.get() && !allPacket.get()) {
            try {
                runWithTimeout(1000) {
                    val byteBuffer = ByteBuffer.allocate(Constants.MAX_PACKET_SIZE)
                    val address: SocketAddress = datagramChannel.receive(byteBuffer)
                    val dataPacketHandler = ReceivedDataPacket(byteBuffer, address, dataPacketMap, lastPacket, lastBlockNumber, localRemoteKey, drop)
                    new Thread(dataPacketHandler).start()
                }
            } catch {
                case _: Exception =>
            }
            if (lastPacket.get() && dataPacketMap.size() == lastBlockNumber.get()) {
                allPacket.set(true)
            }
        }

        var byteArray: Array[Byte] = Array()

        for ( i <- 1 to lastBlockNumber.get()) {
            byteArray = byteArray :++ dataPacketMap.get(i).data
        }

        val outputStream : FileOutputStream = new FileOutputStream("ReceivedFile/" + filepath)
        outputStream.write(byteArray)

    }

    def runWithTimeout(timeoutMs: Long)(f: => Unit) : Option[Unit] = {
        Some(Await.result(Future(f), timeoutMs milliseconds))
    }

    def getLastBlockNumber(buffer: Array[Byte]): Int = BigInt(buffer.slice(2, 4)).intValue

}

case class ReceivedDataPacket(_byteBuffer: ByteBuffer, _address: SocketAddress, _dataPacketMap: ConcurrentHashMap[Int, Data], _lastPacket: AtomicBoolean, _lastBlockNumber: AtomicInteger, localRemoteKey: Array[Byte], drop: Boolean) extends Runnable {
    val byteBuffer = _byteBuffer.flip()
    val address = _address
    val datagramChannel: DatagramChannel = DatagramChannel.open().bind(null)
    val buffer: Array[Byte] = byteBuffer.array().asInstanceOf[Array[Byte]]

    override def run(): Unit = {
        if (drop) {
            val randomDrop = ThreadLocalRandom.current().nextInt(100)
            println("Dropping Packet!")
            if (randomDrop == 0) return
        }
        var dataPacket: Data = getDataPacketOrError(parseBufferIntoPacket(buffer))
        if (dataPacket.blockNumber == -1) {
            println("RECEIVED END PACKET")
            _lastPacket.set(true)
            _lastBlockNumber.set(BigInt(dataPacket.data).intValue - 1)
            return
        }
        if (! Constants.DEBUG_SHOW_DL_XOR_WORKS) dataPacket = Data(dataPacket.blockNumber, FTPUtil.XORData(dataPacket.data, localRemoteKey))

        println("Length: " + dataPacket.getBytes.length)
        sendACKPacket(dataPacket.blockNumber, address)
        _dataPacketMap.put(dataPacket.blockNumber, dataPacket)

    }

    def sendACKPacket(blockNumber: Int, address: SocketAddress): Unit = {
        val ack = ACK(blockNumber)
        val byteBuffer: ByteBuffer = ByteBuffer.wrap(ack.getBytes)
        println(ack)
        datagramChannel.send(byteBuffer, address)
    }

    def getDataPacketOrError(packet: Packet): Data = {
        try {
            packet match {
                case data: Data   => return data
                case error: Error => ErrorHandler.handle(error); return null
                case end: END    => return Data(-1, BigInt(end.blockNumber).toByteArray)
            }
        } catch {
            case _: Exception => datagramChannel.close(); System.exit(1)
        }
        Data(-1, Array())
    }

    def parseBufferIntoPacket(buffer: Array[Byte]): Packet = PacketFactory.get(buffer)

    def isNotMaxSizePacket: Boolean = if (buffer.length < Constants.MAX_PACKET_SIZE) true else false
}
