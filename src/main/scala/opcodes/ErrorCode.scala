package edu.oswego.cs.gmaldona
package opcodes

object ErrorCode extends Enumeration {
    type ErrorCode = Value

    val NOTDEF     = Value(0) // Not defined, see error message
    val FILE_NF    = Value(1) // File not found
    val BAD_ACCESS = Value(2) // Access Violation
    val DISK_ERR   = Value(3) // Disk full
    val BAD_OP     = Value(4) // Illegal TFP operation
    val UNK_ID     = Value(5) // Unknown transfer ID
    val FILE_EX    = Value(6) // File already exists
    val NO_USR     = Value(7) // No such user

    def getErrorCode(value: Int): ErrorCode.ErrorCode = {
        value match {
            case 0 => NOTDEF
            case 1 => FILE_NF
            case 2 => BAD_ACCESS
            case 3 => DISK_ERR
            case 4 => BAD_OP
            case 5 => UNK_ID
            case 6 => FILE_EX
            case 7 => NO_USR
        }
    }
}
