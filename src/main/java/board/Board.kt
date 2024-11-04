package board

import board.Constants.Companion.A1
import board.Constants.Companion.A8
import board.Constants.Companion.B1
import board.Constants.Companion.B8
import board.Constants.Companion.C1
import board.Constants.Companion.C8
import board.Constants.Companion.D1
import board.Constants.Companion.D8
import board.Constants.Companion.DARK
import board.Constants.Companion.E1
import board.Constants.Companion.E8
import board.Constants.Companion.EMPTY
import board.Constants.Companion.F1
import board.Constants.Companion.F8
import board.Constants.Companion.G1
import board.Constants.Companion.G8
import board.Constants.Companion.H1
import board.Constants.Companion.H8
import board.Constants.Companion.KING
import board.Constants.Companion.KNIGHT
import board.Constants.Companion.LIGHT
import board.Constants.Companion.PAWN
import board.Constants.Companion.QUEEN
import board.Constants.Companion.ROOK
import board.Constants.Companion.castle_mask
import board.Constants.Companion.mailbox
import board.Constants.Companion.mailbox64
import board.Constants.Companion.offset
import board.Constants.Companion.offsets
import board.Constants.Companion.slide
import java.lang.System.arraycopy
import java.util.stream.IntStream.range

class Piece

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

    internal constructor(f: Byte, t: Byte, p: Byte, b: Byte) {
        from = f
        to = t
        promote = p
        bits = b
    }
}

class UndoMove {
    var mov: Move = Move()
    var capture: Int = 0
    var castle: Int = 0
    var ep: Int = 0
    var fifty: Int = 0 //public int hash;
}

class Board : Constants {

    var color = IntArray(BOARD_SIZE)
    var piece = IntArray(BOARD_SIZE)
    var pieces = arrayOfNulls<Piece>(BOARD_SIZE)
    var side = 0
    var xside = 0
    var castle = 0
    var ep = 0
    var pseudomoves: MutableList<Move> = ArrayList()
    var halfMoveClock = 0
    var plyNumber = 0
    private var fifty = 0
    private var um = UndoMove()


    constructor() {
        initPieces()
    }

    constructor(board: Board) {
        arraycopy(board.color, 0, color, 0, BOARD_SIZE)
        arraycopy(board.piece, 0, piece, 0, BOARD_SIZE)
        side = board.side
        xside = board.xside
        castle = board.castle
        ep = board.ep
        fifty = board.fifty
        pseudomoves = ArrayList()
        um = UndoMove()
    }

    fun initPieces() {
        (0 until BOARD_SIZE).forEach { c -> pieces[c] = Piece() }
    }

    fun isAttacked(sqTarget: Int, side: Int): Boolean {
        return range(0, BOARD_SIZE)
            .filter { sq: Int -> color[sq] == side }
            .anyMatch { sq: Int -> isAttackedByPiece(sq, sqTarget, piece[sq], side) }
    }

    fun isAttackedByPiece(sq: Int, sqTarget: Int, pieceType: Int, side: Int): Boolean {
        return when (pieceType) {
            PAWN -> isPawnAttacked(sq, sqTarget, side)
            else -> range(0, offsets[pieceType])
                .anyMatch { isAttackedByOffset(sq, sqTarget, pieceType, it) }
        }
    }

    fun isPawnAttacked(sq: Int, sqTarget: Int, side: Int): Boolean {
        val offset = if (side == LIGHT) -8 else 8
        return (sq and 7) != 0 && sq + offset - 1 == sqTarget ||
                (sq and 7) != 7 && sq + offset + 1 == sqTarget
    }

    fun isAttackedByOffset(sq: Int, sqTarget: Int, pieceType: Int, offsetIndex: Int): Boolean {
        var sqIndex = sq
        while (mailbox[mailbox64[sqIndex] + offset[pieceType][offsetIndex]].also { sqIndex = it } != -1) {
            if (sqIndex == sqTarget) return true
            if (color[sqIndex] != EMPTY || !slide[pieceType]) break
        }
        return false
    }

