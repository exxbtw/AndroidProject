class Human(val FIO: String, var age: Int, var speed: Double) {
    var x: Double = 0.0
    var y: Double = 0.0

    fun move() {
        var dx = (-1..1).random()
        var dy = (-1..1).random()
        x = x + dx * speed
        y = y + dy * speed
    }
}

fun main() {
    val humans = Array(14) { i ->
        Human("Human${i + 1}", 18 + i, 2.0 + i * 0.2)
    }

    val simulationTime = 10

    for (t in 1..simulationTime) {
        println("$t-ая секунда")
        for (h in humans) {
            h.move()
            println("${h.FIO} - (${h.x}, ${h.y})")
        }
    }

}