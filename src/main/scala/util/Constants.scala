package edu.oswego.cs.gmaldona
package util

object Constants {

    type Frame = Array[Byte]
    val MAX_PACKET_SIZE: Int = 512
    val HOST = "pi.cs.oswego.edu"
    val PORT = 26923
    //val PORT = 8090
    val WINDOW_SIZE: Int = 5;
    val TIMEOUT = 1

    //Client side controlled
    val DEBUG_SHOW_DL_SLIDING_WINDOW_WORKS = false
    val DEBUG_SHOW_DL_XOR_WORKS = false


}
