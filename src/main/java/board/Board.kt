package board

import java.util.stream.IntStream

class Board : Constants {
    @JvmField
    var color: IntArray = IntArray(BOARD_SIZE)
    @JvmField
    var piece: IntArray = IntArray(BOARD_SIZE)

    var pieces: Array<Piece?> = arrayOfNulls(BOARD_SIZE)
    @JvmField
    var side: Int = 0
    @JvmField
    var xside: Int = 0
    @JvmField
    var castle: Int = 0
    @JvmField
    var ep: Int = 0
    @JvmField
    var pseudomoves: MutableList<Move> = ArrayList()
    @JvmField
    var halfMoveClock: Int = 0
    @JvmField
    var plyNumber: Int = 0
    private var fifty = 0
    private var um = UndoMove()


    constructor() {
        initPieces()
    }

    constructor(board: Board) {
        System.arraycopy(board.color, 0, color, 0, BOARD_SIZE)
        System.arraycopy(board.piece, 0, piece, 0, BOARD_SIZE)
        side = board.side
        xside = board.xside
        castle = board.castle
        ep = board.ep
        fifty = board.fifty
        pseudomoves = ArrayList()
        um = UndoMove()
    }

    private fun initPieces() {
        for (c in 0 until BOARD_SIZE) {
            pieces[c] = Piece()
        }
    }

    private fun in_check(s: Int): Boolean {
        return IntStream.range(0, BOARD_SIZE)
            .filter { i: Int -> piece[i] == Constants.KING && color[i] == s }
            .anyMatch { i: Int -> isAttacked(i, s xor 1) }
    }

    private fun isAttacked(sqTarget: Int, side: Int): Boolean {
        return IntStream.range(0, BOARD_SIZE)
            .filter { sq: Int -> color[sq] == side }
            .anyMatch { sq: Int -> isAttackedByPiece(sq, sqTarget, piece[sq], side) }
    }

    private fun isAttackedByPiece(sq: Int, sqTarget: Int, pieceType: Int, side: Int): Boolean {
        return when (pieceType) {
            Constants.PAWN -> isPawnAttacked(sq, sqTarget, side)
            else -> IntStream.range(0, Constants.offsets[pieceType])
                .anyMatch { isAttackedByOffset(sq, sqTarget, pieceType, it) }
        }
    }

    private fun isPawnAttacked(sq: Int, sqTarget: Int, side: Int): Boolean {
        val offset = if (side == Constants.LIGHT) -8 else 8
        return (sq and 7) != 0 && sq + offset - 1 == sqTarget ||
                (sq and 7) != 7 && sq + offset + 1 == sqTarget
    }

    private fun isAttackedByOffset(sq: Int, sqTarget: Int, pieceType: Int, offsetIndex: Int): Boolean {
        var sqIndex = sq
        while ((Constants.mailbox[Constants.mailbox64[sqIndex] + Constants.offset[pieceType][offsetIndex]].also {
                sqIndex = it
            }) != -1) {
            if (sqIndex == sqTarget) return true
            if (color[sqIndex] != Constants.EMPTY || !Constants.slide[pieceType]) break
        }
        return false
    }


    /**
     * Generates all moves for the current board position.
     *
     *
     * This function iterates over all pieces on the board, and for each piece,
     * it either calls [.gen_pawn] if the piece is a pawn, or
     * [.gen] otherwise.  Then, it calls
     * [.gen_castles] and [.gen_enpassant].
     */
    fun gen() {
        IntStream.range(0, BOARD_SIZE)
            .filter { c: Int -> color[c] == side }
            .forEach { c: Int ->
                if (piece[c] == Constants.PAWN) gen_pawn(c)
                else gen(c)
            }
        gen_castles()
        gen_enpassant()
    }

    private fun gen_pawn(c: Int) {
        val offset = if ((side == Constants.LIGHT)) -8 else 8
        val oppositeColor = side xor 1

        if ((c and 7) != 0 && color[c + offset - 1] == oppositeColor) gen_push(c, c + offset - 1, 17)
        if ((c and 7) != 7 && color[c + offset + 1] == oppositeColor) gen_push(c, c + offset + 1, 17)

        if (color[c + offset] == Constants.EMPTY) {
            gen_push(c, c + offset, 16)
            if ((side == Constants.LIGHT && c >= 48) || (side == Constants.DARK && c <= 15)) if (color[c + (offset shl 1)] == Constants.EMPTY) gen_push(
                c,
                c + (offset shl 1),
                24
            )
        }
    }

    private fun gen_enpassant() {
        if (ep != -1) {
            if (side == Constants.LIGHT) {
                if ((ep and 7) != 0 && color[ep + 7] == Constants.LIGHT && piece[ep + 7] == Constants.PAWN) gen_push(
                    ep + 7,
                    ep,
                    21
                )
                if ((ep and 7) != 7 && (color[ep + 9] == Constants.LIGHT && piece[ep + 9] == Constants.PAWN)) gen_push(
                    ep + 9,
                    ep,
                    21
                )
            } else {
                if ((ep and 7) != 0 && (color[ep - 9] == Constants.DARK && piece[ep - 9] == Constants.PAWN)) gen_push(
                    ep - 9,
                    ep,
                    21
                )
                if ((ep and 7) != 7 && (color[ep - 7] == Constants.DARK && piece[ep - 7] == Constants.PAWN)) gen_push(
                    ep - 7,
                    ep,
                    21
                )
            }
        }
    }

    private fun gen_castles() {
        if (side == Constants.LIGHT) {
            if ((castle and 1) != 0) gen_push(Constants.E1, Constants.G1, 2)
            if ((castle and 2) != 0) gen_push(Constants.E1, Constants.C1, 2)
        } else {
            if ((castle and 4) != 0) gen_push(Constants.E8, Constants.G8, 2)
            if ((castle and 8) != 0) gen_push(Constants.E8, Constants.C8, 2)
        }
    }

