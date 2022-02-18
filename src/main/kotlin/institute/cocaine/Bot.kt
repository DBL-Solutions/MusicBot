package institute.cocaine

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.CoroutineEventListener
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.Message
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.interactions.choice
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.slash
import dev.minn.jda.ktx.interactions.updateCommands
import dev.minn.jda.ktx.listener
import dev.minn.jda.ktx.onCommand
import institute.cocaine.audio.SendHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.AudioChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.time.OffsetDateTime
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.coroutines.startCoroutine
import kotlin.math.ceil
import kotlin.math.floor


class Bot(private val token: String) {
    private val jda: JDA = JDABuilder.createDefault(this.token).enableCache(CacheFlag.VOICE_STATE)
        .enableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS).injectKTX().build()
    private var playerManager: AudioPlayerManager = DefaultAudioPlayerManager()

    private val players = HashMapPutDefault(playerManager)
    private var stopUpdating = false

    init {
        AudioSourceManagers.registerLocalSource(playerManager)
        AudioSourceManagers.registerRemoteSources(playerManager)

        jda.listener<ReadyEvent> { readyEvent ->
            val guild = jda.getGuildById(732689330769887314L)!!
            guild.updateCommands {
                slash(name = "join", description = "Joins your current VC!") {
                    option<VoiceChannel>("channel", "joins another channel, you ain't at")
                }
                slash("disconnect", "Disconnects from it's current VC")
                slash("nowplaying", "Prints the current playing song.") {
                    option<Boolean>("grab", "DMs you the current now playing information")
                }
                slash("queue", "Prints the current queue.")
                slash("skip", "Skips playing & queued tracks") {
                    option<Int>("amount", "the amount of songs to skip")
                }
                slash("seek", "Jumps to a (relative or absolute) position inside of the track") {
                    option<String>("position", "signs for relative seeking, non for absolute", true)
                    // TODO: do this
                }
                slash("play", "Plays a song from the internet, or optionally a local file.") {
                    option<String>("url", "The content to enqueue", true)
                    option<Int>("position", "Enqueueing position, where the song should be inserted") {
                        choice("now", 0L)
                        choice("top", 1L)
                        choice("random", -2L)
                        choice("last / append (default)", -1L)
                    }
                }
                slash("pause", "Pauses the player")
                slash("mtq", "Ich hab auch \"MTQ\" verstanden, anstelle von emptyqueue") // TODO: todo
                slash("dice", "roll the dize") {
                    option<Long>("sites", "Anzahl der Würfelseiten")
                    // TODO: option<String>("equation",)
                }
                slash("clean", "Purges the channel history until a non bot message is encountered")
            }
                .queue()
        }

        jda.listener<GuildVoiceJoinEvent> { event ->
            if (!event.guild.selfMember.voiceState!!.inAudioChannel())
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

            if ((voiceState == null || !voiceState.inAudioChannel()) && argChannel == null) {
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

            textChannel.iterableHistory.takeAsync(320).thenApply { list ->
                list.filter { it.author == jda.selfUser && it.timeCreated.isAfter(OffsetDateTime.now().minusWeeks(2)) }
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
            val runnable = if (!event.guild!!.selfMember.voiceState!!.inAudioChannel()) {
                joinVC(event, event.member!!.voiceState!!.channel!!)
                Runnable {
                    println("Playing music now in ${event.member!!.voiceState!!.channel!!.name}")
                }
            } else {
                event.deferReply().setContent("Trying to add element to queue").queue()
                Runnable {
                    event.hook.deleteOriginal().queue()
                }
            }
            playerManager.loadItem(event.getOption("url")!!.asString, AudioLoader.apply {
                id = event.guild!!.idLong
                this@apply.players = this@Bot.players
                this.runnable = runnable
            })
        }

        jda.onCommand("skip") { event ->
            val n = event.getOption("amount")?.asLong ?: 1L
            players[event.guild!!.idLong].scheduler.skipTracks(n.toInt())
            event.deferReply().setContent("Skipping $n tracks!").queue()
        }

        jda.onCommand("nowplaying") { event ->
            val dm = event.getOption("grab")?.asBoolean ?: false
            val track = players[event.guild!!.idLong].audioPlayer.playingTrack
            if (!dm) {
                event.reply(Message {
                    content = track?.asInfo() ?: "Nothing rn, queue something bish"
                }).queue {
                    GlobalScope.launch {
                        val listener = jda.listener<MessageReceivedEvent> { listenerEvent ->
                            if (event.channel != listenerEvent.channel ) return@listener
                            stopUpdating = true
                        }
                        delay(10_000)
                        updateProgressBar(it, players[event.guild!!.idLong].audioPlayer, listener)
                    }
                }
            } else {
                event.deferReply(true).setContent("Information has been sent!").queue()
                event.user.openPrivateChannel().flatMap {
                    it.sendMessageEmbeds(Embed {
                        description = track?.asInfo(true) ?: "Nothing rn, queue something bish"
                        color = event.guild?.selfMember?.colorRaw
                    })
                }.queue()
            }
        }

        jda.onCommand("dice") { event ->
            val sites = event.getOption("sites")?.asLong ?: 6L
            event.reply("You rolled a ${ThreadLocalRandom.current().nextLong(sites) + 1} with your $sites sided dice!")
        }
    }

    private suspend fun joinVC(event: GenericCommandInteractionEvent, vc: AudioChannel) {
        val audioManager = vc.guild.audioManager
        audioManager.openAudioConnection(vc)
        val idToRef = event.deferReply().setContent("> Successfully joined you in ${vc.asMention}").await().retrieveOriginal().await().idLong
        val sendHandler = players[vc.guild.idLong].acceptEvent(event, event.messageChannel.idLong, idToRef)
        audioManager.sendingHandler = sendHandler
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

    private fun leaveVC(event: GenericCommandInteractionEvent, vc: AudioChannel) {
        event.reply("> Left ${vc.asMention}").queue()
        vc.guild.audioManager.closeAudioConnection()
    }

    private fun updateProgressBar(hook: InteractionHook, player: AudioPlayer, listener: CoroutineEventListener) {
        if (stopUpdating || player.playingTrack == null) {
            stopUpdating = false
            hook.jda.removeEventListener(listener)
            return
        }
        val track = player.playingTrack
        hook.editOriginal(Message {
            content = track?.asInfo() ?: "Nothing rn, queue something bish"
        }).queueAfter(15, TimeUnit.SECONDS) {
            updateProgressBar(hook, player, listener)
        }
    }

    object AudioLoader: AudioLoadResultHandler {

        var id: Long = 0
        lateinit var runnable: Runnable
        lateinit var players: HashMapPutDefault
        override fun trackLoaded(track: AudioTrack) {
            players[id].scheduler.enqueue(track)
            runnable.run()
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
        }

        override fun noMatches() {
        }

        override fun loadFailed(exception: FriendlyException) {
            exception.printStackTrace()
        }
    }

    class HashMapPutDefault(private val playerManager: AudioPlayerManager): HashMap<Long, SendHandler>() {
        override fun get(key: Long): SendHandler {
            if (!containsKey(key)) {
                val temp = SendHandler(playerManager.createPlayer())
                put(key, temp)
                return temp
            }
            return super.get(key)!!
        }
    }

    private fun AudioTrack.asInfo(isEmbed: Boolean = false): String {
        val title = info.title
        val uri = info.uri
        val author = info.author
        val length = (title.length + 3 + author.length + 7 + 6).coerceIn(20, if (isEmbed) 50 else 100)
        val text = "[$title](<$uri>) by `$author` until <t:${(System.currentTimeMillis() + duration - position) / 1000}:t>"
        val (progressbar, pos) = makeProgressbar(position.toFloat() / duration, length, info.isStream)
        val curTime = position.toTime()
        val halfTimeLength = (curTime.length.toFloat() / 2)
        val durTime = duration.toTime()
        val time = when {
            info.isStream -> "\u001B[33m⌊\u001B[35m${" ".repeat(pos - 1)}\u001B[35m\u221E${" ".repeat(pos)}\u001B[33m⌋"
            (pos < (2 + halfTimeLength)) ->
                buildString(4*5 + 2 + length) {
                    append("\u001B[33m⌊\u001B[35m")
                    append(curTime)
                    append(" ".repeat(length - curTime.length - durTime.length + 1))
                    append("\u001B[37m")
                    append(durTime)
                    append("\u001B[33m⌋")
                }
            (pos > (length - durTime.length - halfTimeLength.ceil())) ->
                buildString(4*5 + 3 + length) {
                    append("\u001B[33m⌊\u001B[37m0")
                    append(" ".repeat(length - curTime.length - 1))
                    append("\u001B[35m")
                    append(curTime)
                    append("\u001B[33m⌋")
                }
            else ->
                buildString(5*5 + 2 + length) {
                    append("\u001B[33m⌊\u001B[37m0")
                    append(" ".repeat(pos - halfTimeLength.ceil()))
                    append("\u001B[35m")
                    append(curTime)
                    append(" ".repeat(length - pos - halfTimeLength.floor() - durTime.length - 1))
                    append("\u001B[37m")
                    append(durTime)
                    append("\u001B[33m⌋")
                }
        }

        return """
        ::> $text
        ::```ansi
        ::[40m$progressbar[0m
        ::[40m$time[0m
        ::```
        """.trimMargin("::")
    }

    private fun makeProgressbar(progress: Float, width: Int = 20, isSteam: Boolean): Pair<String, Int> {
        if (isSteam)
            return "\u001B[33m⌈${" ".repeat((width / 2) -1)}\u001B[35m\u221E${" ".repeat((width / 2))}\u001B[33m⌉" to (width / 2)
        val fullchars = (progress * width).floor()
        val a = ">"
        val emptychars = width - fullchars - 1
        return buildString(5 * 5 + 2 + width) {
            append("\u001B[33m⌈\u001B[32m")
            append("#".repeat(fullchars))
            append("\u001B[35m")
            append(a)
            append("\u001B[34m")
            append("-".repeat(emptychars))
            append("\u001B[33m⌉")
        } to (fullchars)
    }

    private fun Long.toTime(): String {
        val hconv = (60 * 60 * 1000)
        val mconv = (60 * 1000)
        val hours = this / hconv
        val min = (this - hours * hconv) / mconv
        val s = (this - hours * hconv - min * mconv) / 1000
        return "" + (if (hours > 0) "$hours:" else "") + (if (min > 0) "$min:" else "") + s
    }

    private fun Float.ceil(): Int = ceil(this).toInt()
    private fun Float.floor(): Int = floor(this).toInt()
}