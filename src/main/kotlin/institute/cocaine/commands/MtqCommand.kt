package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

object MtqCommand: Command() {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val scheduler = players[event.guild!!.idLong].scheduler
        scheduler.queue.clear()
    }
}