package institute.cocaine.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.*

class TrackScheduler(private val audioPlayer: AudioPlayer): AudioEventAdapter() {
    private lateinit var hook: InteractionHook
    val queue = LinkedList<AudioTrack>()
    fun acceptEvent(event: SlashCommandEvent) {
        this.hook = event.hook
    }

    override fun onPause(player: AudioPlayer) {
        val playingTrack = player.playingTrack
        val title = playingTrack.info.title
        val uri = playingTrack.info.uri
        val pos = player.playingTrack.position.toFloat() / 1000
        val dur = playingTrack.duration.toFloat() / 1000
        val perc = 100 * pos / dur
        hook.sendMessage("Paused playing [$title](<$uri>) at ($pos/$dur [%#.2f%%])\"".format(perc)).queue()
    }

    override fun onResume(player: AudioPlayer) {
        val playingTrack = player.playingTrack
        val title = playingTrack.info.title
        val uri = playingTrack.info.uri
        val pos = player.playingTrack.position.toFloat() / 1000
        val dur = playingTrack.duration.toFloat() / 1000
        val perc = 100 * pos / dur
        hook.sendMessage("Resuming [$title](<$uri>) at ($pos/$dur [%#.2f%%])\"".format(perc)).queue()
    }

    override fun onStart(player: AudioPlayer, track: AudioTrack) {
        // A track started playing
        val title = track.info.title
        val uri = track.info.uri
        hook.sendMessage("Playing [$title](<$uri>) (${track.duration.toFloat() / 1000}s long)").queue()
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
        queue.add(track)
    }
}