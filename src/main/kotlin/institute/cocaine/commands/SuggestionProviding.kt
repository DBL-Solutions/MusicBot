package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

sealed interface SuggestionProviding {
    var argHistory: MutableMap<String, Pair<OptionType, MutableList<*>>>

    fun handleSuggestionEvent(event: CommandAutoCompleteInteractionEvent)
}