package tools;

import tscp.Board;
import tscp.Constants;

public class FenToBoard implements Constants {

    private static Board board;

    public static Board toBoard(String fen) {
        board = new Board();
        initFromFEN(fen, true);
        return board;
    }

    private static void initFromFEN(String fen, boolean strict) throws IllegalArgumentException {
        // pos.clear(); // TODO ?
        int index = 0;
        char ch;
        /*========== 1st field : pieces ==========*/
        int row = 7;
        int col = 0;
        while (index < fen.length() && fen.charAt(index) != ' ') {
            ch = fen.charAt(index);
            if (ch == '/') {
                if (col != 8) {
                    throw new IllegalArgumentException("Malformatted fen string: unexpected '/' found at index " + index);
                }
                row--;
                col = 0;
            } else if (ch >= '1' && ch <= '8') {
                int num = (int) (ch - '0');
                if (col + num > 8) {
                    throw new IllegalArgumentException("Malformatted fen string: too many pieces in rank at index " + index + ": " + ch);
                }
                for (int j = 0; j < num; j++) {
                    // int _case = coorToSqi(col, row);
                    setStone(col, row, NO_STONE);

                    col++;
                }
            } else {
                int stone = fenCharToStone(ch);
                if (stone == NO_STONE) {
                    throw new IllegalArgumentException("Malformatted fen string: illegal piece char: " + ch);
                }
                //  int _case = coorToSqi(col, row);
                setStone(col, row, stone);
                col++;
            }
            index++;
        }
        if (row != 0 || col != 8) {
            throw new IllegalArgumentException("Malformatted fen string: missing pieces at index: " + index);
        }
        /*========== 2nd field : to play ==========*/
        if (index + 1 < fen.length() && fen.charAt(index) == ' ') {
            ch = fen.charAt(index + 1);
            switch (ch) {
                case 'w':
                    setToPlay(LIGHT);
                    break;
                case 'b':
                    setToPlay(DARK);
                    break;
                default:
                    throw new IllegalArgumentException("Malformatted fen string: expected 'to play' as second field but found " + ch);
            }
            index += 2;
        }
        /*========== 3rd field : castles ==========*/
        if (index + 1 < fen.length() && fen.charAt(index) == ' ') {
            index++;
            int castles = NO_CASTLES;
            if (fen.charAt(index) == '-') {
                index++;
            } else {
                int last = -1;
                while (index < fen.length() && fen.charAt(index) != ' ') {
                    ch = fen.charAt(index);
                    if (ch == 'K') {
                        castles |= WHITE_SHORT_CASTLE;
                        last = 0;
                    } else if (ch == 'Q' && (!strict || last < 1)) {
                        castles |= WHITE_LONG_CASTLE;
                        last = 1;
                    } else if (ch == 'k' && (!strict || last < 2)) {
                        castles |= BLACK_SHORT_CASTLE;
                        last = 2;
                    } else if (ch == 'q' && (!strict || last < 3)) {
                        castles |= BLACK_LONG_CASTLE;
                        last = 3;
                    } else {
                        throw new IllegalArgumentException("Malformatted fen string: illegal castles identifier or sequence " + ch);
                    }
                    index++;
                }
            }
            board.castle = (castles & 1) == 1 ? 2 : 0;
            board.castle += (castles & 2) == 2 ? 1 : 0;
            board.castle += (castles & 4) == 4 ? 8 : 0;
            board.castle += (castles & 8) == 8 ? 4 : 0;

        } else {
            throw new IllegalArgumentException("Malformatted fen string: expected castles at index " + index);
        }
        /*========== 4th field : ep square ==========*/
        if (index + 1 < fen.length() && fen.charAt(index) == ' ') {
            index++;
            int sqiEP = NO_SQUARE;
            if (fen.charAt(index) == '-') {
                index++;
            } else if (index + 2 < fen.length()) {
                sqiEP = strToSqi(fen.substring(index, index + 2));
                index += 2;
            }
            board.ep = sqiEP;
        } else {
            throw new IllegalArgumentException("Malformatted fen string: expected ep square at index " + index);
        }
        /*========== 5th field : half move clock ==========*/
        if (index + 1 < fen.length() && fen.charAt(index) == ' ') {
            index++;
            int start = index;
            while (index < fen.length() && fen.charAt(index) != ' ') {
                index++;
            }
            board.halfMoveClock = Integer.parseInt(fen.substring(start, index));
        } else {
            throw new IllegalArgumentException("Malformatted fen string: expected half move clock at index " + index);
        }
        /*========== 6th field : full move number ==========*/
        if (index + 1 < fen.length() && fen.charAt(index) == ' ') {
            if (board.side == LIGHT) {
                setPlyNumber(2 * (Integer.parseInt(fen.substring(index + 1)) - 1));
            } else {
                setPlyNumber(2 * (Integer.parseInt(fen.substring(index + 1)) - 1) + 1);
            }
        } else {
            throw new IllegalArgumentException("Malformatted fen string: expected ply number at index " + index);
        }

        /*========== now check the produced position ==========*/
        // @TODO 
//        try {
//            pos.validate();
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new IllegalArgumentException("Malformatted fen string: " + e.getMessage());
//        }
    }

    static void setPlyNumber(int plyNumber) {
        board.plyNumber = plyNumber;
    }

    static void setToPlay(int side) {
        board.side = side;
        board.xside = board.side == LIGHT ? DARK : LIGHT;
    }

    static void setStone(int j, int i, int stone) {
        int _case = 56 - 8 * i + j;
        board.piece[_case]
                = abs(stone) == 0 ? 6
                : abs(stone) == 6 ? 5
                : abs(stone) == 5 ? 0 : abs(stone);
        board.color[_case]
                = stone < 0 ? LIGHT : stone > 0 ? DARK : EMPTY;
    }

    static int abs(int x) {
        return x < 0 ? -x : x;
    }

    public static final int fenCharToStone(char ch) {
        for (int stone = MIN_STONE; stone <= MAX_STONE; stone++) {
            if (fenChars[stone - MIN_STONE] == ch) {
                return stone;
            }
        }
        return NO_STONE;
    }

    public static final int strToSqi(String s) {
        if (s == null || s.length() != 2) {
            return NO_SQUARE;
        }
        int col = charToCol(s.charAt(0));
        if (col == NO_COL) {
            return NO_SQUARE;
        }
        int row = charToRow(s.charAt(1));
        if (row == NO_ROW) {
            return NO_SQUARE;
        }
        return row * 8 + col;

    }

    public static final int charToCol(char ch) {
        if ((ch >= 'a') && (ch <= 'h')) {
            return (int) (ch - 'a');
        } else {
            return NO_COL;
        }
    }

    public static final int charToRow(char ch) {
        if ((ch >= '1') && (ch <= '8')) {
            return (int) (ch - '1');
        } else {
            return NO_ROW;
        }
    }

    public static final char stoneToFenChar(int stone) {
        if (stone >= MIN_STONE && stone <= MAX_STONE) {
            return fenChars[stone - MIN_STONE];
        } else {
            return '?';
        }
    }

    /**
     * @TODO cf ChessPresso
     */
//    public static String getFEN(PositionB pos) {}
}
