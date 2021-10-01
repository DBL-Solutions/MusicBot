package institute.cocaine.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame

import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer


class SendHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler {
    private var lastFrame: AudioFrame? = null
    override fun canProvide(): Boolean {
        lastFrame = audioPlayer.provide() ?: return false
        return true
    }

    override fun provide20MsAudio(): ByteBuffer {
        return if (canProvide())
            ByteBuffer.wrap(lastFrame?.data)
        else
            ByteBuffer.wrap(byteArrayOf())
    }

    override fun isOpus(): Boolean {
        return true
    }
}