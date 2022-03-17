package edu.oswego.cs.gmaldona

import service.{ Client, Server, Service }
import opcodes.Opcode
import packets.{ Data, Error, FTPHeader, PacketFactory }
import util.{ Constants, FTPUtil }

import edu.oswego.cs.gmaldona.LocalMachine.filePath

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Random
import sys.process._
import scala.language.postfixOps

object LocalMachine {

    "clear" !

    var service: Option[Service] = None
    var localKey: Int = math.abs(Random.nextInt())
    var filePath: String = ""
    var host: String = ""
    var drop: Boolean = false
    var remoteKey: Int = 0
    var localRemoteKey: Long = 0
    var address: InetSocketAddress = null

    val client: DatagramChannel = DatagramChannel.open().bind(null)
    val ec = ExecutionContext.global

    def runWithTimeout(timeoutMs: Long)(f: => Unit) : Option[Unit] = {
        Some(Await.result(Future(f), timeoutMs milliseconds))
    }

    def main(args: Array[String]): Unit = {
        println(args.mkString("Array(", ", ", ")"))

        var _args: Array[String] = args
        if (_args.length >= 2) {
            if (_args(0).equals("TRUE")) {
                drop = java.lang.Boolean.parseBoolean(_args(0))
                println("Dropping 1% of Packets.")
            }
            if (_args(_args.length - 1).contains(".edu") || _args(_args.length - 1).equals("localhost")) {
                filePath = _args(_args.length - 2)
                host = _args(_args.length - 1)
            } else {
                filePath = _args(_args.length - 1)
                host = _args(_args.length - 2)
            }
        }
        address = new InetSocketAddress(host, Constants.PORT)
        if (_args.length < 2) argumentError()
        if (_args.length > 2) service = Some(serviceFactory(_args.slice(1, 3)))
        println(localKey)
        if (drop) localKey = localKey.*(-1)
        var receivedFTPAck = false
        var buffer: Array[Byte] = Array()
        while (! receivedFTPAck) {
            if (_args(_args.length - 1).contains(".edu") || _args(_args.length -1).equals("localhost")) sendFTPHeaderPacket(Opcode.WRQ, filePath, localKey)
            else sendFTPHeaderPacket(Opcode.RRQ, filePath, localKey)
            try {
                runWithTimeout(300) {
                    buffer = awaitFTPHeaderACKPacket()
                }
                receivedFTPAck = true
            } catch {
                case e: Exception =>
            }
        }
        if (drop) localKey=localKey.*(-1)

        val FTPHeaderACK = PacketFactory.get(buffer)
        println(FTPHeaderACK)

        FTPHeaderACK.opcode match {
            case Opcode.DATA => parseFTPHeaderACK(FTPHeaderACK.asInstanceOf[Data])
            case Opcode.ERR  => FTPUtil.displayError(FTPHeaderACK.asInstanceOf[Error])
        }
        localRemoteKey = FTPUtil.localRemoteXORKey(localKey, remoteKey)
        println("Client Key: " + BigInt(localRemoteKey))
        client.close()
        service match {
            case Some(s) => s match {
                case _: Client => Client(filePath, address, BigInt(localRemoteKey).toByteArray).start();
                case _: Server => Server(filePath, BigInt(localRemoteKey).toByteArray, drop).start()
            }
            case None => println("Service Error. Try Again.")
        }

    }

    def sendFTPHeaderPacket(request: Opcode.Opcode, filePath: String, localKey: BigInt): Unit = {
        val ftpHeader = FTPHeader(request, filePath, localKey.toByteArray)
        println(ftpHeader.getBytes.mkString("Array(", ", ", ")"))
        val byteBuffer: ByteBuffer = ByteBuffer.wrap(ftpHeader.getBytes)

        client.send(byteBuffer, address)
    }

    def awaitFTPHeaderACKPacket(): Array[Byte] = {
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(Constants.MAX_PACKET_SIZE)
        client.receive(byteBuffer)
        byteBuffer.flip()
        val buffer: Array[Byte] = new Array(byteBuffer.remaining())
        byteBuffer.get(buffer)
        buffer
    }

    def argumentError(): Unit = { println("Invalid arguments... Exiting."); System.exit(1) }

    /** Service Factory:
     *
     *  If the first argument contains a port number then the first argument is a server address and the second argument is a path so a Client is returned
     *  If the second argument contains a port number then the first argument isc a path and the second argument is a server address so a Server is returned
     *
     *  @param args Arguments to for the service
     *  @return Service
     */
    def serviceFactory(args: Array[String]): Service =
        if (args(1).contains(".edu") || args(1).equals("localhost")) {
            Client(filePath, address, BigInt(localRemoteKey).toByteArray)
        }
        else Server(filePath, BigInt(localRemoteKey).toByteArray, drop)
    def parseOptions(): Unit = {}
    def parseFTPHeaderACK(packet: Data): Unit = remoteKey = BigInt(packet.data).intValue
}
