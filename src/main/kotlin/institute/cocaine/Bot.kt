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
import dev.minn.jda.ktx.interactions.slash
import dev.minn.jda.ktx.interactions.subcommand
import dev.minn.jda.ktx.interactions.updateCommands
import dev.minn.jda.ktx.listener
import dev.minn.jda.ktx.onCommand
import dev.minn.jda.ktx.onCommandAutocomplete
import institute.cocaine.audio.SendHandler
import institute.cocaine.commands.CleanCommand
import institute.cocaine.commands.Command
import institute.cocaine.commands.DiceCommand
import institute.cocaine.commands.JoinCommand
import institute.cocaine.commands.NowPlayingCommand
import institute.cocaine.commands.PlayCommand
import institute.cocaine.commands.QueueCommand
import institute.cocaine.commands.RepeatCommand
import institute.cocaine.commands.SeekCommand
import institute.cocaine.commands.SkipCommand
import institute.cocaine.listener.SanJoinListener
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.AudioChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class Bot(private val token: String) {
    private val jda: JDA = JDABuilder.createDefault(this.token).enableCache(CacheFlag.VOICE_STATE)
        .enableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS).injectKTX().build()
    private var playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private var ranInit = false

    private val players = HashMapPutDefault(playerManager, jda)

    companion object {
        val respectPrivacy: Boolean
            get() = System.getenv()["dbl.violate.privacy"]?.toBoolean() ?: true
        val isDevEnvironment: Boolean
            get() = System.getenv()["dbl.dev"]?.toBoolean() ?: false
    }

    init {
        AudioSourceManagers.registerLocalSource(playerManager)
        AudioSourceManagers.registerRemoteSources(playerManager)

        jda.listener<ReadyEvent> { readyEvent ->
            if (ranInit)
                return@listener

            val guild = jda.getGuildById(732689330769887314L)!!

            if (isDevEnvironment) {
                // todo dev upsert of certain commands & making command names a static jvm field
                return@listener
            }

            guild.updateCommands {
                slash(name = "join", description = "Joins your current VC!") {
                    option<VoiceChannel>(name = "channel", description = "joins another channel, you ain't at")
                }
                slash(name = "disconnect", "Disconnects from it's current VC")
                slash(name = "nowplaying", description = "Prints the current playing song.") {
                    option<Boolean>("grab", "DMs you the current information")
                }
                slash(name = "queue", description = "Prints the current queue.") // todo
                slash(name = "skip", description = "Skips playing & queued tracks") {
                    option<Int>(name = "amount", description = "the amount of songs to skip")
                }
                slash(name = "seek", description = "Jumps to a (relative or absolute) position inside of the track") {
                    option<String>(
                        name = "position",
                        description = "signs for relative seeking, non for absolute",
                        required = true,
                        autocomplete = true
                    )
                    // TODO: do this
                }
                slash(name = "play", description = "Plays a song from the internet, or optionally a local file.") {
                    option<String>(name = "url", description = "The content to enqueue", required = true, autocomplete = true)
                    option<Int>(
                        name = "position",
                        description = "Enqueueing position, where the song should be inserted",
                        autocomplete = true
                    )
                }
                slash(name = "pause", description = "Pauses the player")
                slash(name = "mtq", description = "Ich hab auch \"MTQ\" verstanden, anstelle von emptyqueue") // TODO: todo
                slash(name = "dice", description = "roll the dize") {
                    option<Long>(name = "sites", description = "Anzahl der Würfelseiten")
                    // TODO: option<String>("equation",)
                }
                slash(name = "clean", description = "Purges the channel history until a non bot message is encountered")
                slash(name = "repeat", description = "Repeats either the current song or the entire queue") {
                    subcommand("none", "exits the repeat mode & enters classic fifo playback")
                    subcommand("current", "repeats the current song only")
                    subcommand("all", "repeats an entire playlist")
                }
                slash(name = "wipe-history", description = "Cleans all suggestions for autocomplete arguments.") {
                    subcommand("all", "all commands")
                    subcommand("play", "play command")
                    subcommand("skip", "skip command")
                    subcommand("seek", "seek command")
                    // todo
                }
            }.queue()
        }

        jda.listener<GuildVoiceJoinEvent> { event ->
            SanJoinListener.apply {
                this@apply.playerManager = this@Bot.playerManager
                this@apply.players = this@Bot.players
            }.handleEvent(event)
        }

        jda.onCommand("join") { event ->
            JoinCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }

        jda.onCommand("disconnect") { event ->
            event.guild?.selfMember?.voiceState?.channel?.let {
                playerManager.loadItem("./neutral_kick.mp3", object: AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        players[event.guild!!.idLong to event.textChannel].audioPlayer.startTrack(track, false)
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
            CleanCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }

        jda.onCommand("pause") { event ->
            val sendHandler = players[event.guild!!.idLong to event.textChannel]
            sendHandler.audioPlayer.isPaused = !sendHandler.audioPlayer.isPaused
            event.deferReply().setContent("Trying to pause").queue()
        }

        jda.onCommand("play") { event ->
            PlayCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }
        jda.onCommandAutocomplete("play", PlayCommand.URL.name) { event ->
            PlayCommand.handleURLSuggestion(event)
        }
        jda.onCommandAutocomplete("play", PlayCommand.POS.name) { event ->
            PlayCommand.handlePOSSuggestion(event)
        }

        jda.onCommand("skip") { event ->
            SkipCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }
        jda.onCommandAutocomplete("skip", SkipCommand.AMOUNT.name) { event ->
            SkipCommand.handleAMOUNTSuggestionEvent(event)
        }

        jda.onCommand("nowplaying") { event ->
            NowPlayingCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }

        jda.onCommand("dice") { event ->
            DiceCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }

        jda.onCommand("seek") { event ->
            SeekCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }
        jda.onCommandAutocomplete("seek", SeekCommand.POS.name) { event ->
            SeekCommand.handlePOSSuggestionEvent(event)
        }

        jda.onCommand("repeat") { event ->
            RepeatCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }

        jda.onCommand("queue") { event ->
            QueueCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
        }
            // TODO

        jda.onCommand("mtq") { event ->
            QueueCommand.apply {
                Command.playerManager = this@Bot.playerManager
                Command.players = this@Bot.players
            }.handleSlashEvent(event)
            // TODO
        }
    }

    private fun leaveVC(event: GenericCommandInteractionEvent, vc: AudioChannel) {
        event.reply("> Left ${vc.asMention}").queue()
        vc.guild.audioManager.closeAudioConnection()
    }



    object AudioLoader: AudioLoadResultHandler {

        var id: Long = 0
        var index: Int = -1
        lateinit var players: HashMapPutDefault
        override fun trackLoaded(track: AudioTrack) {
            players[id].scheduler.enqueue(track, index)
            index = -1
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            players[id].scheduler.info("Playlist ${playlist.name} has been loaded, but they aren't supported yet!")
        }

        override fun noMatches() {
            players[id].scheduler.info("No matching track was found!")
        }

        override fun loadFailed(exception: FriendlyException) {
            players[id].scheduler.info("Loading the track failed, check logs!")
            exception.printStackTrace()
        }
    }

    class HashMapPutDefault(private val playerManager: AudioPlayerManager, private val jda: JDA): HashMap<Long, SendHandler>() {
        override fun get(key: Long): SendHandler {
            if (!containsKey(key)) {
                val temp = SendHandler(playerManager.createPlayer(), jda.getTextChannelById(806619937904787496L)!!)
                put(key, temp)
                return temp
            }
            return super.get(key)!!
        }

        operator fun get(pair: Pair<Long, TextChannel>): SendHandler {
            if (!containsKey(pair.first)) {
                val temp = SendHandler(playerManager.createPlayer(), pair.second)
                put(pair.first, temp)
                return temp
            }
            return super.get(pair.first)!!
        }
    }
}