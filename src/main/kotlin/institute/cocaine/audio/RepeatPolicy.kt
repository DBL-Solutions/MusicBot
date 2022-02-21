package institute.cocaine.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.*

enum class RepeatPolicy(val logInfo: String, val func: (AudioPlayer, LinkedList<AudioTrack>, AudioTrack?) -> Boolean) {
    NONE("Playing back in FIFO mode.", { player, queue, track ->
        if (queue.peek() != null) {
            player.playTrack(queue.poll())
            true
        } else {
            false
        }
    }),
    FIRST("Repeating the current song only now!", { player, queue, track ->
        if (track != null) {
            queue.addFirst(track)
            player.playTrack(queue.poll())
            true
        } else {
            false
        }
    }),
    // TODO Random Repeat/Playback Policy
    ALL("Played tracks will be added to the end of the queue.", { player, queue, track ->
        if (track != null) {
            queue.add(track)
            player.playTrack(queue.poll())
            true
        } else {
            false
        }
    });

    fun invoke(audioPlayer: AudioPlayer, queue: LinkedList<AudioTrack>, lastTrack: AudioTrack?) = func.invoke(audioPlayer, queue, lastTrack)
}