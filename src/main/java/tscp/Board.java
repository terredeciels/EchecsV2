package tscp;

import java.util.ArrayList;
import java.util.List;

public class Board implements Constants {

    public int[] color = new int[64];
    public int[] piece = new int[64];
    public int side;
    public int xside;
    public int castle;
    public int ep;
    public List<Move> pseudomoves = new ArrayList<>();
    private int fifty;
    private UndoMove um = new UndoMove();
    public int halfMoveClock;
    public int plyNumber;

    public Board() {
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
        for (int i = 0; i < 64; ++i) {
            if (piece[i] == KING && color[i] == s) {
                return attack(i, s ^ 1);
            }
        }
        return true; // shouldn't get here
    }

    private boolean attack(int sq, int s) {
        for (int i = 0; i < 64; ++i) {
            if (color[i] == s) {
                if (piece[i] == PAWN) {
                    if (s == LIGHT) {
                        if ((i & 7) != 0 && i - 9 == sq) {
                            return true;
                        }
                        if ((i & 7) != 7 && i - 7 == sq) {
                            return true;
                        }
                    } else {
                        if ((i & 7) != 0 && i + 7 == sq) {
                            return true;
                        }
                        if ((i & 7) != 7 && i + 9 == sq) {
                            return true;
                        }
                    }
                } else {
                    for (int j = 0; j < offsets[piece[i]]; ++j) {
                        for (int n = i;;) {
                            n = mailbox[mailbox64[n] + offset[piece[i]][j]];
                            if (n == -1) {
                                break;
                            }
                            if (n == sq) {
                                return true;
                            }
                            if (color[n] != EMPTY) {
                                break;
                            }
                            if (!slide[piece[i]]) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void gen() {
        int i;
        int j;
        int n;

        for (i = 0; i < 64; ++i) {
            if (color[i] == side) {
                if (piece[i] == PAWN) {
                    if (side == LIGHT) {
                        if ((i & 7) != 0 && color[i - 9] == DARK) {
                            gen_push(i, i - 9, 17);
                        }
                        if ((i & 7) != 7 && color[i - 7] == DARK) {
                            gen_push(i, i - 7, 17);
                        }
                        if (color[i - 8] == EMPTY) {
                            gen_push(i, i - 8, 16);
                            if (i >= 48 && color[i - 16] == EMPTY) {
                                gen_push(i, i - 16, 24);
                            }
                        }
                    } else {
                        if ((i & 7) != 0 && color[i + 7] == LIGHT) {
                            gen_push(i, i + 7, 17);
                        }
                        if ((i & 7) != 7 && color[i + 9] == LIGHT) {
                            gen_push(i, i + 9, 17);
                        }
                        if (color[i + 8] == EMPTY) {
                            gen_push(i, i + 8, 16);
                            if (i <= 15 && color[i + 16] == EMPTY) {
                                gen_push(i, i + 16, 24);
                            }
                        }
                    }
                } else {
                    for (j = 0; j < offsets[piece[i]]; ++j) {
                        for (n = i;;) {
                            n = mailbox[mailbox64[n] + offset[piece[i]][j]];
                            if (n == -1) {
                                break;
                            }
                            if (color[n] != EMPTY) {
                                if (color[n] == xside) {
                                    gen_push(i, n, 1);
                                }
                                break;
                            }
                            gen_push(i, n, 0);
                            if (!slide[piece[i]]) {
                                break;
                            }
                        }
                    }
                }
            }
        }

        /* generate castle moves */
        if (side == LIGHT) {
            if ((castle & 1) != 0) {
                gen_push(E1, G1, 2);
            }
            if ((castle & 2) != 0) {
                gen_push(E1, C1, 2);
            }
        } else {
            if ((castle & 4) != 0) {
                gen_push(E8, G8, 2);
            }
            if ((castle & 8) != 0) {
                gen_push(E8, C8, 2);
            }
        }

        /* generate en passant moves */
        if (ep != -1) {
            if (side == LIGHT) {
                if ((ep & 7) != 0 && color[ep + 7] == LIGHT && piece[ep + 7] == PAWN) {
                    gen_push(ep + 7, ep, 21);
                }
                if ((ep & 7) != 7 && color[ep + 9] == LIGHT && piece[ep + 9] == PAWN) {
                    gen_push(ep + 9, ep, 21);
                }
            } else {
                if ((ep & 7) != 0 && color[ep - 9] == DARK && piece[ep - 9] == PAWN) {
                    gen_push(ep - 9, ep, 21);
                }
                if ((ep & 7) != 7 && color[ep - 7] == DARK && piece[ep - 7] == PAWN) {
                    gen_push(ep - 7, ep, 21);
                }
            }
        }
    }

    private void gen_push(int from, int to, int bits) {
        if ((bits & 16) != 0) {
            if (side == LIGHT) {
                if (to <= H8) {
                    gen_promote(from, to, bits);
                    return;
                }
            } else if (to >= A1) {
                gen_promote(from, to, bits);
                return;
            }
        }
        pseudomoves.add(new Move((byte) from, (byte) to, (byte) 0, (byte) bits));

    }

    private void gen_promote(int from, int to, int bits) {
        for (int i = KNIGHT; i <= QUEEN; ++i) {
            pseudomoves.add(new Move((byte) from, (byte) to, (byte) i, (byte) (bits | 32)));
        }
    }

    public boolean makemove(Move m) {
        if ((m.bits & 2) != 0) {
            int from;
            int to;

            if (in_check(side)) {
                return false;
            }
            switch (m.to) {
                case 62:
                    if (color[F1] != EMPTY || color[G1] != EMPTY || attack(F1, xside) || attack(G1, xside)) {
                        return false;
                    }
                    from = H1;
                    to = F1;
                    break;
                case 58:
                    if (color[B1] != EMPTY || color[C1] != EMPTY || color[D1] != EMPTY || attack(C1, xside) || attack(D1, xside)) {
                        return false;
                    }
                    from = A1;
                    to = D1;
                    break;
                case 6:
                    if (color[F8] != EMPTY || color[G8] != EMPTY || attack(F8, xside) || attack(G8, xside)) {
                        return false;
                    }
                    from = H8;
                    to = F8;
                    break;
                case 2:
                    if (color[B8] != EMPTY || color[C8] != EMPTY || color[D8] != EMPTY || attack(C8, xside) || attack(D8, xside)) {
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

        /* back up information so we can take the move back later. */
        um.mov = m;
        um.capture = piece[(int) m.to];
        um.castle = castle;
        um.ep = ep;
        um.fifty = fifty;

        castle &= castle_mask[(int) m.from] & castle_mask[(int) m.to];

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
        color[(int) m.to] = side;
        if ((m.bits & 32) != 0) {
            piece[(int) m.to] = m.promote;
        } else {
            piece[(int) m.to] = piece[(int) m.from];
        }
        color[(int) m.from] = EMPTY;
        piece[(int) m.from] = EMPTY;

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

        color[(int) m.from] = side;
        if ((m.bits & 32) != 0) {
            piece[(int) m.from] = PAWN;
        } else {
            piece[(int) m.from] = piece[(int) m.to];
        }
        if (um.capture == EMPTY) {
            color[(int) m.to] = EMPTY;
            piece[(int) m.to] = EMPTY;
        } else {
            color[(int) m.to] = xside;
            piece[(int) m.to] = um.capture;
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
    public String[] piece_char_light = {"P", "N", "B", "R", "Q", "K"};
    public String[] piece_char_dark = {"p", "n", "b", "r", "q", "k"};

    public void print_board() {
        int i;

        System.out.print("\n8 ");
        for (i = 0; i < 64; ++i) {
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
