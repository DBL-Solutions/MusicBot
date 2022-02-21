package institute.cocaine.commands

import institute.cocaine.Bot
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.Command as JDACMD

object PlayCommand: Command(), SuggestionProviding {

    val URL = SuggestionProviding.Argument("url", OptionType.STRING)
    val POS = SuggestionProviding.Argument("position", OptionType.INTEGER)

    override var argHistory: MutableMap<SuggestionProviding.Argument, MutableList<SuggestionProviding.Value<*>>> =
        mutableMapOf(
            URL to mutableListOf(),
            POS to mutableListOf()
        )

    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val runnable = if (!event.guild!!.selfMember.voiceState!!.inAudioChannel()) {
            joinVC(event, event.member!!.voiceState!!.channel!!)
            Runnable {
                println("Playing music now in ${event.member!!.voiceState!!.channel!!.name}")
            }
        } else {
            event.deferReply().setContent("Trying to add element to queue").queue()
            Runnable {
                event.hook.deleteOriginal().queue()
            }
        }
        val urlOption = event.getOption(URL.name)!!.asString
        val posOption = event.getOption("position")?.asLong?.toInt() ?: -1
        playerManager.loadItem(urlOption, Bot.AudioLoader.apply {
            id = event.guild!!.idLong
            index = posOption
            this@apply.players = Companion.players
            this.runnable = runnable
        })
    }

    fun handleURLSuggestion(event: CommandAutoCompleteInteractionEvent) {
        var toReply = argHistory[URL]?.filter { it.matches(event.focusedOption.value) }

        if (!Bot.respectPrivacy)
            toReply = toReply?.sortedByDescending { it.uses }

        event.replyChoices(toReply?.
            map { JDACMD.Choice(it.display, it.data as String) }?.
            take(25) ?: return
        ).queue()
    }

    fun handlePOSSuggestion(event: CommandAutoCompleteInteractionEvent) {
        val choices = mutableSetOf(
            JDACMD.Choice("top", 1L),
            JDACMD.Choice("now", 0L),
            JDACMD.Choice("last / append (default)", -1L),
            JDACMD.Choice("random", -2L)
        )

        val queueLength = Companion.players[event.guild!!.idLong].scheduler.queue.size
        var toAdd = argHistory[POS]?.
        filter { it.matches(event.focusedOption.value) && queueLength > (it.data as Long).toInt() }

        if (!Bot.respectPrivacy)
            toAdd = toAdd?.sortedByDescending { it.uses }

        toAdd?.map { JDACMD.Choice(it.display, it.data as Long) }?.
        take(21)?.forEach(choices::add)

        event.replyChoices(choices).queue()
    }
}