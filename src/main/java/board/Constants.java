package board;

public interface Constants {

    String START_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    char[] fenChars = {'K', 'P', 'Q', 'R', 'B', 'N', '-', 'n', 'b', 'r', 'q', 'p', 'k'};
    int NUM_OF_SQUARES = 64;
    int NO_CASTLES = 0,
            WHITE_LONG_CASTLE = 1,
            WHITE_SHORT_CASTLE = 2,
            BLACK_LONG_CASTLE = 4,
            BLACK_SHORT_CASTLE = 8;
    short MIN_STONE = -6, MAX_STONE = 6, NO_STONE = 0;
    int NO_COL = -1, NO_ROW = -1, NO_SQUARE = -1;
    int LIGHT = 0;
    int DARK = 1;

    int A1 = 56;
    int B1 = 57;
    int C1 = 58;
    int D1 = 59;
    int E1 = 60;
    int F1 = 61;
    int G1 = 62;
    int H1 = 63;
    int A8 = 0;
    int B8 = 1;
    int C8 = 2;
    int D8 = 3;
    int E8 = 4;
    int F8 = 5;
    int G8 = 6;
    int H8 = 7;

    int[] mailbox = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, -1, -1, 8, 9, 10, 11, 12, 13, 14, 15, -1, -1, 16, 17, 18, 19, 20, 21, 22, 23, -1, -1, 24, 25, 26, 27, 28, 29, 30, 31, -1, -1, 32, 33, 34, 35, 36, 37, 38, 39, -1, -1, 40, 41, 42, 43, 44, 45, 46, 47, -1, -1, 48, 49, 50, 51, 52, 53, 54, 55, -1, -1, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    int[] mailbox64 = {21, 22, 23, 24, 25, 26, 27, 28, 31, 32, 33, 34, 35, 36, 37, 38, 41, 42, 43, 44, 45, 46, 47, 48, 51, 52, 53, 54, 55, 56, 57, 58, 61, 62, 63, 64, 65, 66, 67, 68, 71, 72, 73, 74, 75, 76, 77, 78, 81, 82, 83, 84, 85, 86, 87, 88, 91, 92, 93, 94, 95, 96, 97, 98};

    Piece Cavalier = new Piece(1,LIGHT,8, new int[]{-21, -19, -12, -8, 8, 12, 19, 21},false);
//
//    class Pion extends Piece{
//        int pion = 0;
//        int offsets = 0;
//        int[] offset = {0, 0, 0, 0, 0, 0, 0, 0};
//        boolean slide = false;
//
//        public Pion(int code, int offsets, int[] offset, boolean slide) {
//            super(code, offsets, offset, slide);
//        }
//    }
//    class Cavalier extends Piece{
//        int cavalier = 1;
//        int[] offset = {-21, -19, -12, -8, 8, 12, 19, 21};
//        int offsets = 8 ; //= offset.length;
//        boolean slide = false;
//
//        public Cavalier(int code, int offsets, int[] offset, boolean slide) {
//            super(code, offsets, offset, slide);
//        }
//    }
//
//
    int PAWN = 0, KNIGHT = 1, BISHOP = 2, ROOK = 3, QUEEN = 4, KING = 5;
    int EMPTY = 6;
    boolean[] slide = {false, false, true, true, true, false};
    int[] offsets = {0, 8, 4, 4, 8, 8};
    int[] P = {0, 0, 0, 0, 0, 0, 0, 0};
    int[] C = {-21, -19, -12, -8, 8, 12, 19, 21};
    int[] F = {-11, -9, 9, 11, 0, 0, 0, 0};
    int[] T = {-10, -1, 1, 10, 0, 0, 0, 0};
    int[] D = {-11, -10, -9, -1, 1, 9, 10, 11};
    int[] R = {-11, -10, -9, -1, 1, 9, 10, 11};

    int[][] offset = {P, C, F, T, D, R};


    int[] castle_mask = {7, 15, 15, 15, 3, 15, 15, 11, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 13, 15, 15, 15, 12, 15, 15, 14};

}
