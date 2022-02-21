package institute.cocaine.commands

import institute.cocaine.audio.RepeatPolicy
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

object RepeatCommand: Command() {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val newPolicy = when (event.subcommandName) {
            "none" -> RepeatPolicy.NONE
            "current" -> RepeatPolicy.FIRST
            "all" -> RepeatPolicy.ALL
            else -> RepeatPolicy.NONE // HOW? Kotlin can't know this tho
        }
        val scheduler = players[event.guild!!.idLong].scheduler
        scheduler.policy = newPolicy
    }
}