package institute.cocaine.commands

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.CoroutineEventListener
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.Message
import dev.minn.jda.ktx.listener
import institute.cocaine.commands.NowPlayingCommand.ceil
import institute.cocaine.commands.NowPlayingCommand.floor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.concurrent.TimeUnit

object NowPlayingCommand: Command() {

    private var stopUpdating = false

    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val audioPlayer = players[event.guild!!.idLong].audioPlayer
        val dm = event.getOption("grab")?.asBoolean ?: false
        val track = audioPlayer.playingTrack
        if (!dm) {
            event.reply(Message {
                content = track?.asInfo() ?: "Nothing rn, queue something bish"
            }).queue {
                GlobalScope.launch {
                    val listener = event.jda.listener<MessageReceivedEvent> { listenerEvent ->
                        if (event.channel != listenerEvent.channel ) return@listener
                        stopUpdating = true
                    }
                    delay(10_000)
                    updateProgressBar(it, audioPlayer, listener)
                }
            }
        } else {
            event.deferReply(true).setContent("Information has been sent!").queue()
            event.user.openPrivateChannel().flatMap {
                it.sendMessageEmbeds(Embed {
                    description = track?.asInfo(true) ?: "Nothing rn, queue something bish"
                    color = event.guild?.selfMember?.colorRaw
                })
            }.queue()
        }
    }

    private fun updateProgressBar(hook: InteractionHook, player: AudioPlayer, listener: CoroutineEventListener) {
        if (stopUpdating || player.playingTrack == null) {
            stopUpdating = false
            hook.jda.removeEventListener(listener)
            return
        }
        val track = player.playingTrack
        hook.editOriginal(Message {
            content = track?.asInfo() ?: "Nothing rn, queue something bish"
        }).queueAfter(15, TimeUnit.SECONDS) {
            updateProgressBar(hook, player, listener)
        }
    }

    private fun AudioTrack.asInfo(isEmbed: Boolean = false): String {
        val title = info.title
        val uri = info.uri
        val author = info.author
        val length = (title.length + 3 + author.length + 7 + 6).coerceIn(20, if (isEmbed) 50 else 100)
        val text = "[$title](<$uri>) by `$author` until <t:${(System.currentTimeMillis() + duration - position) / 1000}:t>"
        val (progressbar, pos) = makeProgressbar(position.toFloat() / duration, length, info.isStream)
        val curTime = position.toTime()
        val halfTimeLength = (curTime.length.toFloat() / 2)
        val durTime = duration.toTime()
        val time = makeTime(info.isStream, pos, length, halfTimeLength, curTime, durTime)

        return """
        ::> $text
        ::```ansi
        ::[40m$progressbar[0m
        ::[40m$time[0m
        ::```
        """.trimMargin("::")
    }

    private fun makeProgressbar(progress: Float, width: Int = 20, isSteam: Boolean): Pair<String, Int> {
        if (isSteam)
            return "\u001B[33m⌈${" ".repeat((width / 2) -1)}\u001B[35m\u221E${" ".repeat((width / 2))}\u001B[33m⌉" to (width / 2)
        val fullchars = (progress * width).floor()
        val a = ">"
        val emptychars = width - fullchars - 1
        return buildString(5 * 5 + 2 + width) {
            append("\u001B[33m⌈\u001B[32m")
            append("#".repeat(fullchars))
            append("\u001B[35m")
            append(a)
            append("\u001B[34m")
            append("-".repeat(emptychars))
            append("\u001B[33m⌉")
        } to (fullchars)
    }

    private fun makeTime(isSteam: Boolean, pos: Int, length: Int, halfTimeLength: Float, curTime: String, durTime: String): String {
        val front = pos < (2 + halfTimeLength)
        val back = pos > (length - durTime.length - halfTimeLength.ceil())
        val hasTimeCollision = (front) || (back)
        if (isSteam)
            return "\u001B[33m⌊\u001B[35m${" ".repeat(pos - 1)}\u001B[35m\u221E${" ".repeat(pos)}\u001B[33m⌋"

        if (hasTimeCollision)
            return buildString(4*5 + 3 + length) {
                append("\u001B[33m⌊\u001B[3")
                if (front) {
                    append("5m")
                    append(curTime)
                } else {
                    append("7m0")
                }
                append(" ".repeat(length - curTime.length - (durTime.length * front) + (1L * back)))
                if (back) {
                    append("5m")
                    append(curTime)
                } else {
                    append("7m")
                    append(durTime)
                }
                append("\u001B[33m⌋")
            }

        return buildString(5*5 + 2 + length) {
            append("\u001B[33m⌊\u001B[37m0")
            append(" ".repeat(pos - halfTimeLength.ceil()))
            append("\u001B[35m")
            append(curTime)
            append(" ".repeat(length - pos - halfTimeLength.floor() - durTime.length - 1))
            append("\u001B[37m")
            append(durTime)
            append("\u001B[33m⌋")
        }
    }

    private fun Long.toTime(): String {
        val hconv = (60 * 60 * 1000)
        val mconv = (60 * 1000)
        val hours = this / hconv
        val min = (this - hours * hconv) / mconv
        val s = (this - hours * hconv - min * mconv) / 1000
        return "" + (if (hours > 0) "$hours:" else "") + (if (min > 0) "$min:" else "") + s
    }

    private fun Float.ceil(): Int = kotlin.math.ceil(this).toInt()
    private fun Float.floor(): Int = kotlin.math.floor(this).toInt()



    private operator fun Long.times(bool: Boolean): Int {
        return if (bool) -(this.toInt())
            else this.toInt()
    }

    private operator fun Int.times(bool: Boolean): Int {
        return if (bool) this
            else 0
    }
}