    private fun gen(c: Int) {
        val p = piece[c]

        for (d in 0 until Constants.offsets[p]) {
            var _c = c
            while (true) {
                _c = Constants.mailbox[Constants.mailbox64[_c] + Constants.offset[p][d]]
                if (_c == -1) break
                if (color[_c] != Constants.EMPTY) {
                    if (color[_c] == xside) gen_push(c, _c, 1)
                    break
                }
                gen_push(c, _c, 0)
                if (!Constants.slide[p]) break
            }
        }
    }


    private fun gen_push(from: Int, to: Int, bits: Int) {
        if ((bits and 16) != 0 && (if (side == Constants.LIGHT) to <= Constants.H8 else to >= Constants.A1)) {
            gen_promote(from, to, bits)
            return
        }
        pseudomoves.add(Move(from.toByte(), to.toByte(), 0.toByte(), bits.toByte()))
    }


    private fun gen_promote(from: Int, to: Int, bits: Int) {
        for (i in Constants.KNIGHT..Constants.QUEEN) pseudomoves.add(
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

            if (in_check(side)) return false
            when (m.to) {
                62.toByte() -> {
                    if (color[Constants.F1] != Constants.EMPTY || color[Constants.G1] != Constants.EMPTY || isAttacked(
                            Constants.F1, xside
                        ) || isAttacked(Constants.G1, xside)
                    ) {
                        return false
                    }
                    from = Constants.H1
                    to = Constants.F1
                }

                58.toByte() -> {
                    if (color[Constants.B1] != Constants.EMPTY || color[Constants.C1] != Constants.EMPTY || color[Constants.D1] != Constants.EMPTY || isAttacked(
                            Constants.C1, xside
                        ) || isAttacked(Constants.D1, xside)
                    ) {
                        return false
                    }
                    from = Constants.A1
                    to = Constants.D1
                }

                6.toByte() -> {
                    if (color[Constants.F8] != Constants.EMPTY || color[Constants.G8] != Constants.EMPTY || isAttacked(
                            Constants.F8, xside
                        ) || isAttacked(Constants.G8, xside)
                    ) {
                        return false
                    }
                    from = Constants.H8
                    to = Constants.F8
                }

                2.toByte() -> {
                    if (color[Constants.B8] != Constants.EMPTY || color[Constants.C8] != Constants.EMPTY || color[Constants.D8] != Constants.EMPTY || isAttacked(
                            Constants.C8, xside
                        ) || isAttacked(Constants.D8, xside)
                    ) {
                        return false
                    }
                    from = Constants.A8
                    to = Constants.D8
                }

                else -> {
                    from = -1
                    to = -1
                }
            }
            color[to] = color[from]
            piece[to] = piece[from]
            color[from] = Constants.EMPTY
            piece[from] = Constants.EMPTY
        }

        /* back up information, so we can take the move back later. */
        um.mov = m
        um.capture = piece[m.to.toInt()]
        um.castle = castle
        um.ep = ep
        um.fifty = fifty

        castle = castle and (Constants.castle_mask[m.from.toInt()] and Constants.castle_mask[m.to.toInt()])

        ep = if ((m.bits.toInt() and 8) != 0) if (side == Constants.LIGHT) m.to + 8 else m.to - 8
        else -1

        fifty = if ((m.bits.toInt() and 17) != 0) 0 else fifty + 1

        /* move the piece */
        color[m.to.toInt()] = side
        piece[m.to.toInt()] = if ((m.bits.toInt() and 32) != 0) m.promote.toInt() else piece[m.from.toInt()]
        color[m.from.toInt()] = Constants.EMPTY
        piece[m.from.toInt()] = Constants.EMPTY

        /* erase the pawn if this is an en passant move */
        if ((m.bits.toInt() and 4) != 0) {
            val offset = if ((side == Constants.LIGHT)) 8 else -8
            piece[m.to + offset] = Constants.EMPTY
            color[m.to + offset] = piece[m.to + offset]
        }

        side = side xor 1
        xside = xside xor 1
        if (in_check(xside)) {
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
            piece[m.from.toInt()] = Constants.PAWN
        } else {
            piece[m.from.toInt()] = piece[m.to.toInt()]
        }
        if (um.capture == Constants.EMPTY) {
            color[m.to.toInt()] = Constants.EMPTY
            piece[m.to.toInt()] = Constants.EMPTY
        } else {
            color[m.to.toInt()] = xside
            piece[m.to.toInt()] = um.capture
        }
        if ((m.bits.toInt() and 2) != 0) {
            val from: Int
            val to: Int

            when (m.to) {
                62.toByte() -> {
                    from = Constants.F1
                    to = Constants.H1
                }

                58.toByte() -> {
                    from = Constants.D1
                    to = Constants.A1
                }

                6.toByte() -> {
                    from = Constants.F8
                    to = Constants.H8
                }

                2.toByte() -> {
                    from = Constants.D8
                    to = Constants.A8
                }

                else -> {
                    from = -1
                    to = -1
                }
            }
            color[to] = side
            piece[to] = Constants.ROOK
            color[from] = Constants.EMPTY
            piece[from] = Constants.EMPTY
        }
        if ((m.bits.toInt() and 4) != 0) {
            if (side == Constants.LIGHT) {
                color[m.to + 8] = xside
                piece[m.to + 8] = Constants.PAWN
            } else {
                color[m.to - 8] = xside
                piece[m.to - 8] = Constants.PAWN
            }
        }
    }

    companion object {
        const val BOARD_SIZE: Int = 64
    }
}