    fun gen() {
        range(0, BOARD_SIZE)
            .filter { c: Int -> color[c] == side }
            .forEach { c: Int ->
                if (piece[c] == PAWN) genPawn(c)
                else gen(c)
            }
        genCastles()
        genEnpassant()
    }

    fun genPawn(c: Int) {
        val offset = if (side == LIGHT) -8 else 8
        val oppositeColor = side xor 1

        if ((c and 7) != 0 && color[c + offset - 1] == oppositeColor) genPush(c, c + offset - 1, 17)
        if ((c and 7) != 7 && color[c + offset + 1] == oppositeColor) genPush(c, c + offset + 1, 17)

        if (color[c + offset] == EMPTY) {
            genPush(c, c + offset, 16)
            if (side == LIGHT && c >= 48 || (side == DARK && c <= 15)) if (color[c + (offset shl 1)] == EMPTY) genPush(
                c, c + (offset shl 1), 24
            )
        }
    }

    fun genEnpassant() {
        when {
            ep != -1 -> {
                val offsets = if (side == LIGHT) listOf(7, 9) else listOf(-9, -7)
                val targetColor = if (side == LIGHT) LIGHT else DARK
                offsets.forEach { offset ->
                    val newEp = ep + offset
                    if (ep and 7 != (if (offset == offsets[0]) 0 else 7))
                        if (color[newEp] == targetColor && piece[newEp] == PAWN) genPush(newEp, ep, 21)
                }
            }
        }


//        if (ep != -1) {
//            if (side == LIGHT) {
//                if ((ep and 7) != 0 && color[ep + 7] == LIGHT && piece[ep + 7] == PAWN) genPush(ep + 7, ep, 21)
//                if ((ep and 7) != 7 && (color[ep + 9] == LIGHT && piece[ep + 9] == PAWN)) genPush(ep + 9, ep, 21)
//            } else {
//                if ((ep and 7) != 0 && (color[ep - 9] == DARK && piece[ep - 9] == PAWN)) genPush(ep - 9, ep, 21)
//                if ((ep and 7) != 7 && (color[ep - 7] == DARK && piece[ep - 7] == PAWN)) genPush(ep - 7, ep, 21)
//            }
//        }
    }

     fun genCastles() {
        if (side == LIGHT) {
            if ((castle and 1) != 0) genPush(E1, G1, 2)
            if ((castle and 2) != 0) genPush(E1, C1, 2)
        } else {
            if ((castle and 4) != 0) genPush(E8, G8, 2)
            if ((castle and 8) != 0) genPush(E8, C8, 2)
        }
    }

     fun gen(c: Int) {
        val p = piece[c]

        for (d in 0 until offsets[p]) {
            var to = c
            while (true) {
                to = mailbox[mailbox64[to] + offset[p][d]]
                if (to == -1) break
                if (color[to] != EMPTY) {
                    if (color[to] == xside) genPush(c, to, 1)
                    break
                }
                genPush(c, to, 0)
                if (!slide[p]) break
            }
        }
    }


    private fun genPush(from: Int, to: Int, bits: Int) {
        if ((bits and 16) != 0 && (if (side == LIGHT) to <= H8 else to >= A1)) {
            genPromote(from, to, bits)
            return
        }
        pseudomoves.add(Move(from.toByte(), to.toByte(), 0.toByte(), bits.toByte()))
    }


    private fun genPromote(from: Int, to: Int, bits: Int) {
        for (i in KNIGHT..QUEEN) pseudomoves.add(
            Move(
                from.toByte(),
                to.toByte(),
                i.toByte(),
                (bits or 32).toByte()
            )
        )
    }

