package institute.cocaine

import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import dev.minn.jda.ktx.listener
import dev.minn.jda.ktx.onCommand
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

class Bot(private val token: String) {
    private val jda: JDA = JDABuilder.createLight(this.token).injectKTX().build()

    init {
        jda.listener<ReadyEvent> { readyEvent ->
            val guild = jda.getGuildById("")!!
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

            if (voiceState == null || !voiceState.inVoiceChannel() || voiceState.channel == null) {
                if (argChannel == null || argChannel !is VoiceChannel) {
                    return@onCommand
                }
                event.deferReply().queue()
                return@onCommand
            }
            val channel = voiceState.channel!!
            joinVC(event, channel)
        }
    }

    private fun joinVC(event: SlashCommandEvent, vc: VoiceChannel) {
        vc.guild.audioManager.openAudioConnection(vc)
        event.reply("> Successfully joined you in ${vc.name}").queue()
    }
}