package institute.cocaine.commands

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.await
import institute.cocaine.Bot
import net.dv8tion.jda.api.entities.AudioChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

sealed class Command {

    companion object {
        lateinit var playerManager: AudioPlayerManager
        lateinit var players: Bot.HashMapPutDefault
    }

    abstract suspend fun handleSlashEvent(event: GenericCommandInteractionEvent)

    protected suspend fun joinVC(event: GenericCommandInteractionEvent, vc: AudioChannel, tc: TextChannel) {
        val audioManager = vc.guild.audioManager
        audioManager.openAudioConnection(vc)
        val idToRef = event.deferReply().setContent("> Successfully joined you in ${vc.asMention}").await().retrieveOriginal().await().idLong
        val sendHandler = players[vc.guild.idLong to tc].acceptEvent(event, idToRef)
        audioManager.sendingHandler = sendHandler
        playerManager.loadItem("./neutral_con.mp3", object: AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                players[vc.guild.idLong to tc].audioPlayer.startTrack(track, false)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
            }

            override fun noMatches() {
            }

            override fun loadFailed(exception: FriendlyException) {
                exception.printStackTrace()
            }
        })
    }

    protected fun Long.toTime(): String {
        val hconv = (60 * 60 * 1000)
        val mconv = (60 * 1000)
        val hours = this / hconv
        val min = (this - hours * hconv) / mconv
        val s = (this - hours * hconv - min * mconv) / 1000
        return "" + (if (hours > 0) "%2d:".format(hours) else "") + (if (min > 0) "%2d:".format(min) else "") + "%2d".format(s)
    }
}