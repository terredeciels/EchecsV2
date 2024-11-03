package perft

import board.Board
import board.Move
import perft.FenToBoard.toBoard
import perft.PerftCompare.PerftResult

object PerftSpeed {
    @JvmStatic
    fun main(args: Array<String>) {
        perftTest()
    }

    //        0	1
    //        1	20
    //        2	400
    //        3	8,902
    //        4	197,281
    //        6	119,060,324
    //        7	3,195,901,860
    //        8	84,998,978,956
    //        9	2,439,530,234,167
    //        10	69,352,859,712,417
    //        11	2,097,651,003,696,806
    private fun perftTest() {
        val resexpected = arrayOf("0", "20", "400", "8902", "197281", "4865609", "119060324")
        //voir https://www.chessprogramming.org/Perft_Results
        val f = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        val board = toBoard(f)
        val max_depth = 6
        val t0 = System.nanoTime().toDouble()
        for (depth in 1..max_depth) {
            val res = perft(Board(board!!), depth)
            val t1 = System.nanoTime().toDouble()
            println("Depth " + depth + " : " + (t1 - t0) / 1000000000 + " sec")
            println("Count = " + res.moveCount + "  /  " + resexpected[depth])
            assert(res.moveCount.toString() == resexpected[depth])
        }
    }

    private fun perft(board: Board, depth: Int): PerftResult {
        val result = PerftResult()
        if (depth == 0) {
            result.moveCount++
            return result
        }
        board.gen()
        val moves: List<Move> = board.pseudomoves
        for (move in moves) {
            if (board.makemove(move)) {
                val subPerft = perft(Board(board), depth - 1)
                board.takeback()
                result.moveCount += subPerft.moveCount
            }
        }
        return result
    }
}
