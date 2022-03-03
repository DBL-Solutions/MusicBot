package institute.cocaine.commands

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

object QueueCommand: Command() {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val scheduler = players[event.guild!!.idLong].scheduler
        scheduler.info(
            scheduler.queue
                .mapIndexed { index, audioTrack -> audioTrack.toQueueInfo(index) }
                .joinToString(separator = "\n")
        )
    }

    private fun AudioTrack.toQueueInfo(index: Int): String {
        val title = info.title
        val uri = info.uri
        val author = info.author
        val length = duration.toTime()
        return "#${index + 1} [$title](<$uri>) - `$author` ($length)"
    }

    private fun Long.toTime(): String {
        val hconv = (60 * 60 * 1000)
        val mconv = (60 * 1000)
        val hours = this / hconv
        val min = (this - hours * hconv) / mconv
        val s = (this - hours * hconv - min * mconv) / 1000
        return "" + (if (hours > 0) "%2d:".format(hours) else "") + (if (min > 0) "%2d:".format(min) else "") + "%2d".format(s)
    }
}
