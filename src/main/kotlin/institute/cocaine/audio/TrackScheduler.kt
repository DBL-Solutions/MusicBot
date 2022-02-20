package institute.cocaine.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.SLF4J
import institute.cocaine.commands.PlayCommand
import institute.cocaine.commands.SuggestionProviding
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.LinkedList
import java.util.concurrent.ThreadLocalRandom

class TrackScheduler(private val audioPlayer: AudioPlayer): AudioEventAdapter() {

    private enum class PlayerState(val format: String) {
        PAUSE("Paused playing [%s](<%s>) at (%#.3f/%#.3f [%#.2f%%])"),
        RESUME("Resuming [%s](<%s>) at (%#.3f/%#.3f [%#.2f%%])"),
        PLAY("Playing [%s](<%s>) (%#.3fs long)"),
        ENQUEUE("Added [%s](<%s>) (%#.3fs long) to the queue at pos #%d (approx. in %s)"),
        INFO("%s");

        fun format(vararg args: Any?) = format.format(*args)
    }

    private val logger by SLF4J
    private lateinit var jda: JDA
    private lateinit var hook: InteractionHook
    private var chanId: Long = 0
    private var idToRef: Long = 0
    val queue = LinkedList<AudioTrack>()
    fun acceptEvent(event: GenericCommandInteractionEvent, channelId: Long, idToRef: Long) {
        this.hook = event.hook
        this.jda = event.jda
        this.chanId = channelId
        this.idToRef = idToRef
    }

    override fun onPause(player: AudioPlayer) {
        logMessageToHook(player.playingTrack, PlayerState.PAUSE)
    }

    override fun onResume(player: AudioPlayer) {
        logMessageToHook(player.playingTrack, PlayerState.RESUME)
    }

    override fun onStart(player: AudioPlayer, track: AudioTrack) {
        logMessageToHook(track, PlayerState.PLAY)
    }

    override fun onEnd(player: AudioPlayer, track: AudioTrack?, endReason: AudioTrackEndReason) {
        if (!endReason.mayStartNext) {
            return
        }
        if (queue.peek() != null)
            player.playTrack(queue.poll())

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    override fun onException(player: AudioPlayer, track: AudioTrack?, exception: FriendlyException) {
        // An already playing track threw an exception (track end event will still be received separately)
    }

    override fun onStuck(player: AudioPlayer, track: AudioTrack?, thresholdMs: Long, stackTrace: Array<StackTraceElement>) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
    }

    private fun logMessageToHook(track: AudioTrack?, playerState: PlayerState, count: Int = 0, info: String = "") {
        if (track == null) {
            val str = "hmm track to log was null but ${playerState.name} has been performed"
            if (hook.isExpired) {
                jda.getTextChannelById(chanId)?.sendMessage(str)?.queue() ?: logger.warn("channel {} null & palying track null", chanId)
            } else {
                hook.sendMessage(str).queue()
            }
            return
        }
        val title = track.info.title
        val uri = track.info.uri
        val pos = audioPlayer.playingTrack?.position?.toFloat()?.div(1000)
        val dur = track.duration.toFloat() / 1000
        val perc = (100) * (pos?.div(dur) ?: 1f)
        val playsIn = if (count > 0) (queue.take(count).sumOf { it.duration }) else (queue.sumOf { it.duration }) + (audioPlayer.playingTrack?.position ?: 0L)
        val qPos = if (count > 0) (count + 1) else (queue.size + 1)

        if (!hook.isExpired) {
            val action = when (playerState) {
                PlayerState.PAUSE -> hook.sendMessage(PlayerState.PAUSE.format(title, uri, pos, dur, perc))
                PlayerState.RESUME -> hook.sendMessage(PlayerState.RESUME.format(title, uri, pos, dur, perc))
                PlayerState.PLAY -> hook.sendMessage(PlayerState.PLAY.format(title, uri, dur))
                PlayerState.ENQUEUE -> hook.sendMessage(PlayerState.ENQUEUE.format(title, uri, dur, qPos, playsIn.toTime()))
                PlayerState.INFO -> hook.sendMessage(PlayerState.INFO.format(info))
            }
            action.queue()
        } else {
            val action = jda.getTextChannelById(chanId)?.sendMessageEmbeds(Embed {
                description = when (playerState) {
                    PlayerState.PAUSE -> PlayerState.PAUSE.format(title, uri, pos, dur, perc)
                    PlayerState.RESUME -> PlayerState.RESUME.format(title, uri, pos, dur, perc)
                    PlayerState.PLAY -> PlayerState.PLAY.format(title, uri, dur)
                    PlayerState.ENQUEUE -> PlayerState.ENQUEUE.format(title, uri, dur, qPos, playsIn.toTime())
                    PlayerState.INFO -> PlayerState.INFO.format(info)
                }
            })
            action?.referenceById(idToRef)?.queue() ?: logger.warn("channel {} was null, could not ref msg {} for logging player info", chanId, idToRef)
        }
    }

    @Suppress("unused")
    private fun skipTrack() {
        skipTracks(1)
    }

    fun skipTracks(count: Int) {
        var nextTrack: AudioTrack? = null
        for (i in 0 until count) {
            nextTrack = queue.poll()
        }
        if (nextTrack == null) {
            audioPlayer.stopTrack()
        } else {
            audioPlayer.playTrack(nextTrack)
        }
    }

    fun enqueue(track: AudioTrack, index: Int) {
        PlayCommand.addToHistory(PlayCommand.URL, SuggestionProviding.Value(track.info.title, track.info.uri))

        when (index) {
             0 -> {
                 audioPlayer.startTrack(track, false)
             }
            -1 -> {
                logMessageToHook(track, PlayerState.ENQUEUE, index)
                queue.add(track)
            }
            -2 -> {
                val rndPos = ThreadLocalRandom.current().nextInt(queue.size)
                logMessageToHook(track, PlayerState.ENQUEUE, rndPos)
                queue.add(rndPos, track)
            }
            else -> {
                if (index in 1 .. queue.size) {
                    if (index != 1)
                        PlayCommand.addToHistory(PlayCommand.POS, SuggestionProviding.Value("${index}. pos", index))
                    logMessageToHook(track, PlayerState.ENQUEUE, index)
                    queue.add(index - 1, track)
                } else {
                    logMessageToHook(track, PlayerState.ENQUEUE, count = 0, info = "The provided position was outside of the queue")
                    queue.add(track)
                }
            }
        }

        if (audioPlayer.playingTrack == null) {
            audioPlayer.playTrack(queue.poll())
        }
    }

    private fun Long.toTime(): String {
        val hconv = (60 * 60 * 1000)
        val mconv = 60000
        val hours = this / hconv
        val min = (this - hours * hconv) / mconv
        val  s = (this - hours * hconv - min * mconv).toFloat() / 1000
        return "" + (if (hours > 0) "${hours}hours " else "") + (if (min > 0) "${min}mins " else "") + "${s}s"
    }
}