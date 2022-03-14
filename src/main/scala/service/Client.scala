package edu.oswego.cs.gmaldona
package service
import packets.{ ACK, Data, Error, Packet, PacketFactory }

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections
import edu.oswego.cs.gmaldona.opcodes.Opcode
import edu.oswego.cs.gmaldona.util.{ Constants, ErrorHandler, FTPUtil }
import edu.oswego.cs.gmaldona.util.Constants.Frame

import java.io.File
import java.net.{ InetSocketAddress, SocketAddress }
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.{ ConcurrentHashMap, ExecutorService, Executors }
import java.util.stream.Collectors
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps

case class Client(filepath: String) extends Service {

    val executionService: ExecutorService = Executors.newFixedThreadPool(Constants.WINDOW_SIZE)
    var window: ConcurrentHashMap[Integer, Thread] = new ConcurrentHashMap[Integer, Thread]();
    val file: File = new File(filepath)
    val fileBytes: Array[Byte] = FTPUtil.fileToByteArray(file)
    val fileBytesInFrames: List[Frame] = List().++( FTPUtil.ByteArrayToMaxPacketSizeArray(fileBytes))
    val datagramChannel: DatagramChannel = DatagramChannel.open().bind(null)

    val ackMap: ConcurrentHashMap[Int, ACK] = new ConcurrentHashMap[Int, ACK]()

    implicit val ec = ExecutionContext.global

    def start(): Unit = {
        println("Starting Client. Sending File Data...")
        var isDone = false
        var windowStartIndex = 0;
        var windowEndIndex = 0;
        var blockNumber = 1;

        while (! isDone) {
            if (ackMap.size() < Constants.WINDOW_SIZE ) {
                executionService.submit(ClientMessager(blockNumber, fileBytesInFrames(blockNumber)))
                ackMap.put(blockNumber, ACK(-1))
                windowEndIndex = blockNumber
                blockNumber = blockNumber.+(1)
            }
            else {
                if (blockNumber == fileBytesInFrames.length) {
                    isDone = true
                }
                else if (ackMap.get(windowStartIndex + 1).blockNumber != -1 && blockNumber < fileBytesInFrames.length) {
                    windowStartIndex = windowStartIndex.+(1)
                    executionService.submit(ClientMessager(blockNumber, fileBytesInFrames(blockNumber)))
                    windowEndIndex = blockNumber
                    ackMap.put(blockNumber, ACK(-1))
                    blockNumber = blockNumber.+(1)
                }
            }
        }

        while (ackMap.values().stream().filter( ack => ack.blockNumber == -1).count() > 0) {}

    }

    def parseBufferIntoPacket(buffer: Array[Byte]): Packet = PacketFactory.get(buffer)

    def getAckPacketOrError(packet: Packet): ACK = {
        try {
            packet match {
                case ack: ACK   => return ack
                case error: Error => ErrorHandler.handle(error); return null
            }
        } catch {
            case _: Exception => datagramChannel.close(); System.exit(1)
        }
        ACK(-1)
    }


    def runWithTimeout(timeoutMs: Long)(f: => Packet) : Option[Packet] = {
        Some(Await.result(Future(f), timeoutMs milliseconds))
    }

    case class ClientMessager(blockNumber: Int, frame: Frame) extends Runnable {

        val address: SocketAddress = new InetSocketAddress(Constants.HOST, Constants.PORT)

        override def run(): Unit = {
            val dataPacket = Data(blockNumber, frame)
            var hasReceivedACK = false
            while (! hasReceivedACK) {
                sendPacket(dataPacket, address)
                try {
                    val receivedPacket: Packet = runWithTimeout(2000) {parseBufferIntoPacket(receivePacket(datagramChannel)._1)}.get
                    val ack: ACK = getAckPacketOrError(receivedPacket)
                    println(blockNumber + " : " + ack)
                    if (ack.blockNumber > 0) {
                        ackMap.put(ack.blockNumber, ack)
                    }
                    if (ackMap.get(blockNumber).blockNumber != -1) {
                        hasReceivedACK = true
                        if (! Constants.DEBUG_SHOW_DL_SLIDING_WINDOW_WORKS) ackMap.put(blockNumber, ack)
                    }
                } catch {
                    case _: Exception =>
                }
            }

        }

        def sendPacket(packet: Packet, address: SocketAddress): Unit = {
            val byteBuffer: ByteBuffer = ByteBuffer.wrap(packet.getBytes)
            datagramChannel.send(byteBuffer, address)
        }

    }

}




