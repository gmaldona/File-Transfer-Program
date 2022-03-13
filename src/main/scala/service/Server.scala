package edu.oswego.cs.gmaldona
package service
import packets.{ ACK, Data, Error, Packet, PacketFactory }

import edu.oswego.cs.gmaldona.util.Constants.Frame
import edu.oswego.cs.gmaldona.util.{ Constants, ErrorHandler, FTPUtil }

import java.net.{ InetSocketAddress, SocketAddress }
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.{ ExecutorService, Executors, ConcurrentHashMap }
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps

case class Server() extends Service {

    val executionService: ExecutorService = Executors.newFixedThreadPool(Constants.MAX_FRAMES)
    val address: SocketAddress = new InetSocketAddress(Constants.HOST, Constants.PORT)
    var datagramChannel: DatagramChannel = null
    var hasReceivedLastPacket = false
    var hasReceivedAllPacket = false
    @volatile var lastBlockNumber = 0
    @volatile var openedThreads = 0

    implicit val ec = ExecutionContext.global

    def start(): Unit = {

        println("Starting Server. Listening for File Data...")
        datagramChannel = DatagramChannel.open().bind(address)
        // not thread safe
        val dataPacketMap: ConcurrentHashMap[Int, Data] = new ConcurrentHashMap[Int, Data]()

        while (!hasReceivedAllPacket || ! hasReceivedAllPacket) {
            if (openedThreads < Constants.MAX_FRAMES) {
                new Thread(() => {
                    println("open thread")
                    val _byteBuffer = ByteBuffer.allocate(Constants.MAX_PACKET_SIZE)
                    val _address: SocketAddress = datagramChannel.receive(_byteBuffer)
                    println("Packet RECEIVED")
                    val byteBuffer = _byteBuffer
                    val address = _address
                    val buffer: Frame = parseByteBuffer(byteBuffer)
                    val dataPacket: Data = getDataPacketOrError( parseBufferIntoPacket(buffer) )
                    dataPacketMap.put(dataPacket.blockNumber, dataPacket)
                    if (dataPacket.getBytes.length == 512) sendACKPacket(dataPacket.blockNumber, address)
                    else { hasReceivedLastPacket = true; lastBlockNumber = dataPacket.blockNumber }
                } ).start()
                openedThreads = openedThreads.+(1)
            }
            if (dataPacketMap.size() == lastBlockNumber) hasReceivedAllPacket = true
        }

        dataPacketMap.forEach( (key, value) => println(key + ": " + value.getBytes.mkString("Array(", ", ", ")")) )



    }

    def runWithTimeout(timeoutMs: Long)(f: => Data) : Option[Data] = {
        Some(Await.result(Future(f), timeoutMs milliseconds))
    }

    def parseByteBuffer(byteBuffer: ByteBuffer): Frame = {
        byteBuffer.flip()
        byteBuffer.array()
    }

    def parseBufferIntoPacket(buffer: Array[Byte]): Packet = PacketFactory.get(buffer)

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


    override var remoteAddress: String = _
    override var port: Int = _
    override var pathLocation: String = _

}
