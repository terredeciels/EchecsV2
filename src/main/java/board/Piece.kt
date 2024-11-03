package board

class Piece {
    var code: Int = 0
    var couleur: Int = 0
    var nbdir: Int = 0
    lateinit var dir: IntArray
    var glisse: Boolean = false

    constructor()

    constructor(code: Int, couleur: Int, nbdir: Int, dir: IntArray, glisse: Boolean) {
        this.code = code
        this.couleur = couleur
        this.nbdir = nbdir
        this.dir = dir
        this.glisse = glisse
    }
}
