package institute.cocaine.commands

import institute.cocaine.Bot
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.Command as JDACMD

object PlayCommand: Command(), SuggestionProviding {

    val URL = SuggestionProviding.Argument("url", OptionType.STRING)
    val POS = SuggestionProviding.Argument("position", OptionType.INTEGER)

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

    override var argHistory: MutableMap<SuggestionProviding.Argument, MutableSet<SuggestionProviding.Value<*>>> =
        mutableMapOf(
            URL to mutableSetOf(),
            POS to mutableSetOf()
        )

    override var suggesttionArgs: Array<String> = arrayOf(URL.name, POS.name)

    override fun handleSuggestionEvent(event: CommandAutoCompleteInteractionEvent) {
        when (event.focusedOption.name) {
            URL.name -> event.replyChoices(
                argHistory[URL]?.filter { it.display.contains(event.focusedOption.value, ignoreCase = true) }?.map { JDACMD.Choice(it.display, it.data as String) }?.take(25) ?: return
            ).queue()
            POS.name -> {
                val choices = mutableSetOf(
                    JDACMD.Choice("top", 1L),
                    JDACMD.Choice("now", 0L),
                    JDACMD.Choice("last / append (default)", -1L),
                    JDACMD.Choice("random", -2L)
                )
                event.replyChoices(choices).queue()
            }
        }
    }
}