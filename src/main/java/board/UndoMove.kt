package board

internal class UndoMove {
    var mov: Move = Move()
    var capture: Int = 0
    var castle: Int = 0
    var ep: Int = 0
    var fifty: Int = 0 //public int hash;
}
