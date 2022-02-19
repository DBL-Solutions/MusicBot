package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import java.util.concurrent.ThreadLocalRandom

object DiceCommand: Command() {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val sites = event.getOption("sites")?.asLong ?: 6L
        event.reply("You rolled a ${ThreadLocalRandom.current().nextLong(sites) + 1} with your $sites sided dice!")
    }


}