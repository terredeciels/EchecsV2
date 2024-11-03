package perft;

import board.Board;
import board.Move;
import perft.PerftCompare.PerftResult;

import java.util.List;

public class PerftSpeed {

    public static void main(String[] args) {
        perftTest();
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
    private static void perftTest() {
        String[] resexpected = new String[]{"0", "20", "400", "8902", "197281", "4865609", "119060324"};
        //voir https://www.chessprogramming.org/Perft_Results
       String f = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        Board board = FenToBoard.toBoard(f);
        int max_depth = 6;
        double t0 = System.nanoTime();
        for (int depth = 1; depth <= max_depth; depth++) {
            PerftResult res = perft(new Board(board), depth);
            double t1 = System.nanoTime();
            System.out.println("Depth " + depth + " : " + (t1 - t0) / 1000000000 + " sec");
            System.out.println("Count = " + res.moveCount + "  /  " + resexpected[depth]);
            assert (Double.toString(res.moveCount).equals(resexpected[depth]));
        }

    }

    private static PerftResult perft(Board board, int depth) {

        PerftResult result = new PerftResult();
        if (depth == 0) {
            result.moveCount++;
            return result;
        }
        board.gen();
        List<Move> moves = board.pseudomoves;
        for (Move move : moves) {
            if (board.makemove(move)) {
                PerftResult subPerft = perft(new Board(board), depth - 1);
                board.takeback();
                result.moveCount += subPerft.moveCount;
            }
        }
        return result;
    }
}
