package institute.cocaine.commands


import institute.cocaine.Bot
import net.dv8tion.jda.api.interactions.commands.OptionType

sealed interface SuggestionProviding {

    var argHistory: MutableMap<Argument, MutableList<Value<*>>>

    fun <T> addToHistory(arg: Argument, data: Value<T>) {
        val list: MutableList<Value<*>>? = argHistory[arg]
        if (list == null) {
            argHistory[arg] = mutableListOf(data)
            return
        }
        val storedData = list.firstOrNull { it == data }
        if (storedData == null) {
            list.add(data)
            return
        }
        storedData.uses += 1
    }

    data class Argument(val name: String, val type: OptionType)

    @Suppress("EqualsOrHashCode")
    data class Value<T>(val display: String, val data: T) {

        var uses: Int = 0
            get() = if(Bot.respectPrivacy) 0 else field
            set(value) {
                field = if (Bot.respectPrivacy) 0 else value
            }

        fun matches(other: String): Boolean {
            if (display.contains(other, true))
                return true

            if (data is String)
                return data.contains(other, true)

            if (data is Number)
                return data.toString().contains(other)

            return false
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Value<*>)
                return false

            if (other.display == this.display && this.data == other.data)
                return true
            return false
        }
    }

}

