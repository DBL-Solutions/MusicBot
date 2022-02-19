package institute.cocaine.commands


import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

sealed interface SuggestionProviding {

    var argHistory: MutableMap<Argument, MutableSet<Value<*>>>
    var suggesttionArgs: Array<String>

    fun handleSuggestionEvent(event: CommandAutoCompleteInteractionEvent)

    fun <T> addToHistory(arg: Argument, data: Value<T>) {
        argHistory[arg]?.add(data)
    }

    data class Argument(val name: String, val type: OptionType)

    data class Value<T>(val display: String, val data: T)

}