    fun makemove(m: Move): Boolean {
        if ((m.bits.toInt() and 2) != 0) {
            val from: Int
            val to: Int

            if (inCheck(this, side)) return false
            when (m.to) {
                62.toByte() -> {
                    if (color[F1] != EMPTY || color[G1] != EMPTY || isAttacked(
                            F1, xside
                        ) || isAttacked(G1, xside)
                    ) {
                        return false
                    }
                    from = H1
                    to = F1
                }

                58.toByte() -> {
                    if (color[B1] != EMPTY || color[C1] != EMPTY || color[D1] != EMPTY || isAttacked(
                            C1, xside
                        ) || isAttacked(D1, xside)
                    ) {
                        return false
                    }
                    from = A1
                    to = D1
                }

                6.toByte() -> {
                    if (color[F8] != EMPTY || color[G8] != EMPTY || isAttacked(
                            F8, xside
                        ) || isAttacked(G8, xside)
                    ) {
                        return false
                    }
                    from = H8
                    to = F8
                }

                2.toByte() -> {
                    if (color[B8] != EMPTY || color[C8] != EMPTY || color[D8] != EMPTY || isAttacked(
                            C8, xside
                        ) || isAttacked(D8, xside)
                    ) {
                        return false
                    }
                    from = A8
                    to = D8
                }

                else -> {
                    from = -1
                    to = -1
                }
            }
            color[to] = color[from]
            piece[to] = piece[from]
            color[from] = EMPTY
            piece[from] = EMPTY
        }

        /* back up information, so we can take the move back later. */
        um.mov = m
        um.capture = piece[m.to.toInt()]
        um.castle = castle
        um.ep = ep
        um.fifty = fifty

        castle = castle and (castle_mask[m.from.toInt()] and castle_mask[m.to.toInt()])

        ep = if ((m.bits.toInt() and 8) != 0) if (side == LIGHT) m.to + 8 else m.to - 8
        else -1

        fifty = if ((m.bits.toInt() and 17) != 0) 0 else fifty + 1

        /* move the piece */
        color[m.to.toInt()] = side
        piece[m.to.toInt()] = if ((m.bits.toInt() and 32) != 0) m.promote.toInt() else piece[m.from.toInt()]
        color[m.from.toInt()] = EMPTY
        piece[m.from.toInt()] = EMPTY

        /* erase the pawn if this is an en passant move */
        if ((m.bits.toInt() and 4) != 0) {
            val offset = if ((side == LIGHT)) 8 else -8
            piece[m.to + offset] = EMPTY
            color[m.to + offset] = piece[m.to + offset]
        }

        side = side xor 1
        xside = xside xor 1
        if (inCheck(this, xside)) {
            takeback()
            return false
        }

        return true
    }

    fun takeback() {
        side = side xor 1
        xside = xside xor 1

        val m = um.mov
        castle = um.castle
        ep = um.ep
        fifty = um.fifty

        color[m.from.toInt()] = side
        if ((m.bits.toInt() and 32) != 0) {
            piece[m.from.toInt()] = PAWN
        } else {
            piece[m.from.toInt()] = piece[m.to.toInt()]
        }
        if (um.capture == EMPTY) {
            color[m.to.toInt()] = EMPTY
            piece[m.to.toInt()] = EMPTY
        } else {
            color[m.to.toInt()] = xside
            piece[m.to.toInt()] = um.capture
        }
        if ((m.bits.toInt() and 2) != 0) {
            val from: Int
            val to: Int

            when (m.to) {
                62.toByte() -> {
                    from = F1
                    to = H1
                }

                58.toByte() -> {
                    from = D1
                    to = A1
                }

                6.toByte() -> {
                    from = F8
                    to = H8
                }

                2.toByte() -> {
                    from = D8
                    to = A8
                }

                else -> {
                    from = -1
                    to = -1
                }
            }
            color[to] = side
            piece[to] = ROOK
            color[from] = EMPTY
            piece[from] = EMPTY
        }
        if ((m.bits.toInt() and 4) != 0) {
            if (side == LIGHT) {
                color[m.to + 8] = xside
                piece[m.to + 8] = PAWN
            } else {
                color[m.to - 8] = xside
                piece[m.to - 8] = PAWN
            }
        }
    }

    companion object {
        const val BOARD_SIZE: Int = 64
    }

    private fun inCheck(board: Board, s: Int): Boolean {
        return range(0, BOARD_SIZE)
            .filter { i: Int -> board.piece[i] == KING && board.color[i] == s }
            .anyMatch { i: Int -> board.isAttacked(i, s xor 1) }
    }
}
