package nl.punishments.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.punishments.velocity.managers.PunishmentManager;
import nl.punishments.velocity.models.Punishment;
import nl.punishments.velocity.utils.TimeUtil;

import java.util.Optional;

public class LoginListener {

    private final PunishmentManager pm;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public LoginListener(PunishmentManager pm) {
        this.pm = pm;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Optional<Punishment> ban = pm.getActiveBan(event.getPlayer().getUniqueId());
        ban.ifPresent(punishment -> {
            String until = punishment.isPermanent() ? "Permanent" : TimeUtil.formatDate(punishment.getExpiresAt());
            event.setResult(com.velocitypowered.api.event.ResultedEvent.ComponentResult.denied(
                    mm.deserialize(
                            "<red><bold>Je bent gebanned van deze server!</bold></dark_red>\n\n" +
                            "<gray>Reden: </gray><white>" + punishment.getReason() + "</white>\n" +
                            "<gray>Gebanned door: </gray><red>" + punishment.getStaffName() + "</red>\n" +
                            "<gray>Geldig tot: </gray><yellow>" + until + "</yellow>\n\n" +
                            "<dark_gray>Wil je meer informatie over je punishment of bezwaar indienen? Maak een ticket aan in de discord!."
                    )
            ));
        });
    }
}
