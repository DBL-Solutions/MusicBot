package institute.cocaine

import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.interactions.choice
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import dev.minn.jda.ktx.listener
import dev.minn.jda.ktx.onCommand
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.ReadyEvent

class Bot(private val token: String) {
    private val jda: JDA = JDABuilder.createLight(this.token).injectKTX().build()

    init {
        jda.listener<ReadyEvent> { readyEvent: ReadyEvent ->
            val guild = jda.getGuildById("")!!
            guild.upsertCommand(name = "join", description = "Joins your current VC!") {
                option<String>("channel", "joins another channel, you ain't at") {
                    for (voiceChannel in guild.voiceChannelCache.take(24)) {
                        choice(voiceChannel.name, voiceChannel.id)
                    }
                }
            }.queue()
        }

        jda.onCommand("join") { event ->
            val id = event.getOption("channel")?.asString ?: return@onCommand
            val channel = event.guild?.voiceChannelCache?.getElementById(id) ?: return@onCommand
            event.deferReply().setContent("not yet in ${channel.name}").queue()
        }
    }
}