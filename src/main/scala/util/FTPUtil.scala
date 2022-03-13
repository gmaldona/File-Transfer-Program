package edu.oswego.cs.gmaldona
package util

import java.io.{ File, FileInputStream, IOException }
import java.nio.ByteBuffer
import scala.util.Random
import packets.Error

object FTPUtil {

    def fileToByteArray(file: File): Array[Byte] = {
        val inputStream: FileInputStream = new FileInputStream(file);
        val buffer: Array[Byte] = new Array(file.length().asInstanceOf[Int])
        inputStream.read(buffer)
        inputStream.close()
        buffer
    }

    def ByteArrayToMaxPacketSizeArray(arr: Array[Byte]): List[Array[Byte]] = {
        var modifiedArr = arr;
        var listOfByteArrays:List[Array[Byte]] = List()
        val DATA_PACKET_METADATA = 5
        if (modifiedArr.length - DATA_PACKET_METADATA <=0) println("Please Enter a Larger Max Packet Size")
        while (modifiedArr.length > 0) {
            var subArray: Array[Byte] = Array()
            if (modifiedArr.length >= Constants.MAX_PACKET_SIZE - DATA_PACKET_METADATA)
                subArray = modifiedArr.slice(0, Constants.MAX_PACKET_SIZE - DATA_PACKET_METADATA)
            else
                subArray = modifiedArr

            listOfByteArrays = listOfByteArrays :+ subArray
            modifiedArr = modifiedArr.slice(subArray.length, modifiedArr.length)
        }
        listOfByteArrays
    }

    def extractPacket(header: ByteBuffer): Array[Byte] = {
        header.flip()
        val buffer: Array[Byte] = new Array(header.remaining())
        header.get(buffer)
        buffer
    }

    def localRemoteXORKey(localKey: Int, remoteKey: Int): Long = Long.MaxValue - (localKey + remoteKey)

    def displayError(errorPacket: Error): Unit = {
        println("Error Code:\t\t" + errorPacket.errorCode)
        println("Error Message:\t" + errorPacket.errorMessage)
        System.exit(1)
    }

}
