package institute.cocaine.commands

import institute.cocaine.Bot
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

object QueueCommand: Command() {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val scheduler = players[event.guild!!.idLong].scheduler
        scheduler.info(scheduler.queue.toString())
    }
}
