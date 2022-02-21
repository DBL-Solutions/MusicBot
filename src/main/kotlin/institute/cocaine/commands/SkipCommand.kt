package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.Command as JDACMD

object SkipCommand : Command(), SuggestionProviding {

    val AMOUNT = SuggestionProviding.Argument("amount", OptionType.INTEGER)

    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val n = event.getOption("amount")?.asLong ?: 1L
        players[event.guild!!.idLong].scheduler.skipTracks(n.toInt())
        event.deferReply().setContent("Skipping $n tracks!").queue()
    }

    override var argHistory: MutableMap<SuggestionProviding.Argument, MutableList<SuggestionProviding.Value<*>>> =
        mutableMapOf(
            AMOUNT to mutableListOf()
        )

    fun handleAMOUNTSuggestionEvent(event: CommandAutoCompleteInteractionEvent) {
        event.replyChoices(JDACMD.Choice("autocomplete not implemented", 1L)).queue()
        // TODO("Not yet implemented")
    }
}