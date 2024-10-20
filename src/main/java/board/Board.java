package board;

import java.util.ArrayList;
import java.util.List;

public class Board extends Piece implements Constants {


    public static final int BOARD_SIZE = 64;
    public int[] color = new int[BOARD_SIZE];
    public int[] piece = new int[BOARD_SIZE];

    public Piece[] pieces = new Piece[BOARD_SIZE];
    public int side;
    public int xside;
    public int castle;
    public int ep;
    public List<Move> pseudomoves = new ArrayList<>();
    public int halfMoveClock;
    public int plyNumber;
    public String[] piece_char_light = {"P", "N", "B", "R", "Q", "K"};
    public String[] piece_char_dark = {"p", "n", "b", "r", "q", "k"};
    private int fifty;
    private UndoMove um = new UndoMove();

//    {
//        for (int c = 0; c < BOARD_SIZE; c++) {
//            pieces[c] = new Piece();
//        }
//    }

    public Board() {
        initPieces();
    }

    private void initPieces() {
        for (int c = 0; c < BOARD_SIZE; c++) {
            pieces[c] = new Piece();
        }
    }

    public Board(Board board) {
        color = board.color;
        piece = board.piece;
        side = board.side;
        xside = board.xside;
        castle = board.castle;
        ep = board.ep;
        fifty = board.fifty;
        pseudomoves = new ArrayList<>();
        um = new UndoMove();
    }

    private boolean in_check(int s) {
        for (int i = 0; i < BOARD_SIZE; ++i)
            if (piece[i] == KING && color[i] == s)
                return isAttacked(i, s ^ 1);


        return true; // shouldn't get here
    }

    private boolean isAttacked(int sqTarget, int side) {
        for (int sq = 0; sq < BOARD_SIZE; ++sq) {
            if (color[sq] == side && piece[sq] == PAWN) {
                int offset = (side == LIGHT) ? -8 : 8;
                if ((sq & 7) != 0 && sq + offset - 1 == sqTarget) return true;
                if ((sq & 7) != 7 && sq + offset + 1 == sqTarget) return true;
            } else if (color[sq] == side && piece[sq] != PAWN) {
                for (int o = 0; o < offsets[piece[sq]]; ++o) {
                    int sqIndex = sq;
                    while ((sqIndex = mailbox[mailbox64[sqIndex] + offset[piece[sq]][o]]) != -1) {
                        if (sqIndex == sqTarget) return true;
                        if (color[sqIndex] != EMPTY || !slide[piece[sq]]) break;
                    }
                }
            }
        }
        return false;
    }
    public void gen() {

        for (int c = 0; c < BOARD_SIZE; ++c) {
            if (color[c] == side) {
                if (piece[c] == PAWN) {
                    final int offset = (side == LIGHT) ? -8 : 8;
                    final int oppositeColor = side ^ 1;

                    if ((c & 7) != 0 && color[c + offset - 1] == oppositeColor) gen_push(c, c + offset - 1, 17);
                    if ((c & 7) != 7 && color[c + offset + 1] == oppositeColor) gen_push(c, c + offset + 1, 17);

                    if (color[c + offset] == EMPTY) {
                        gen_push(c, c + offset, 16);
                        if ((side == LIGHT && c >= 48) || (side == DARK && c <= 15)) {
                            if (color[c + (offset << 1)] == EMPTY) gen_push(c, c + (offset << 1), 24);
                        }
                    }
                } else {
                    gen(c);
                }
            }
        }

        /* generate castle moves */
        if (side == LIGHT) {
            if ((castle & 1) != 0) gen_push(E1, G1, 2);
            if ((castle & 2) != 0) gen_push(E1, C1, 2);
        } else {
            if ((castle & 4) != 0) gen_push(E8, G8, 2);
            if ((castle & 8) != 0) gen_push(E8, C8, 2);
        }

        /* generate en passant moves */


        if (ep != -1) {
            if (side == LIGHT) {
                if ((ep & 7) != 0 && color[ep + 7] == LIGHT && piece[ep + 7] == PAWN) gen_push(ep + 7, ep, 21);
                if ((ep & 7) != 7 && (color[ep + 9] == LIGHT && piece[ep + 9] == PAWN)) gen_push(ep + 9, ep, 21);
            } else {
                if ((ep & 7) != 0 && (color[ep - 9] == DARK && piece[ep - 9] == PAWN)) gen_push(ep - 9, ep, 21);
                if ((ep & 7) != 7 && (color[ep - 7] == DARK && piece[ep - 7] == PAWN)) gen_push(ep - 7, ep, 21);
            }

        }

    }

    private void gen(int c) {
        int p = piece[c];

        for (int d = 0; d < offsets[p]; ++d) {

            int _c = c;
            while (true) {
                _c = mailbox[mailbox64[_c] + offset[p][d]];
                if (_c == -1) break;
                if (color[_c] != EMPTY) {
                    if (color[_c] == xside) gen_push(c, _c, 1);
                    break;
                }
                gen_push(c, _c, 0);
                if (!slide[p]) break;
            }


        }
    }

//    private int fmailbox(Piece q, int _c, int d) {
//        int delta = q.dir[d];
//        return mailbox[mailbox64[_c] + delta];
//    }

