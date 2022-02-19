package institute.cocaine.commands

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import java.time.OffsetDateTime

object CleanCommand: Command() {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val textChannel = event.textChannel

        textChannel.iterableHistory.takeAsync(320).thenApply { list ->
            list.filter { it.author == event.jda.selfUser && it.timeCreated.isAfter(OffsetDateTime.now().minusWeeks(2)) }
        }.thenAccept {
            textChannel.purgeMessages(it)
        }
        event.deferReply().setContent("deletiert").queue()
    }
}