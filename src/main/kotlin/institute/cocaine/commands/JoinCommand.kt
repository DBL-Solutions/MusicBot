package institute.cocaine.commands

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

object JoinCommand: Command() {
    override suspend fun handleSlashEvent(event: GenericCommandInteractionEvent) {
        val argChannel = event.getOption("channel")?.asGuildChannel
        val guild = event.guild ?: return
        val member = event.member ?: return

        val voiceState = member.voiceState

        if ((voiceState == null || !voiceState.inAudioChannel()) && argChannel == null) {
            event.reply("You ain't in a channel and didn't provide one either.").queue()
            return
        }
        if (argChannel != null && argChannel !is VoiceChannel) {
            event.reply("Can't join that **non**-voice-channel.").queue()
            return
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
            return
        }
        event.reply("Can't connect").queue()
        return
    }
}