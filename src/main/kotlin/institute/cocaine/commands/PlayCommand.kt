package institute.cocaine.commands

import institute.cocaine.Bot
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

object PlayCommand: Command(), SuggestionProviding {

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
        playerManager.loadItem(event.getOption("url")!!.asString, Bot.AudioLoader.apply {
            id = event.guild!!.idLong
            index = event.getOption("position")?.asLong?.toInt() ?: -1
            this@apply.players = this@PlayCommand.players
            this.runnable = runnable
        })
    }

    override var argHistory: MutableMap<String, Pair<OptionType, MutableList<*>>> = mutableMapOf(
        ("url" to (OptionType.STRING to mutableListOf<String>())),
        ("position" to (OptionType.INTEGER to mutableListOf<Long>()))
    )

    override fun handleSuggestionEvent(event: CommandAutoCompleteInteractionEvent) {

    }
}