package institute.cocaine.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.LinkedList

class TrackScheduler(private val audioPlayer: AudioPlayer): AudioEventAdapter() {

    private enum class PlayerState(val format: String) {
        PAUSE("Paused playing [%s](<%s>) at (%#.3f/%#.3f [%#.2f%%])"),
        RESUME("Resuming [%s](<%s>) at (%#.3f/%#.3f [%#.2f%%])"),
        PLAY("Playing [%s](<%s>) (%#.3fs long)"),
        ENQUEUE("Added [%s](<%s>) (%#.3fs long) to the queue at pos #%d (approx. in %s)");

        fun format(vararg args: Any?) = format.format(*args)
    }

    private lateinit var hook: InteractionHook
    val queue = LinkedList<AudioTrack>()
    fun acceptEvent(event: GenericCommandInteractionEvent) {
        this.hook = event.hook
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

    private fun logMessageToHook(track: AudioTrack, playerState: PlayerState) {
        val title = track.info.title
        val uri = track.info.uri
        val pos = audioPlayer.playingTrack?.position?.toFloat()?.div(1000)
        val dur = track.duration.toFloat() / 1000
        val perc = (100) * (pos?.div(dur) ?: 1f)
        val playsIn = queue.sumOf { it.duration } + audioPlayer.playingTrack.duration

        val action = when (playerState) {
            PlayerState.PAUSE -> hook.sendMessage(PlayerState.PAUSE.format(title, uri, pos, dur, perc))
            PlayerState.RESUME -> hook.sendMessage(PlayerState.RESUME.format(title, uri, pos, dur, perc))
            PlayerState.PLAY -> hook.sendMessage(PlayerState.PLAY.format(title, uri, dur))
            PlayerState.ENQUEUE -> hook.sendMessage(PlayerState.ENQUEUE.format(title, uri, dur, queue.size + 1, playsIn.toTime()))
        }
        action.queue()
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

    fun enqueue(track: AudioTrack) {
        logMessageToHook(track, PlayerState.ENQUEUE)
        queue.add(track)
    }

    private fun Long.toTime(): String {
        val hconv = (60 * 60 * 1000)
        val mconv = 60000
        val hours = this / hconv
        val min = (this - hours * hconv) / mconv
        val  s = (this - hours * hconv - min * mconv).toFloat() / 1000
        return "${hours}h ${min}min ${s}s"
    }
}