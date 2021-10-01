package institute.cocaine

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import dev.minn.jda.ktx.listener
import dev.minn.jda.ktx.onCommand
import institute.cocaine.audio.SendHandler
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag


class Bot(private val token: String) {
    private val jda: JDA = JDABuilder.createDefault(this.token).enableCache(CacheFlag.VOICE_STATE)
        .enableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS).injectKTX().build()
    var playerManager: AudioPlayerManager = DefaultAudioPlayerManager()

    private val players = HashMap<Long, SendHandler>()

    init {
        AudioSourceManagers.registerLocalSource(playerManager)
        AudioSourceManagers.registerRemoteSources(playerManager)

        jda.listener<ReadyEvent> { readyEvent ->
            val guild = jda.getGuildById(732689330769887314L)!!
            guild.upsertCommand(name = "join", description = "Joins your current VC!") {
                option<VoiceChannel>("channel", "joins another channel, you ain't at")
            }.queue()
        }

        jda.listener<GuildVoiceJoinEvent> { event ->
            val member = event.member
            if (member == event.guild.selfMember)
                return@listener
        }

        jda.onCommand("join") { event ->
            val argChannel = event.getOption("channel")?.asGuildChannel
            val guild = event.guild ?: return@onCommand
            val member = event.member ?: return@onCommand

            val voiceState = member.voiceState

            if ((voiceState == null || !voiceState.inVoiceChannel()) && argChannel == null) {
                event.reply("You ain't in a channel and didn't provide one either.").queue()
                return@onCommand
            }
            if (argChannel != null && argChannel !is VoiceChannel) {
                event.reply("Can't join that **non**-voice-channel.").queue()
                return@onCommand
            }

            val channel = if (voiceState!!.channel == null) {
                argChannel as VoiceChannel
            } else {
                voiceState.channel!!
            }


            if (member.hasPermission(channel, Permission.VOICE_CONNECT)
                && guild.selfMember.hasPermission(channel, Permission.VOICE_CONNECT)
            ) {
                joinVC(event, channel)
                return@onCommand
            }
            event.reply("Can't connect").queue()
            return@onCommand
        }
    }

    private fun joinVC(event: SlashCommandEvent, vc: VoiceChannel) {
        val audioManager = vc.guild.audioManager
        audioManager.openAudioConnection(vc)
        val player = playerManager.createPlayer()
        val sendHandler = SendHandler(player)
        sendHandler.acceptEvent(event)
        audioManager.sendingHandler = sendHandler
        players[vc.guild.idLong] = sendHandler
        event.deferReply().setContent("> Successfully joined you in `${vc.name}`").queue()
        playerManager.loadItem("./neutral_con.mp3", object: AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                println("loaded")
                players[vc.guild.idLong]!!.audioPlayer.startTrack(track, true)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                println("how did we get here")
            }

            override fun noMatches() {
                println("No match")
            }

            override fun loadFailed(exception: FriendlyException) {
                exception.printStackTrace()
            }
        })
    }

    private fun leaveVC(event: SlashCommandEvent, vc: VoiceChannel) {
        val audioManager = vc.guild.audioManager
        audioManager.closeAudioConnection()
    }
}