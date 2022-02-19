package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

object SkipCommand : Command(), SuggestionProviding {

    val AMOUNT = SuggestionProviding.Argument("amount", OptionType.INTEGER)

    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val n = event.getOption("amount")?.asLong ?: 1L
        players[event.guild!!.idLong].scheduler.skipTracks(n.toInt())
        event.deferReply().setContent("Skipping $n tracks!").queue()
    }

    override var argHistory: MutableMap<SuggestionProviding.Argument, MutableSet<SuggestionProviding.Value<*>>> =
        mutableMapOf(
            AMOUNT to mutableSetOf()
        )
    override var suggesttionArgs: Array<String> = arrayOf(AMOUNT.name)

    override fun handleSuggestionEvent(event: CommandAutoCompleteInteractionEvent) {
        TODO("Not yet implemented")
    }
}