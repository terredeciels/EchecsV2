package board

/**
 * 1	capture 2	castle 4	en passant capture 8	pushing a pawn 2 squares 16	pawn
 * move 32	promote
 */
class Move {
    var from: Byte = 0
    var to: Byte = 0
    var promote: Byte = 0
    var bits: Byte = 0

    internal constructor()

    internal constructor(from: Byte, to: Byte, promote: Byte, bits: Byte) {
        this.from = from
        this.to = to
        this.promote = promote
        this.bits = bits
    }
}
