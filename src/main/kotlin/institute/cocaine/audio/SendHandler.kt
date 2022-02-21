package institute.cocaine.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import java.nio.ByteBuffer


data class SendHandler(val audioPlayer: AudioPlayer, val channel: TextChannel) : AudioSendHandler {
    val scheduler = TrackScheduler(audioPlayer, channel)
    init {
        audioPlayer.addListener(scheduler)
    }

    private var lastFrame: AudioFrame? = null
    override fun canProvide(): Boolean {
        lastFrame = audioPlayer.provide() ?: return false
        return true
    }

    override fun provide20MsAudio(): ByteBuffer {
        return ByteBuffer.wrap(lastFrame!!.data)
    }

    override fun isOpus(): Boolean {
        return true
    }

    fun acceptEvent(event: GenericCommandInteractionEvent, idToRef: Long = 0L): SendHandler {
        scheduler.acceptEvent(event, idToRef)
        return this
    }
}