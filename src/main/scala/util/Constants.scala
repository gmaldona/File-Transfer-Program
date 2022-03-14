package edu.oswego.cs.gmaldona
package util

import java.time.Duration

object Constants {

    type Frame = Array[Byte]
    val MAX_PACKET_SIZE: Int = 512
    val HOST = "localhost"
    val PORT = 26923
    //val PORT = 8090
    val WINDOW_SIZE: Int = 5;
    val TIMEOUT = 1

    val DEBUG_SHOW_DL_SLIDING_WINDOW_WORKS = false



}
