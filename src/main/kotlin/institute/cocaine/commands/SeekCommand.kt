package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

object SeekCommand : Command(), SuggestionProviding {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        event.reply("comming soon:tm:!")
    }

    override var argHistory: MutableMap<String, Pair<OptionType, MutableList<*>>> =
        mutableMapOf()

    override fun handleSuggestionEvent(event: CommandAutoCompleteInteractionEvent) {
        TODO("Not yet implemented")
    }
}