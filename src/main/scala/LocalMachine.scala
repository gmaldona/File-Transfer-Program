package edu.oswego.cs.gmaldona

import service.{ Client, Server, Service }

import edu.oswego.cs.gmaldona.opcodes.Opcode
import edu.oswego.cs.gmaldona.packets.{ Data, Error, FTPHeader, PacketFactory }
import edu.oswego.cs.gmaldona.util.{ Constants, FTPUtil }

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import scala.util.Random

object LocalMachine {

    var service: Option[Service] = None
    var localKey: Int = math.abs(Random.nextInt())
    var filePath: String = ""
    var host: String = ""
    var remoteKey: Int = 0
    var localRemoteKey: Long = 0
    val address = new InetSocketAddress(host, Constants.PORT)

    val client: DatagramChannel = DatagramChannel.open().bind(null)

    def main(args: Array[String]): Unit = {
        var _args: Array[String] = args
        if (_args.length >= 2) {
            if (_args(_args.length - 1).contains(".edu")) {
                filePath = _args(_args.length - 2)
                host = _args(_args.length - 1)
            } else {
                filePath = _args(_args.length - 1)
                host = _args(_args.length - 2)
            }
        }

        if (_args.length == 0) _args = Array("File-Transfer-Program", "4.4:4", "testData/test.txt")
        if (_args.length < 2) argumentError()
        if (_args.length == 2) service = Some(serviceFactory(_args.slice(1, 3)))
        if (_args.length > 2) parseOptions()

        if (_args(_args.length - 1).contains(".edu")) sendFTPHeaderPacket(Opcode.RRQ, filePath, localKey)
        else sendFTPHeaderPacket(Opcode.WRQ, filePath, localKey)

//        sendFTPHeaderPacket(Opcode.RRQ, filePath, localKey)
        val buffer: Array[Byte] = awaitFTPHeaderACKPacket()

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
                case _: Server => Server(filePath, BigInt(localRemoteKey).toByteArray).start()
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
    def serviceFactory(args: Array[String]): Service = if (args(0).contains(":")) Server(filePath, BigInt(localRemoteKey).toByteArray) else Client(filePath, address, BigInt(localRemoteKey).toByteArray)
    def parseOptions(): Unit = {}
    def parseFTPHeaderACK(packet: Data): Unit = remoteKey = BigInt(packet.data).intValue
}
