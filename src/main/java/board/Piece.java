package board;

public class Piece {
    public int code;
    public int couleur;
    int nbdir;
    int[] dir;
    boolean glisse;

    public Piece() {
    }

    public Piece(int code, int couleur, int nbdir, int[] dir, boolean glisse) {
        this.code = code;
        this.couleur = couleur;
        this.nbdir = nbdir;
        this.dir = dir;
        this.glisse = glisse;
    }


}
