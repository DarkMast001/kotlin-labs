# Упрощённый TOML сериализатор

## Сериализация

**Поддерживаемые типы для сериализации: String, Int, Double, Boolean, List, data class.**

Функция сериализации рекурсивно обходит объект и вложенные в него объекты. Такой механизм реализуется с помощью сета `visited: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())`.

Почему именно `IdentityHashMap()`, а не `mutableSetOf()`?

Поскольку `data class` генерирует `equals()` и `hashCode()` на основе всех свойств, то при попытке добавить объект в сет `visited.add(obj)`, вызываются функции `equals()` и `hashCode()`, но в `obj` есть свойство, которое ссылается на другой `data class`, в котором тоже есть свойство, которое ссылается на первый `data class`. То есть вычисление `hashCode()` вызывает бесконечную рекурсию. Результат: `StackOverflowError`.

`IdentityHashMap()` использует сравнение по ссылке, и циклы обнаруживаются корректно, и рекурсия завершается.

`from: String? = null` хранит в себе название поля объекта из которого мы пришли, чтобы была возможность корректно сериализовать вложенные объекты, вложенных объектов.

При сериализации перебираются все свойства с помощью `for (prop in kClass.declaredMemberProperties)`, но `kClass.declaredMemberProperties` не гарантирует порядок свойств => получается такая ситуация, когда сначала выводятся "простые" свойства (String, Int, ...), потом вложенные, (у вложенных точно так же), а потом опять "простые". Результат: некорректный синтаксис `TOML`.

Решение: создать 2 списка: `val simpleParts = mutableListOf<String>()` и `val nestedParts = mutableListOf<String>()`, где в 1-ый сначала записываем все "простые", а во 2-ой - вложенные и в конце обхединяем их.

### Пример сериализации

```Kotlin
data class User (
    val name: String,
    val age: Int,
    val isAdmin: Boolean,
    val spending: Double,
    val colleagues: List<String>,
    val otherUser: User?,
    val data: Data
)
```

```Kotlin
data class Data (
    val str: String
)
```

```Kotlin
val data1 = Data("important string")
val data2 = Data("other important string")

val user1 = User("Ivan", 20, false, 1.333, listOf("Tom", "Sonya"), null, data1)
val user2 = User("Dima", 22, true, 2.444, listOf("Ivan", "Alex"), user1, data2)

val toml = TomlSerializer.serialize(user2)
println(toml)
```

```bash
age = 22
colleagues = ["Ivan", "Alex"]
isAdmin = true
name = "Dima"
spending = 2.444

[data]
str = "other important string"

[otherUser]
age = 20
colleagues = ["Tom", "Sonya"]
isAdmin = false
name = "Ivan"
otherUser = null
spending = 1.333

[otherUser.data]
str = "important string"
```

## Десериализация

Функция `deserialize(toml: String)` является `inline` функцией, поскольку тогда тип `T` становится известен во время компиляции и к нему можно получить доступ.

В паре с `inline` используется `reified`, что позволяет нам сделать тип `T` доступным во время выполнения.

Прежде чем переходить к созданию объекта, необходимо сначала подготовить строки.

_Десериализовать в качестве примера будем файл из примера сериализации._

### Создание плоской мапы

Сначала необходимо определить `Map` со всеми названиями полей (ключ), при этом ключ должен содержать в себе полный путь до этого поля, и их значениями, чтобы потом было проще создать вложенную мапу. Это происходит в функции ` fun parseFlatMap(lines: List<String>): Map<String, Any?>`. По предыдущему примеру получается следующая `Map`:

```
flatMap = {
	"age" = 22,
	"colleagues" = {
		"Ivan",
		"Alex"
	},
	"isAdmin" = true,
	"name" = "Dima",
	"spending" = 2.444,
	"data.str" = "other important string",
	"otherUser.age" = 20,
	"otherUser.colleagues" = {
		"Tom",
		"Sonya"
	},
	"otherUser.isAdmin" = false,
	"otherUser.name" = "Ivan",
	"otherUser.otherUser" = null,
	"otherUser.spending" = 1.333,
	"otherUser.data.str" = "important string"
}
```

### Создание вложенной мапы

Теперь есть необходимость создать вложенную мапу из плоской. Вложенность реализуется рекурсией при условии, что текущий обрабатываемый элемент является уже ранее созданной (инициализированной) `Map`.

Полученная вложенная `Map`:

```
nestedMap = {
	"age" = 22,
	"colleagues" = {
		"Ivan",
		"Alex"
	},
	"isAdmin" = true,
	"name" = "Dima",
	"spending" = 2.444,
	"data" = "other important string",
	"otherUser" = {
		"age" = 20,
		"colleagues" = {
			"Tom",
			"Sonya"
		},
		"isAdmin" = false,
		"name" = "Ivan",
		"otherUser" = null,
		"spending" = 1.333,
		"data" = "important string"
	}
}
```

Теперь есть `Map`, где каждый ключ отвечает за своё поле в своём объекте (если рассматривать уровень вложенности), и по такой `Map` можно создать объект.

### Создание объекта

Благодаря рекурсивному обходу вложенных компонентов в `Map`, удаётся создать все вложенные объекты в "корневой" объект.

Продолжая пример из сериализации, сделаем десериализацию такого объека:

```Kotlin
val restored = TomlSerializer.deserialize<User>(toml)
println("\nRestored: $restored")

println("Equal? ${user2 == restored}")
```

```Bash
Restored: User(name=Dima, age=22, isAdmin=true, spending=2.444, colleagues=[Ivan, Alex], otherUser=User(name=Ivan, age=20, isAdmin=false, spending=1.333, colleagues=[Tom, Sonya], otherUser=null, data=Data(str=important string)), data=Data(str=other important string))
Equal? true
```
