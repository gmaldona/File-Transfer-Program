package edu.oswego.cs.gmaldona
package service
import packets.{ ACK, Data, Error, Packet, PacketFactory }

import edu.oswego.cs.gmaldona.opcodes.Opcode
import edu.oswego.cs.gmaldona.util.{ Constants, ErrorHandler, FTPUtil }
import edu.oswego.cs.gmaldona.util.Constants.Frame

import java.util.ArrayList
import java.io.File
import java.net.{ InetSocketAddress, SocketAddress }
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util
import java.util.concurrent.{ ConcurrentHashMap, ExecutorService, Executors }
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps

case class Client(filepath: String) extends Service {

    val executionService: ExecutorService = Executors.newFixedThreadPool(Constants.MAX_FRAMES)
    var window: ConcurrentHashMap[Integer, Thread] = new ConcurrentHashMap[Integer, Thread]();
    val file: File = new File(filepath)
    val fileBytes: Array[Byte] = FTPUtil.fileToByteArray(file)
    val fileBytesInFrames: List[Frame] = List().++( FTPUtil.ByteArrayToMaxPacketSizeArray(fileBytes))
    val datagramChannel: DatagramChannel = DatagramChannel.open().bind(null)

    implicit val ec = ExecutionContext.global

    def start(): Unit = {
        println("Starting Client. Sending File Data...")
        var hasExited = false
        var windowStartIndex = 0;
        var windowEndIndex = 0;
        var blockNumber = 1;
        while(blockNumber <= fileBytesInFrames.size - 1) {
            executionService.submit(ClientMessager(blockNumber, fileBytesInFrames(blockNumber)))
            blockNumber = blockNumber.+(1)
        }

        while (true) {}
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
                    val receivedPacket: Packet = runWithTimeout(2000 + (dataPacket.blockNumber) * 10) {parseBufferIntoPacket(receivePacket(datagramChannel)._1)}.get
                    val ack: ACK = getAckPacketOrError(receivedPacket)
                    println(blockNumber + " : " + ack)
                    if (ack.blockNumber == blockNumber) hasReceivedACK = true
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




