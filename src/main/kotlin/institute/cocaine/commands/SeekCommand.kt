package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

object SeekCommand : Command(), SuggestionProviding {

    val POS = SuggestionProviding.Argument("position", OptionType.STRING)

    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        event.reply("comming soon:tm:!").queue()
    }

    override var argHistory: MutableMap<SuggestionProviding.Argument, MutableSet<SuggestionProviding.Value<*>>> =
        mutableMapOf(POS to mutableSetOf())

    override var suggesttionArgs: Array<String> = arrayOf(POS.name)

    override fun handleSuggestionEvent(event: CommandAutoCompleteInteractionEvent) {
        event.replyChoiceStrings("not yet...").queue()
    }
}