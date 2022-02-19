package institute.cocaine.listener

import institute.cocaine.commands.PlayCommand
import institute.cocaine.commands.SeekCommand
import institute.cocaine.commands.SkipCommand
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent

object ArgSuggestListener {

    val supportedCmds = arrayOf("play", "seek", "skip")

    suspend fun handleEvent(event: CommandAutoCompleteInteractionEvent) {
        val cmd = event.name
        if (cmd !in supportedCmds)
            return

        when (cmd) {
            "play" -> PlayCommand.handleSuggestionEvent(event)
            "seek" -> SeekCommand.handleSuggestionEvent(event)
            "skip" -> SkipCommand.handleSuggestionEvent(event)
        }
    }
}