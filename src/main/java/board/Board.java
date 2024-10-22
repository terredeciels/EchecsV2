package board;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.arraycopy;
import static java.util.stream.IntStream.range;

public class Board implements Constants {


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
    private int fifty;
    private UndoMove um = new UndoMove();


    public Board() {
        initPieces();
    }

    public Board(Board board) {
        arraycopy(board.color, 0, color, 0, BOARD_SIZE);
        arraycopy(board.piece, 0, piece, 0, BOARD_SIZE);
        side = board.side;
        xside = board.xside;
        castle = board.castle;
        ep = board.ep;
        fifty = board.fifty;
        pseudomoves = new ArrayList<>();
        um = new UndoMove();
    }

    private void initPieces() {
        for (int c = 0; c < BOARD_SIZE; c++) {
            pieces[c] = new Piece();
        }
    }

    private boolean in_check(int s) {
        return range(0, BOARD_SIZE)
                .filter(i -> piece[i] == KING && color[i] == s)
                .anyMatch(i -> isAttacked(i, s ^ 1));
    }

    private boolean isAttacked(int sqTarget, int side) {
        return range(0, BOARD_SIZE)
                .filter(sq -> color[sq] == side)
                .anyMatch(sq -> isAttackedByPiece(sq, sqTarget, piece[sq], side));
    }

    private boolean isAttackedByPiece(int sq, int sqTarget, int pieceType, int side) {
        if (pieceType == PAWN) {
            int offset = (side == LIGHT) ? -8 : 8;
            return (sq & 7) != 0 && sq + offset - 1 == sqTarget ||
                    (sq & 7) != 7 && sq + offset + 1 == sqTarget;
        } else return range(0, offsets[pieceType])
                .anyMatch(o -> isAttackedByOffset(sq, sqTarget, pieceType, o));
    }

    private boolean isAttackedByOffset(int sq, int sqTarget, int pieceType, int offsetIndex) {
        int sqIndex = sq;
        while ((sqIndex = mailbox[mailbox64[sqIndex] + offset[pieceType][offsetIndex]]) != -1) {
            if (sqIndex == sqTarget) return true;
            if (color[sqIndex] != EMPTY || !slide[pieceType]) break;
        }
        return false;
    }


    public void gen() {

        range(0, BOARD_SIZE)
                .filter(c -> color[c] == side)
                .forEach(c -> {
                    if (piece[c] == PAWN) gen_pawn(c);
                    else gen(c);
                });
        gen_castles();

        gen_enpassant();

    }

    private void gen_pawn(int c) {
        final int offset = (side == LIGHT) ? -8 : 8;
        final int oppositeColor = side ^ 1;

        if ((c & 7) != 0 && color[c + offset - 1] == oppositeColor) gen_push(c, c + offset - 1, 17);
        if ((c & 7) != 7 && color[c + offset + 1] == oppositeColor) gen_push(c, c + offset + 1, 17);

        if (color[c + offset] == EMPTY) {
            gen_push(c, c + offset, 16);
            if ((side == LIGHT && c >= 48) || (side == DARK && c <= 15))
                if (color[c + (offset << 1)] == EMPTY) gen_push(c, c + (offset << 1), 24);
        }
    }

    private void gen_enpassant() {
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

    private void gen_castles() {
        if (side == LIGHT) {
            if ((castle & 1) != 0) gen_push(E1, G1, 2);
            if ((castle & 2) != 0) gen_push(E1, C1, 2);
        } else {
            if ((castle & 4) != 0) gen_push(E8, G8, 2);
            if ((castle & 8) != 0) gen_push(E8, C8, 2);
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

        if ((m.bits & 8) != 0) ep = side == LIGHT ? m.to + 8 : m.to - 8;
        else ep = -1;

        fifty = (m.bits & 17) != 0 ? 0 : fifty + 1;

        /* move the piece */
        color[m.to] = side;
        piece[m.to] = (m.bits & 32) != 0 ? m.promote : piece[m.from];
        color[m.from] = EMPTY;
        piece[m.from] = EMPTY;

        /* erase the pawn if this is an en passant move */
        if ((m.bits & 4) != 0) {
            int offset = (side == LIGHT) ? 8 : -8;
            color[m.to + offset] = piece[m.to + offset] = EMPTY;
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

}
