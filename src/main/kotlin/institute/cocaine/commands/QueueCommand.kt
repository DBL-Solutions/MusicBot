package institute.cocaine.commands

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.Message
import dev.minn.jda.ktx.interactions.sendPaginator
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import java.util.LinkedList
import java.util.Queue
import kotlin.time.Duration.Companion.minutes

object QueueCommand: Command() {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val scheduler = players[event.guild!!.idLong].scheduler

        val queueContent = scheduler.queue
            .mapIndexed { index, audioTrack -> audioTrack.toQueueInfo(index) }
            .joinToString(separator = "\n")

        val messages = queueContent.sliceOnSequenceInsideOfLimit("\n", 4000)
            .map { Message {
                content = it
            }}
            .toTypedArray()
        
        event.hook.sendPaginator(pages = messages, expireAfter = 10.minutes).queue()

    }

    private fun AudioTrack.toQueueInfo(index: Int): String {
        val title = info.title
        val uri = info.uri
        val author = info.author
        val length = duration.toTime()
        return "#${index + 1} [$title](<$uri>) - `$author` ($length)"
    }
    
    private fun String.sliceOnSequenceInsideOfLimit(
        str: String,
        maxLength: Int
    ): Queue<String> {
        val out = LinkedList<String>()

        if (this.length < maxLength) {
            out.add(this)
            return out
        }

        var lastIndex = 0
        var lastSliceEnd = 0
        var previous: Int

        while (lastIndex != -1) {
            previous = lastIndex
            lastIndex = this.indexOf(str, lastIndex + str.length, false)
            if ((lastIndex - lastSliceEnd) > (maxLength - str.length)) {
                out.add(substring(lastSliceEnd, previous))
                lastSliceEnd = previous + str.length
            }
        }

        out.add(substring(lastSliceEnd))

        return out
    }
}
