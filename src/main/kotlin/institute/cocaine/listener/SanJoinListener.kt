package institute.cocaine.listener

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import institute.cocaine.Bot
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent

object SanJoinListener {

    lateinit var playerManager: AudioPlayerManager
    lateinit var players: Bot.HashMapPutDefault

    fun handleEvent(event: GuildVoiceJoinEvent) {
        if (!event.guild.selfMember.voiceState!!.inAudioChannel())
            return
        val member = event.member
        if (member.idLong != 198137282018934784L)
            return
        playerManager.loadItem("./buddy_con.mp3", object: AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                players[event.guild.idLong].audioPlayer.startTrack(track, false)

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
}