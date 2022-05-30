package perft;

import tscp.Board;
import tscp.Constants;
import tscp.Move;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import tools.FenToBoard;

public class PerftCompare implements Constants {

    public static void main(String[] args) throws IOException {
        int maxDepth = 4;
        FileReader fileReader = new FileReader("C:\\Users\\gille\\IdeaProjects\\TSCPJv1.5\\src\\main\\java\\perft\\perftsuite.epd");
        BufferedReader reader = new BufferedReader(fileReader);
        String line;
        int passes = 0;
        int fails = 0;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(";");
            if (parts.length >= 3) {
                String fen = parts[0].trim();
                for (int i = 1; i < parts.length; i++) {
                    if (i > maxDepth) {
                        break;
                    }
                    String entry = parts[i].trim();
                    String[] entryParts = entry.split(" ");
                    int perftResult = Integer.parseInt(entryParts[1]);

                    Board board = FenToBoard.toBoard(fen);

                    PerftResult result = Perft.perft(board, i);
                    if (perftResult == result.moveCount) {
                        passes++;
                        System.out.println("PASS: " + fen + ". Moves " + result.moveCount + ", depth " + i);
                    } else {
                        fails++;
                        System.out.println("FAIL: " + fen + ". Moves " + result.moveCount + ", depth " + i);
                        break;
                    }
                }
            }
        }

        System.out.println("Passed: " + passes);
        System.out.println("Failed: " + fails);
    }

    static class PerftResult {

        public long timeTaken = 0;
        long moveCount = 0;

    }

    private static class Perft {

        static PerftResult perft(Board board, int depth) {

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
}
