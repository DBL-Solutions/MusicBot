package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.Command as JDACMD

object SeekCommand : Command(), SuggestionProviding {

    val POS = SuggestionProviding.Argument("position", OptionType.STRING)

    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        event.reply("comming soon:tm:!").queue()

        // TODO: do this
    }

    override var argHistory: MutableMap<SuggestionProviding.Argument, MutableList<SuggestionProviding.Value<*>>> =
        mutableMapOf(POS to mutableListOf())

    fun handlePOSSuggestionEvent(event: CommandAutoCompleteInteractionEvent) {
        event.replyChoices(JDACMD.Choice("not yet...", "hmm yes the todo here is made out of todo")).queue()

        // TODO: do this
    }
}