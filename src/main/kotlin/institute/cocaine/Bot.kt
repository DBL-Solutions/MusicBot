package institute.cocaine

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.interactions.choice
import dev.minn.jda.ktx.interactions.command
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.updateCommands
import dev.minn.jda.ktx.listener
import dev.minn.jda.ktx.onCommand
import institute.cocaine.audio.SendHandler
import kotlinx.coroutines.delay
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

    private val players = HashMapPutDefault(playerManager)

    init {
        AudioSourceManagers.registerLocalSource(playerManager)
        AudioSourceManagers.registerRemoteSources(playerManager)

        jda.listener<ReadyEvent> { readyEvent ->
            val guild = jda.getGuildById(732689330769887314L)!!
            guild.updateCommands {
                command(name = "join", description = "Joins your current VC!") {
                    option<VoiceChannel>("channel", "joins another channel, you ain't at")
                }
                command("disconnect", "Disconnects from it's current VC")
                command("nowplaying", "Prints the current playing song.") {
                    option<String>("grab", "DMs you the current now playing information")
                }
                command("queue", "Prints the current queue.")
                command("skip", "Skips playing & queued tracks") {
                    option<Int>("amount", "the amount of songs to skip")
                }
                command("seek", "Jumps to a (relative or absolute) position inside of the track") {
                    option<String>("position", "signs for relative seeking, non for absolute")
                    // TODO: do this
                }
                command("play", "Plays a song from the internet, or optionally a local file.") {
                    option<String>("url", "The content to enqueue", true)
                    option<Int>("position", "Enqueueing position, where the song should be inserted") {
                        choice("now", 0L)
                        choice("top", 1L)
                        choice("random", -2L)
                        choice("last / append (default)", -1L)
                    }
                }
                command("pause", "Pauses the player")
                command("mtq", "Ich hab auch \"MTQ\" verstanden, anstelle von emptyqueue") // TODO: todo
                command("dice", "roll the dize") {
                    option<Long>("sites", "Anzahl der Würfelseiten")
                    // TODO: option<String>("equation",)
                }
                command("clean", "Purges the channel history until a non bot message is encountered")
            }.queue()
        }

        jda.listener<GuildVoiceJoinEvent> { event ->
            if (!event.guild.selfMember.voiceState!!.inVoiceChannel())
                return@listener
            val member = event.member
            if (member.idLong != 198137282018934784L)
                return@listener
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

        jda.onCommand("disconnect") { event ->
            event.guild?.selfMember?.voiceState?.channel?.let {
                playerManager.loadItem("./neutral_kick.mp3", object: AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        players[event.guild!!.idLong].audioPlayer.startTrack(track, false)
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist) {
                    }

                    override fun noMatches() {
                    }

                    override fun loadFailed(exception: FriendlyException) {
                        exception.printStackTrace()
                    }
                })
                delay(2380)
                leaveVC(event, it)
            }
        }

        jda.onCommand("clean") { event ->
            val textChannel = event.textChannel

            textChannel.iterableHistory.takeWhileAsync {
                it.author == jda.selfUser
            }.thenAccept {
                textChannel.purgeMessages(it)
            }
            event.deferReply().setContent("deletiert").queue()
        }

        jda.onCommand("pause") { event ->
            val sendHandler = players[event.guild!!.idLong].acceptEvent(event)
            sendHandler.audioPlayer.isPaused = !sendHandler.audioPlayer.isPaused
            event.deferReply().queue()
        }

        jda.onCommand("play") { event ->
            if (!event.guild!!.selfMember.voiceState!!.inVoiceChannel()) {
                joinVC(event, event.member!!.voiceState!!.channel!!)
            } else {
                event.deferReply().setContent("Some thonk").queue()
            }
            playerManager.loadItem(event.getOption("url")!!.asString, AudioLoader.also {
                it.id = event.guild!!.idLong
                it.players = players
            })
        }

        jda.onCommand("skip") { event ->
            val n = event.getOption("amount")?.asLong ?: 1
            players[event.guild!!.idLong].scheduler.skipTracks(n.toInt())
            event.deferReply().queue()
        }
    }

    private suspend fun joinVC(event: SlashCommandEvent, vc: VoiceChannel) {
        val audioManager = vc.guild.audioManager
        audioManager.openAudioConnection(vc)
        val sendHandler = players[vc.guild.idLong].acceptEvent(event)
        audioManager.sendingHandler = sendHandler
        event.deferReply().setContent("> Successfully joined you in ${vc.asMention}").queue()
        playerManager.loadItem("./neutral_con.mp3", object: AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                players[vc.guild.idLong].audioPlayer.startTrack(track, false)

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

    private fun leaveVC(event: SlashCommandEvent, vc: VoiceChannel) {
        event.reply("> Left ${vc.asMention}").queue()
        vc.guild.audioManager.closeAudioConnection()
    }

    object AudioLoader : AudioLoadResultHandler {
        var id: Long = 0
        lateinit var players: HashMapPutDefault
        override fun trackLoaded(track: AudioTrack) {
            players[id].scheduler.enqueue(track)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
        }

        override fun noMatches() {
        }

        override fun loadFailed(exception: FriendlyException) {
            exception.printStackTrace()
        }
    }

    class HashMapPutDefault(private val playerManager: AudioPlayerManager) : HashMap<Long, SendHandler>() {
        override fun get(key: Long): SendHandler {
            if (!containsKey(key)) {
                val temp = SendHandler(playerManager.createPlayer())
                put(key, temp)
                return temp
            }
            return super.get(key)!!
        }
    }
}