    private void gen_push(int from, int to, int bits) {
        if ((bits & 16) != 0 && (side == LIGHT ? to <= H8 : to >= A1)) {
            gen_promote(from, to, bits);
            return;
        }
        pseudomoves.add(new Move((byte) from, (byte) to, (byte) 0, (byte) bits));
    }


    private void gen_promote(int from, int to, int bits) {
        for (int i = KNIGHT; i <= QUEEN; ++i)
            pseudomoves.add(new Move((byte) from, (byte) to, (byte) i, (byte) (bits | 32)));
    }

    public boolean makemove(Move m) {
        if ((m.bits & 2) != 0) {
            int from;
            int to;

            if (in_check(side)) return false;
            switch (m.to) {
                case 62:
                    if (color[F1] != EMPTY || color[G1] != EMPTY || isAttacked(F1, xside) || isAttacked(G1, xside)) {
                        return false;
                    }
                    from = H1;
                    to = F1;
                    break;
                case 58:
                    if (color[B1] != EMPTY || color[C1] != EMPTY || color[D1] != EMPTY || isAttacked(C1, xside) || isAttacked(D1, xside)) {
                        return false;
                    }
                    from = A1;
                    to = D1;
                    break;
                case 6:
                    if (color[F8] != EMPTY || color[G8] != EMPTY || isAttacked(F8, xside) || isAttacked(G8, xside)) {
                        return false;
                    }
                    from = H8;
                    to = F8;
                    break;
                case 2:
                    if (color[B8] != EMPTY || color[C8] != EMPTY || color[D8] != EMPTY || isAttacked(C8, xside) || isAttacked(D8, xside)) {
                        return false;
                    }
                    from = A8;
                    to = D8;
                    break;
                default: // shouldn't get here
                    from = -1;
                    to = -1;
                    break;
            }
            color[to] = color[from];
            piece[to] = piece[from];
            color[from] = EMPTY;
            piece[from] = EMPTY;
        }

        /* back up information, so we can take the move back later. */
        um.mov = m;
        um.capture = piece[m.to];
        um.castle = castle;
        um.ep = ep;
        um.fifty = fifty;

        castle &= castle_mask[m.from] & castle_mask[m.to];

        if ((m.bits & 8) != 0) {
            if (side == LIGHT) {
                ep = m.to + 8;
            } else {
                ep = m.to - 8;
            }
        } else {
            ep = -1;
        }
        if ((m.bits & 17) != 0) {
            fifty = 0;
        } else {
            ++fifty;
        }

        /* move the piece */
        color[m.to] = side;
        if ((m.bits & 32) != 0) {
            piece[m.to] = m.promote;
        } else {
            piece[m.to] = piece[m.from];
        }
        color[m.from] = EMPTY;
        piece[m.from] = EMPTY;

        /* erase the pawn if this is an en passant move */
        if ((m.bits & 4) != 0) {
            if (side == LIGHT) {
                color[m.to + 8] = EMPTY;
                piece[m.to + 8] = EMPTY;
            } else {
                color[m.to - 8] = EMPTY;
                piece[m.to - 8] = EMPTY;
            }
        }

        side ^= 1;
        xside ^= 1;
        if (in_check(xside)) {
            takeback();
            return false;
        }

        return true;
    }

    public void takeback() {

        side ^= 1;
        xside ^= 1;

        Move m = um.mov;
        castle = um.castle;
        ep = um.ep;
        fifty = um.fifty;

        color[m.from] = side;
        if ((m.bits & 32) != 0) {
            piece[m.from] = PAWN;
        } else {
            piece[m.from] = piece[m.to];
        }
        if (um.capture == EMPTY) {
            color[m.to] = EMPTY;
            piece[m.to] = EMPTY;
        } else {
            color[m.to] = xside;
            piece[m.to] = um.capture;
        }
        if ((m.bits & 2) != 0) {
            int from;
            int to;

            switch (m.to) {
                case 62:
                    from = F1;
                    to = H1;
                    break;
                case 58:
                    from = D1;
                    to = A1;
                    break;
                case 6:
                    from = F8;
                    to = H8;
                    break;
                case 2:
                    from = D8;
                    to = A8;
                    break;
                default: // shouldn't get here
                    from = -1;
                    to = -1;
                    break;
            }
            color[to] = side;
            piece[to] = ROOK;
            color[from] = EMPTY;
            piece[from] = EMPTY;
        }
        if ((m.bits & 4) != 0) {
            if (side == LIGHT) {
                color[m.to + 8] = xside;
                piece[m.to + 8] = PAWN;
            } else {
                color[m.to - 8] = xside;
                piece[m.to - 8] = PAWN;
            }
        }
    }

    public void print_board() {
        int i;

        System.out.print("\n8 ");
        for (i = 0; i < Board.BOARD_SIZE; ++i) {
            switch (color[i]) {
                case EMPTY:
                    System.out.print(". ");
                    break;
                case LIGHT:
                    System.out.printf(piece_char_light[piece[i]] + " ");
                    break;
                case DARK:
                    System.out.printf(piece_char_dark[piece[i]] + " ");
                    break;
            }
            if ((i + 1) % 8 == 0 && i != 63) {
                System.out.printf("\n%d ", 7 - (i >> 3));
            }
        }
        System.out.print("\n\n   a b c d e f g h\n\n");
    }
}
