package institute.cocaine.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent
import com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason

abstract class AudioEventAdapter : AudioEventListener {

    override fun onEvent(event: AudioEvent) {
        when (event) {
            is PlayerPauseEvent -> {
                onPause(event.player)
            }
            is PlayerResumeEvent -> {
                onResume(event.player)
            }
            is TrackStartEvent -> {
                onStart(event.player, event.track)
            }
            is TrackEndEvent -> {
                onEnd(event.player, event.track, event.endReason)
            }
            is TrackExceptionEvent -> {
                onException(event.player, event.track, event.exception)
            }
            is TrackStuckEvent -> {
                onStuck(event.player, event.track, event.thresholdMs, event.stackTrace)
            }
        }
    }

    abstract fun onPause(player: AudioPlayer)
    abstract fun onResume(player: AudioPlayer)
    abstract fun onStart(player: AudioPlayer, track: AudioTrack)
    abstract fun onEnd(player: AudioPlayer, track: AudioTrack?, endReason: AudioTrackEndReason)
    abstract fun onException(player: AudioPlayer, track: AudioTrack?, exception: FriendlyException)
    abstract fun onStuck(player: AudioPlayer, track: AudioTrack?, thresholdMs: Long, stackTrace: Array<StackTraceElement>)
}