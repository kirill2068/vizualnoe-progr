import kotlin.random.Random

class Human {
    var surname: String =""
    var name: String =""
    var second_n: String =""
    var age: Int = 0
    var speed: Double = 0.0

    var groupN: Int = -1
    var x = 0
    var y = 0

    constructor(surname_: String, name_: String, secondN: String, group_n: Int, age_: Int) {
        name = name_
        surname = surname_
        second_n = secondN
        groupN = group_n
        age = age_
        speed = 1.0
        println("Создан Human: $name")
    }
    fun move() {
        val dx = Random.nextInt(-1,2)
        val dy = Random.nextInt(-1,2)

        x += dx
        y += dy

        println("$surname $name перешел(а) на: ($x, $y)")

    }
    fun getCoordinate(): String = "($x, $y)"

}

fun main() {
    val humans = arrayOf(
        Human("Виктория", "Багазий", "Викторовна", 432, 20),
        Human("Игнат", "Бенескул", "Максимович", 432, 19),
        Human("Иван", "Боровецкий", "Яковлевич", 432, 18),
        Human("Таисия", "Воинова", "Александровна", 432, 19),
        Human("Владимир", "Гомбоев", "Евгеньевич", 432, 19),
        Human("Алёна", "Григорьева", "Алексеевна", 432, 18),
        Human("Михаил", "Зинаков", "Романович", 432, 21),
        Human("Роман", "Крикунов", "Сергеевич", 432, 19),
        Human("Александр", "Пастухов", "Андреевич", 432, 20),
        Human("Тимофей", "Петров", "Игоревич", 432, 18),
        Human("Владислав", "Салий", "Павлович", 432, 19),
        Human("Кирилл", "Симонов", "Дмитриевич", 432, 19),
        Human("Александр", "Стаценко", "Олегович", 432, 20),
        Human("Виктория", "Стебихова", "Владимировна", 432, 20),
        Human("Михаил", "Синица", "Александрович", 433, 19),
        Human("Илья", "Луценко", "Михайлович", 431, 20),
        Human("Данил", "Садилов", "Сергеевич", 431, 20),
        Human("Данил", "Антропов", "Сергеевич", 432, 20),
    )
    println("Длительность симуляции:")
    val simulationTiming = try {
        readln().toInt()
    } catch (e: Exception) {
        println("Ошибка ввода!")
        return
    }

    if (simulationTiming <= 0) {
        println("Неправильная длительность симуляции!")
        return
    }

    println("Начало участников (0,0)")

    var second = 1
    while (second <= simulationTiming) {
        println("${second}")

        var i = 0
        while (i < humans.size) {
            humans[i].move()
            i++
        }
        println()
        second++
    }

    println("\nКонечное состояние:")
    for (human in humans) {
        println("${human.surname} ${human.name} ${human.second_n}: ${human.getCoordinate()}")
    }
}
